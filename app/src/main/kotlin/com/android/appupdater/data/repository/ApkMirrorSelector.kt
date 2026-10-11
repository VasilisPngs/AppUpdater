package com.android.appupdater.data.repository

import android.util.DisplayMetrics
import com.android.appupdater.data.isStableRelease
import com.android.appupdater.data.model.ApkMirrorApk
import com.android.appupdater.data.model.ApkMirrorApp
import com.android.appupdater.data.model.InstalledApp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal class MirrorCandidate(val apk: ApkMirrorApk, val versionName: String, val publishedAt: Long?)

internal class ApkMirrorSelector(
    private val sdk: Int,
    abis: List<String>,
    densityDpi: Int,
    private val isTelevision: Boolean,
    private val isAutomotive: Boolean
) {
    private val abis = abis.map(String::lowercase)
    private val densityBucket = DENSITY_BUCKETS.firstOrNull { it >= densityDpi } ?: DENSITY_BUCKETS.last()

    fun select(app: ApkMirrorApp, installed: InstalledApp, requiresTelevisionBuild: Boolean): MirrorCandidate? {
        if (!isStableRelease(app.versionName)) return null
        val releaseNumbers = versionNumbers(app.versionName)
        if (isInstalledVersion(releaseNumbers, versionNumbers(installed.versionName))) return null
        val installedFlavour = installedFlavour(installed.versionName)
        val apk = app.apks
            .asSequence()
            .filter { it.versionCode > installed.versionCode }
            .filter { it.minimumApi <= sdk }
            .filter { isStableLink(it.link, releaseNumbers) }
            .filter { matchesFlavour(it, installedFlavour, releaseNumbers) }
            .filter { matchesFormFactor(it, requiresTelevisionBuild) }
            .filter { matchesSignature(it, installed) }
            .filter { abiRank(it) != UNSUPPORTED_ABI }
            .minWithOrNull(
                compareBy<ApkMirrorApk> { it.minimumApi != installed.minSdk }
                    .thenBy(::abiRank)
                    .thenByDescending { variantSuffix(it.versionCode, installed.versionCode) }
                    .thenBy(::densityRank)
                    .thenByDescending(ApkMirrorApk::minimumApi)
                    .thenByDescending(ApkMirrorApk::versionCode)
            ) ?: return null
        return MirrorCandidate(
            apk = apk,
            versionName = fullVersionName(apk, app.versionName),
            publishedAt = publishedAt(apk.publishDate.ifBlank { app.publishDate })
        )
    }

    private fun isInstalledVersion(releaseNumbers: List<String>, installedNumbers: List<String>): Boolean =
        releaseNumbers.isNotEmpty() &&
            installedNumbers.size >= releaseNumbers.size &&
            installedNumbers.subList(0, releaseNumbers.size) == releaseNumbers

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
        if (apk.architectures.isEmpty() || apk.architectures.any { it in UNIVERSAL_ARCHITECTURES }) return abis.size
        return apk.architectures.minOf { architecture ->
            abis.indexOf(architecture.lowercase()).takeIf { it >= 0 } ?: UNSUPPORTED_ABI
        }
    }

    private fun densityRank(apk: ApkMirrorApk): Int {
        if (apk.densities.isEmpty() || NO_DENSITY in apk.densities) return UNIVERSAL_DENSITY_RANK
        val ranges = apk.densities.mapNotNull(::densityRange)
        return when {
            ranges.isEmpty() -> UNIVERSAL_DENSITY_RANK
            ranges.any { it.first == it.last && it.first == densityBucket } -> MATCHING_DENSITY_RANK
            ranges.any { it.first < it.last && densityBucket in it } -> UNIVERSAL_DENSITY_RANK
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

    private fun matchesFlavour(apk: ApkMirrorApk, installedFlavour: String?, releaseNumbers: List<String>): Boolean {
        if (installedFlavour == null) return true
        val apkFlavour = flavour(variantTokens(apk.link, releaseNumbers)) ?: return true
        return apkFlavour == installedFlavour
    }

    private fun matchesSignature(apk: ApkMirrorApk, installed: InstalledApp): Boolean = when {
        apk.signatureSha256s.isNotEmpty() && installed.signatureSha256s.isNotEmpty() ->
            apk.signatureSha256s.any { it in installed.signatureSha256s }
        apk.signatureSha1s.isNotEmpty() && installed.signatureSha1s.isNotEmpty() ->
            apk.signatureSha1s.any { it in installed.signatureSha1s }
        else -> true
    }

    private fun versionNumbers(version: String): List<String> = versionTokens(version).mapNotNull(::versionNumber)

    private fun versionTokens(version: String): List<String> = version.lowercase().split(*VERSION_SEPARATORS)

    private fun versionNumber(token: String): String? = token
        .removePrefix(VERSION_PREFIX)
        .takeIf { it.firstOrNull()?.isDigit() == true }
        ?.replaceFirst(LEADING_ZEROS, "")

    private fun installedFlavour(versionName: String): String? {
        val tokens = versionTokens(versionName)
        return flavour(tokens.subList(tokens.indexOfLast { versionNumber(it) != null } + 1, tokens.size))
    }

    private fun variantTokens(link: String, numbers: List<String>): List<String> {
        if (numbers.isEmpty()) return emptyList()
        val tokens = slugTokens(link.trimEnd('/').substringAfterLast('/'))
        var matched = 0
        tokens.forEachIndexed { index, token ->
            if (versionNumber(token) == numbers[matched] && ++matched == numbers.size) return tokens.drop(index + 1)
        }
        return emptyList()
    }

    private fun flavour(tokens: List<String>): String? = tokens.firstOrNull { token ->
        token.isNotEmpty() && token.all { it.isLetter() || it == '_' } && token !in NEUTRAL_TOKENS
    }

    private fun isStableLink(link: String, releaseNumbers: List<String>): Boolean = link
        .substringAfter(APKMIRROR_PATH_PREFIX, "")
        .split('/')
        .drop(2)
        .all { isStableRelease(versionPart(it, releaseNumbers)) }

    private fun versionPart(slug: String, releaseNumbers: List<String>): String {
        val tokens = slugTokens(slug)
        val first = releaseNumbers.firstOrNull() ?: return slug
        val start = tokens.indexOfFirst { versionNumber(it) == first }
        return if (start < 0) slug else tokens.subList(start, tokens.size).joinToString("-")
    }

    private fun slugTokens(slug: String): List<String> = slug.lowercase().split('-')

    private fun publishedAt(value: String): Long? {
        val text = value.trim().replace(' ', 'T')
        if (text.isEmpty()) return null
        return runCatching { OffsetDateTime.parse(text).toInstant() }
            .recoverCatching { LocalDateTime.parse(text).toInstant(ZoneOffset.UTC) }
            .recoverCatching { LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant() }
            .getOrNull()
            ?.toEpochMilli()
    }

    private companion object {
        const val APKMIRROR_PATH_PREFIX = "/apk/"
        const val NO_DENSITY = "nodpi"
        const val DPI_SUFFIX = "dpi"
        const val MAX_VERSION_NAME = 60
        const val UNSUPPORTED_ABI = Int.MAX_VALUE
        const val MATCHING_DENSITY_RANK = 0
        const val UNIVERSAL_DENSITY_RANK = 1
        const val FOREIGN_DENSITY_RANK = 2
        const val MIN_VARIANT_SUFFIX = 2
        const val WEAR_STANDALONE = "wear_standalone"
        const val LEANBACK = "leanback"
        const val LEANBACK_STANDALONE = "leanback_standalone"
        const val AUTOMOTIVE = "automotive"
        const val VERSION_PREFIX = "v"
        val LEADING_ZEROS = Regex("^0+(?=\\d)")
        val UNIVERSAL_ARCHITECTURES = setOf("universal", "noarch")
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
}
