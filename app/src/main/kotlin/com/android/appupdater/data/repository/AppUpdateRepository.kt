package com.android.appupdater.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import android.util.DisplayMetrics
import com.android.appupdater.data.api.ApkMirrorClient
import com.android.appupdater.data.appLabel
import com.android.appupdater.data.isStableRelease
import com.android.appupdater.data.model.ApkMirrorApk
import com.android.appupdater.data.model.ApkMirrorApp
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayVersion
import com.android.appupdater.data.play.PlayCatalog
import com.android.appupdater.data.play.playCertificateHash
import com.aurora.gplayapi.data.models.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.Locale

sealed interface ScanStatus {
    data object Scanning : ScanStatus
    data class Success(val updates: List<AppUpdateInfo>) : ScanStatus
    data class Error(val message: String, val partialUpdates: List<AppUpdateInfo>) : ScanStatus
}

class AppUpdateRepository(
    context: Context,
    private val playCatalog: PlayCatalog,
    private val client: ApkMirrorClient = ApkMirrorClient(context.packageName)
) {
    private val packageManager = context.packageManager
    private val isTelevision = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    private val isAutomotive = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    private val packageFlags = PackageManager.PackageInfoFlags.of(
        (PACKAGE_FLAGS or if (isTelevision) PackageManager.GET_CONFIGURATIONS else 0).toLong()
    )
    private val deviceAbis = Build.SUPPORTED_ABIS.map(String::lowercase)
    private val universalAbiRank = deviceAbis.size
    private val deviceDensityBucket = DENSITY_BUCKETS
        .firstOrNull { it >= context.resources.displayMetrics.densityDpi }
        ?: DENSITY_BUCKETS.last()

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val televisionLaunchers = if (isTelevision) televisionLaunchers() else emptySet()
        packageManager.getInstalledPackages(packageFlags)
            .mapNotNull { packageInfo -> runCatching { packageInfo.toInstalledApp(televisionLaunchers) }.getOrNull() }
    }

    private fun televisionLaunchers(): Set<String> = packageManager
        .queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong())
        )
        .mapTo(HashSet()) { it.activityInfo.packageName }

    fun scanForUpdates(appsToCheck: List<InstalledApp>): Flow<ScanStatus> = flow {
        if (appsToCheck.isEmpty()) {
            emit(ScanStatus.Success(emptyList()))
            return@flow
        }

        val batches = appsToCheck.chunked(API_BATCH_SIZE)
        val (mirrorResults, playResult) = coroutineScope {
            val play = async { attempt { playCatalog.lookup(appsToCheck.map(InstalledApp::packageName)) } }
            val mirror = batches.map { batch -> async { attempt { client.appExists(batch.map(InstalledApp::packageName)) } } }
            mirror.awaitAll() to play.await()
        }

        val mirrorApps = mirrorResults.mapNotNull(Result<List<ApkMirrorApp>>::getOrNull).flatten()
        val playApps = playResult.getOrNull().orEmpty()
        val installedByPackage = appsToCheck.associateBy(InstalledApp::packageName)
        val confirmation = confirmPlayUpdates(
            playApps.filter { app -> installedByPackage[app.packageName]?.let { playUpdate(app, it) } != null }
        )
        val delivery = offerMirrorVersionsOnPlay(merge(appsToCheck, mirrorApps, playApps, confirmation.apps), playApps)
        val updates = delivery.updates.sortedWith(NEWEST_FIRST)

        val failures = buildList {
            val failedBatches = mirrorResults.count(Result<List<ApkMirrorApp>>::isFailure)
            val mirrorFailure = mirrorResults.firstNotNullOfOrNull { it.exceptionOrNull() }?.let(::reason)
            when {
                failedBatches == batches.size -> add("APKMirror: $mirrorFailure")
                failedBatches > 0 -> add("Some applications were not checked on APKMirror: $mirrorFailure")
            }
            playResult.exceptionOrNull()?.let { add("Google Play: ${reason(it)}") }
            val unchecked = confirmation.unconfirmed + delivery.unchecked
            if (unchecked > 0) add("Google Play: $unchecked updates could not be checked")
        }

        emit(
            if (failures.isEmpty()) ScanStatus.Success(updates)
            else ScanStatus.Error(failures.joinToString("\n"), updates)
        )
    }.flowOn(Dispatchers.IO)

    private inline fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private fun reason(exception: Throwable): String =
        exception.message?.takeIf(String::isNotBlank) ?: exception::class.simpleName.orEmpty()

    private suspend fun confirmPlayUpdates(candidates: List<App>): PlayConfirmation = coroutineScope {
        val permits = Semaphore(PLAY_CONFIRMATIONS)
        val results = candidates.filterNot { it.isTestBuild }.map { app ->
            async {
                app.packageName to permits.withPermit { attempt { playCatalog.details(app.packageName).second } }
            }
        }.awaitAll()
        PlayConfirmation(
            apps = results.mapNotNull { (packageName, result) ->
                result.getOrNull()?.takeUnless { it.isTestBuild }?.let { packageName to it }
            }.toMap(),
            unconfirmed = results.count { it.second.isFailure }
        )
    }

    private suspend fun offerMirrorVersionsOnPlay(updates: List<AppUpdateInfo>, playApps: List<App>): PlayDelivery =
        coroutineScope {
            val offerTypes = playApps.associate { it.packageName to it.offerType }
            val permits = Semaphore(PLAY_CONFIRMATIONS)
            val checks = updates.map { update ->
                async {
                    val offerType = offerTypes[update.packageName]
                    if (update.apkMirrorUrl == null || !update.playAvailable || offerType == null) return@async update to null
                    update to permits.withPermit {
                        attempt {
                            playCatalog.delivers(
                                update.packageName,
                                update.newVersionCode,
                                offerType,
                                packageManager.playCertificateHash(update.packageName)
                            )
                        }
                    }
                }
            }.awaitAll()
            PlayDelivery(
                updates = checks.map { (update, delivered) ->
                    if (delivered?.getOrNull() == true) {
                        update.copy(playUpdate = PlayVersion(update.newVersionName, update.newVersionCode))
                    } else {
                        update
                    }
                },
                unchecked = checks.count { it.second?.isFailure == true }
            )
        }

    private val App.isTestBuild: Boolean
        get() = testingProgram?.isSubscribed == true || earlyAccess

    private fun playUpdate(app: App, installed: InstalledApp): App? = app.takeIf {
        matchesSignature(it, installed) && it.versionCode > installed.versionCode && isStableRelease(it.versionName)
    }

    private fun merge(
        installed: List<InstalledApp>,
        mirrorApps: List<ApkMirrorApp>,
        playApps: List<App>,
        confirmedPlayApps: Map<String, App>
    ): List<AppUpdateInfo> {
        val mirrorByPackage = mirrorApps.groupBy(ApkMirrorApp::packageName)
        val playByPackage = playApps.associateBy(App::packageName)

        return installed.mapNotNull { app ->
            val play = playByPackage[app.packageName]?.takeIf { matchesSignature(it, app) }
            val requiresTelevisionBuild = isTelevision && (app.isTelevisionBuild || play != null)
            val mirror = mirrorByPackage[app.packageName]
                ?.mapNotNull { mirrorCandidate(it, app, requiresTelevisionBuild) }
                ?.maxByOrNull { it.apk.versionCode }
            val playUpdate = confirmedPlayApps[app.packageName]?.let { playUpdate(it, app) }

            when {
                playUpdate != null && (mirror == null || playUpdate.versionCode >= mirror.apk.versionCode) -> AppUpdateInfo(
                    packageName = app.packageName,
                    appName = packageManager.appLabel(app.packageName),
                    newVersionName = playUpdate.versionName,
                    newVersionCode = playUpdate.versionCode,
                    publishedAt = mirror?.takeIf { it.apk.versionCode == playUpdate.versionCode }?.publishedAt,
                    apkMirrorUrl = null,
                    playAvailable = true,
                    playUpdate = PlayVersion(playUpdate.versionName, playUpdate.versionCode)
                )
                mirror != null -> AppUpdateInfo(
                    packageName = app.packageName,
                    appName = packageManager.appLabel(app.packageName),
                    newVersionName = mirror.versionName,
                    newVersionCode = mirror.apk.versionCode,
                    publishedAt = mirror.publishedAt,
                    apkMirrorUrl = mirror.apk.link.toAbsoluteApkMirrorUrl(),
                    playAvailable = play != null,
                    playUpdate = playUpdate?.let { PlayVersion(it.versionName, it.versionCode) }
                )
                else -> null
            }
        }
    }

    private fun mirrorCandidate(
        app: ApkMirrorApp,
        installed: InstalledApp,
        requiresTelevisionBuild: Boolean
    ): MirrorCandidate? {
        if (!isStableRelease(app.versionName)) return null
        val releaseNumbers = versionNumbers(app.versionName)
        if (isInstalledVersion(releaseNumbers, installed)) return null
        val apk = bestApk(app.apks, installed, releaseNumbers, requiresTelevisionBuild) ?: return null
        return MirrorCandidate(
            apk = apk,
            versionName = fullVersionName(apk, app.versionName),
            publishedAt = publishedAt(apk.publishDate.ifBlank { app.publishDate })
        )
    }

    private fun isInstalledVersion(releaseNumbers: List<String>, installed: InstalledApp): Boolean {
        val installedNumbers = versionNumbers(installed.versionName)
        return releaseNumbers.isNotEmpty() &&
            installedNumbers.size >= releaseNumbers.size &&
            releaseNumbers.indices.all { releaseNumbers[it].trimStart('0') == installedNumbers[it].trimStart('0') }
    }

    private fun matchesSignature(app: App, installed: InstalledApp): Boolean =
        app.certificateSetList.any { set ->
            runCatching { Base64.getUrlDecoder().decode(set.certificateSet).toHex() }.getOrNull() in installed.signatureSha1s
        }

    private fun bestApk(
        apks: List<ApkMirrorApk>,
        installed: InstalledApp,
        releaseNumbers: List<String>,
        requiresTelevisionBuild: Boolean
    ): ApkMirrorApk? = apks
        .asSequence()
        .filter { it.versionCode > installed.versionCode }
        .filter { it.minimumApi <= Build.VERSION.SDK_INT }
        .filter { isStableLink(it.link, releaseNumbers) }
        .filter { matchesFlavour(it, installed, releaseNumbers) }
        .filter { matchesFormFactor(it, requiresTelevisionBuild) }
        .filter { matchesSignature(it, installed) }
        .filter { abiRank(it) != UNSUPPORTED_ABI }
        .minWithOrNull(
            compareByDescending<ApkMirrorApk> { variantSuffix(it.versionCode, installed.versionCode) }
                .thenBy(::abiRank)
                .thenBy(::densityRank)
                .thenByDescending(ApkMirrorApk::minimumApi)
                .thenByDescending(ApkMirrorApk::versionCode)
        )

    private fun variantSuffix(versionCode: Long, installedVersionCode: Long): Int {
        var candidate = versionCode
        var current = installedVersionCode
        var matched = 0
        while (candidate > 0 && current > 0 && candidate % 10 == current % 10) {
            matched++
            candidate /= 10
            current /= 10
        }
        return if (matched >= MIN_VARIANT_SUFFIX) matched else 0
    }

    private fun fullVersionName(apk: ApkMirrorApk, releaseVersion: String): String {
        val description = apk.description.trim().substringBefore('\n').trim()
        val detailed = releaseVersion.isNotEmpty() &&
            description.startsWith(releaseVersion) &&
            description.length <= MAX_VERSION_NAME
        return if (detailed) description else releaseVersion
    }

    private fun abiRank(apk: ApkMirrorApk): Int {
        if (apk.architectures.isEmpty()) return universalAbiRank
        if (apk.architectures.any { it in UNIVERSAL_ARCHITECTURES }) return universalAbiRank
        return apk.architectures.minOf(::abiIndex)
    }

    private fun abiIndex(architecture: String): Int =
        deviceAbis.indexOf(architecture).takeIf { it >= 0 } ?: UNSUPPORTED_ABI

    private fun densityRank(apk: ApkMirrorApk): Int {
        if (apk.densities.isEmpty() || NO_DENSITY in apk.densities) return UNIVERSAL_DENSITY_RANK

        val ranges = apk.densities.mapNotNull(::densityRange)
        return when {
            ranges.isEmpty() -> UNIVERSAL_DENSITY_RANK
            ranges.any { it.first == it.last && it.first == deviceDensityBucket } -> MATCHING_DENSITY_RANK
            ranges.any { it.first < it.last && deviceDensityBucket in it } -> UNIVERSAL_DENSITY_RANK
            else -> FOREIGN_DENSITY_RANK
        }
    }

    private fun densityRange(density: String): IntRange? {
        val bounds = density.removeSuffix(DPI_SUFFIX).split('-').map { it.trim().toIntOrNull() ?: return null }
        return when (bounds.size) {
            1 -> bounds[0]..bounds[0]
            2 -> bounds[0]..bounds[1]
            else -> null
        }
    }

    private fun matchesFormFactor(apk: ApkMirrorApk, requiresTelevisionBuild: Boolean): Boolean = when {
        WEAR_STANDALONE in apk.capabilities -> false
        !isAutomotive && apk.capabilities.any { AUTOMOTIVE in it } -> false
        requiresTelevisionBuild -> LEANBACK in apk.capabilities || LEANBACK_STANDALONE in apk.capabilities
        isTelevision -> true
        else -> LEANBACK_STANDALONE !in apk.capabilities
    }

    private fun matchesFlavour(apk: ApkMirrorApk, installed: InstalledApp, releaseNumbers: List<String>): Boolean {
        val installedFlavour = flavour(installed.versionName.lowercase().split(*VERSION_SEPARATORS)) ?: return true
        val apkFlavour = flavour(variantTokens(apk.link, releaseNumbers)) ?: return true
        return apkFlavour == installedFlavour
    }

    private fun versionNumbers(version: String): List<String> =
        version.split(*VERSION_SEPARATORS).filter { it.isNotEmpty() && it.all(Char::isDigit) }

    private fun variantTokens(link: String, numbers: List<String>): List<String> {
        if (numbers.isEmpty()) return emptyList()
        val tokens = link.trimEnd('/').substringAfterLast('/').lowercase().split('-')
        var matched = 0
        tokens.forEachIndexed { index, token ->
            if (token == numbers[matched] && ++matched == numbers.size) return tokens.drop(index + 1)
        }
        return emptyList()
    }

    private fun flavour(tokens: List<String>): String? = tokens.firstOrNull { token ->
        token.isNotEmpty() && token.all { it.isLetter() || it == '_' } && token !in NEUTRAL_TOKENS
    }

    private fun matchesSignature(apk: ApkMirrorApk, installed: InstalledApp): Boolean = when {
        apk.signatureSha256s.isNotEmpty() && installed.signatureSha256s.isNotEmpty() ->
            apk.signatureSha256s.any { it in installed.signatureSha256s }
        apk.signatureSha1s.isNotEmpty() && installed.signatureSha1s.isNotEmpty() ->
            apk.signatureSha1s.any { it in installed.signatureSha1s }
        else -> true
    }

    private fun publishedAt(value: String): Long? {
        val text = value.trim().replace(' ', 'T')
        if (text.isEmpty()) return null

        return runCatching { OffsetDateTime.parse(text).toInstant() }
            .recoverCatching { LocalDateTime.parse(text).toInstant(ZoneOffset.UTC) }
            .recoverCatching { LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant() }
            .getOrNull()
            ?.toEpochMilli()
    }

    private fun isStableLink(link: String, releaseNumbers: List<String>): Boolean = link
        .substringAfter(APKMIRROR_PATH_PREFIX, "")
        .split('/')
        .drop(2)
        .all { isStableRelease(versionPart(it, releaseNumbers)) }

    private fun versionPart(slug: String, releaseNumbers: List<String>): String {
        val tokens = slug.split('-')
        val start = releaseNumbers.firstOrNull()?.let(tokens::indexOf) ?: -1
        return if (start < 0) slug else tokens.subList(start, tokens.size).joinToString("-")
    }

    private fun PackageInfo.toInstalledApp(televisionLaunchers: Set<String>): InstalledApp? {
        val appInfo = applicationInfo ?: return null
        val certificates = signingInfo.certificates()

        return InstalledApp(
            packageName = packageName,
            versionName = versionName.orEmpty(),
            versionCode = longVersionCode,
            signatureSha1s = certificates.digests("SHA-1"),
            signatureSha256s = certificates.digests("SHA-256"),
            isEnabled = appInfo.enabled,
            isTelevisionBuild = packageName in televisionLaunchers ||
                reqFeatures?.any { it.name == PackageManager.FEATURE_LEANBACK } == true
        )
    }

    private fun SigningInfo?.certificates(): List<Signature> = when {
        this == null -> emptyList()
        hasMultipleSigners() -> apkContentsSigners.toList()
        else -> signingCertificateHistory.toList()
    }

    private fun List<Signature>.digests(algorithm: String): Set<String> {
        if (isEmpty()) return emptySet()
        val digest = MessageDigest.getInstance(algorithm)
        return mapTo(mutableSetOf()) { signature ->
            digest.digest(signature.toByteArray()).toHex()
        }
    }

    private fun ByteArray.toHex(): String = toHexString()

    private fun String.toAbsoluteApkMirrorUrl(): String = APKMIRROR_URL + this

    private companion object {
        const val API_BATCH_SIZE = 100
        const val APKMIRROR_URL = "https://www.apkmirror.com"
        const val APKMIRROR_PATH_PREFIX = "/apk/"
        const val NO_DENSITY = "nodpi"
        const val DPI_SUFFIX = "dpi"
        const val MAX_VERSION_NAME = 60
        val NEWEST_FIRST = compareByDescending<AppUpdateInfo> { it.publishedAt ?: Long.MAX_VALUE }
            .thenBy { it.appName.lowercase(Locale.ROOT) }
        const val UNSUPPORTED_ABI = Int.MAX_VALUE
        const val MATCHING_DENSITY_RANK = 0
        const val UNIVERSAL_DENSITY_RANK = 1
        const val FOREIGN_DENSITY_RANK = 2
        const val PACKAGE_FLAGS = PackageManager.GET_SIGNING_CERTIFICATES
        val UNIVERSAL_ARCHITECTURES = setOf("universal", "noarch")
        const val WEAR_STANDALONE = "wear_standalone"
        const val LEANBACK = "leanback"
        const val LEANBACK_STANDALONE = "leanback_standalone"
        const val AUTOMOTIVE = "automotive"
        const val PLAY_CONFIRMATIONS = 4
        const val MIN_VARIANT_SUFFIX = 2
        val VERSION_SEPARATORS = charArrayOf('.', '-', ' ', '(', ')', '[', ']')
        val NEUTRAL_TOKENS = setOf("android", "apk", "bundle", "download", "arm", "armeabi", "universal", "noarch", "nodpi")
        val DENSITY_BUCKETS = listOf(
            DisplayMetrics.DENSITY_LOW,
            DisplayMetrics.DENSITY_MEDIUM,
            DisplayMetrics.DENSITY_TV,
            DisplayMetrics.DENSITY_HIGH,
            DisplayMetrics.DENSITY_XHIGH,
            DisplayMetrics.DENSITY_XXHIGH,
            DisplayMetrics.DENSITY_XXXHIGH
        )
    }

    private class MirrorCandidate(val apk: ApkMirrorApk, val versionName: String, val publishedAt: Long?)

    private class PlayConfirmation(val apps: Map<String, App>, val unconfirmed: Int)

    private class PlayDelivery(val updates: List<AppUpdateInfo>, val unchecked: Int)
}
