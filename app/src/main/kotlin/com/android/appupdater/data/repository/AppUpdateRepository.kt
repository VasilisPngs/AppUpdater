package com.android.appupdater.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import com.android.appupdater.data.api.ApkMirrorClient
import com.android.appupdater.data.appLabel
import com.android.appupdater.data.isStableRelease
import com.android.appupdater.data.model.ApkMirrorApp
import com.android.appupdater.data.model.AppUpdateInfo
import com.android.appupdater.data.model.InstalledApp
import com.android.appupdater.data.model.PlayVersion
import com.android.appupdater.data.play.PlayCatalog
import com.android.appupdater.data.play.playCertificateHash
import com.aurora.gplayapi.data.models.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Base64
import java.util.Locale

sealed interface ScanStatus {
    data class Success(val updates: List<AppUpdateInfo>) : ScanStatus
    data class Error(val message: String, val partialUpdates: List<AppUpdateInfo>) : ScanStatus
}

class AppUpdateRepository(
    private val context: Context,
    private val playCatalog: PlayCatalog,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val client = ApkMirrorClient(context.packageName)
    private val packageManager = context.packageManager
    private val isTelevision = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    private val packageFlags = PackageManager.PackageInfoFlags.of(
        (PACKAGE_FLAGS or if (isTelevision) PackageManager.GET_CONFIGURATIONS else 0).toLong()
    )
    private val selector = ApkMirrorSelector(
        sdk = Build.VERSION.SDK_INT,
        abis = Build.SUPPORTED_ABIS.asList(),
        densityDpi = context.resources.displayMetrics.densityDpi,
        isTelevision = isTelevision,
        isAutomotive = packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    )

    val packageChanges: Flow<String> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_PACKAGE_REPLACED && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
                intent.data?.schemeSpecificPart?.let(::trySend)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme(PACKAGE_SCHEME)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(ioDispatcher) {
        val televisionLaunchers = if (isTelevision) televisionLaunchers() else emptySet()
        packageManager.getInstalledPackages(packageFlags)
            .mapNotNull { packageInfo -> runCatching { packageInfo.toInstalledApp(televisionLaunchers) }.getOrNull() }
    }

    suspend fun getInstalledApp(packageName: String): InstalledApp? = withContext(ioDispatcher) {
        val televisionLaunchers = if (isTelevision) televisionLaunchers(packageName) else emptySet()
        runCatching { packageManager.getPackageInfo(packageName, packageFlags).toInstalledApp(televisionLaunchers) }.getOrNull()
    }

    private fun televisionLaunchers(packageName: String? = null): Set<String> = packageManager
        .queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(packageName),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong())
        )
        .mapTo(HashSet()) { it.activityInfo.packageName }

    suspend fun scanForUpdates(appsToCheck: List<InstalledApp>): ScanStatus = withContext(ioDispatcher) {
        if (appsToCheck.isEmpty()) return@withContext ScanStatus.Success(emptyList())

        val batches = appsToCheck.chunked(API_BATCH_SIZE)
        val (mirrorResults, playResult) = coroutineScope {
            val play = async {
                attempt { runInterruptible { playCatalog.lookup(appsToCheck.map(InstalledApp::packageName)) } }
            }
            val mirror = batches.map { batch ->
                async { attempt { runInterruptible { client.appExists(batch.map(InstalledApp::packageName)) } } }
            }
            mirror.awaitAll() to play.await()
        }

        val mirrorApps = mirrorResults.mapNotNull(Result<List<ApkMirrorApp>>::getOrNull).flatten()
        val playApps = playResult.getOrNull().orEmpty()
        val installedByPackage = appsToCheck.associateBy(InstalledApp::packageName)
        val confirmation = confirmPlayUpdates(
            playApps.filter { app -> installedByPackage[app.packageName]?.let { playUpdate(app, it) } != null }
        )
        val delivery = offerMirrorVersionsOnPlay(merge(appsToCheck, mirrorApps, playApps, confirmation), playApps)
        val updates = delivery.updates.sortedWith(NEWEST_FIRST)

        val failures = buildList {
            val failedBatches = mirrorResults.count(Result<List<ApkMirrorApp>>::isFailure)
            val mirrorFailure = mirrorResults.firstNotNullOfOrNull { it.exceptionOrNull() }?.let(::reason)
            when {
                failedBatches == batches.size -> add("APKMirror: $mirrorFailure")
                failedBatches > 0 -> add("Some applications were not checked on APKMirror: $mirrorFailure")
            }
            playResult.exceptionOrNull()?.let { add("Google Play: ${reason(it)}") }
            val unchecked = (confirmation.unconfirmed + delivery.unchecked)
                .distinct()
                .map(packageManager::appLabel)
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
            if (unchecked.isNotEmpty()) add("Google Play: could not check ${unchecked.joinToString()}")
        }

        if (failures.isEmpty()) ScanStatus.Success(updates) else ScanStatus.Error(failures.joinToString("\n"), updates)
    }

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
                app.packageName to permits.withPermit {
                    attempt { runInterruptible { playCatalog.details(app.packageName) } }
                }
            }
        }.awaitAll()
        val confirmed = results.mapNotNull { (packageName, result) ->
            result.getOrNull()?.takeUnless { it.second.isTestBuild }?.let { packageName to it }
        }
        PlayConfirmation(
            apps = confirmed.associate { (packageName, details) -> packageName to details.second },
            dates = confirmed.mapNotNull { (packageName, details) ->
                playDate(details.second.updatedOn, details.first.locale)?.let { packageName to it }
            }.toMap(),
            unconfirmed = results.filter { it.second.isFailure }.map { it.first }
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
                            runInterruptible {
                                playCatalog.delivers(
                                    update.packageName,
                                    update.newVersionCode,
                                    offerType,
                                    packageManager.playCertificateHash(update.packageName)
                                )
                            }
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
                unchecked = checks.filter { it.second?.isFailure == true }.map { it.first.packageName }
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
        confirmation: PlayConfirmation
    ): List<AppUpdateInfo> {
        val mirrorByPackage = mirrorApps.groupBy(ApkMirrorApp::packageName)
        val playByPackage = playApps.associateBy(App::packageName)

        return installed.mapNotNull { app ->
            val play = playByPackage[app.packageName]?.takeIf { matchesSignature(it, app) }
            val requiresTelevisionBuild = isTelevision && (app.isTelevisionBuild || play != null)
            val mirror = mirrorByPackage[app.packageName]
                ?.mapNotNull { selector.select(it, app, requiresTelevisionBuild) }
                ?.maxByOrNull { it.apk.versionCode }
            val playUpdate = confirmation.apps[app.packageName]?.let { playUpdate(it, app) }

            when {
                playUpdate != null && (mirror == null || playUpdate.versionCode >= mirror.apk.versionCode) -> AppUpdateInfo(
                    packageName = app.packageName,
                    appName = packageManager.appLabel(app.packageName),
                    newVersionName = playUpdate.versionName,
                    newVersionCode = playUpdate.versionCode,
                    publishedAt = mirror?.takeIf { it.apk.versionCode == playUpdate.versionCode }?.publishedAt
                        ?: confirmation.dates[app.packageName],
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

    private fun matchesSignature(app: App, installed: InstalledApp): Boolean =
        app.certificateSetList.any { set ->
            runCatching { Base64.getUrlDecoder().decode(set.certificateSet).toHexString() }.getOrNull() in installed.signatureSha1s
        }

    private fun playDate(value: String, locale: Locale): Long? = runCatching {
        LocalDate.parse(value.trim(), DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

    private fun PackageInfo.toInstalledApp(televisionLaunchers: Set<String>): InstalledApp? {
        val appInfo = applicationInfo ?: return null
        val certificates = signingInfo.certificates()

        return InstalledApp(
            packageName = packageName,
            versionName = versionName.orEmpty(),
            versionCode = longVersionCode,
            minSdk = appInfo.minSdkVersion,
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
            digest.digest(signature.toByteArray()).toHexString()
        }
    }

    private fun String.toAbsoluteApkMirrorUrl(): String = APKMIRROR_URL + this

    private companion object {
        const val API_BATCH_SIZE = 100
        const val PACKAGE_SCHEME = "package"
        const val APKMIRROR_URL = "https://www.apkmirror.com"
        val NEWEST_FIRST = compareByDescending<AppUpdateInfo> { it.publishedAt ?: Long.MIN_VALUE }
            .thenBy { it.appName.lowercase(Locale.ROOT) }
        const val PACKAGE_FLAGS = PackageManager.GET_SIGNING_CERTIFICATES
        const val PLAY_CONFIRMATIONS = 4
    }

    private class PlayConfirmation(val apps: Map<String, App>, val dates: Map<String, Long>, val unconfirmed: List<String>)

    private class PlayDelivery(val updates: List<AppUpdateInfo>, val unchecked: List<String>)
}
