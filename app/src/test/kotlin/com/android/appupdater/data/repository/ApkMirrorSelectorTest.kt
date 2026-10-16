package com.android.appupdater.data.repository

import com.android.appupdater.data.model.ApkMirrorApk
import com.android.appupdater.data.model.ApkMirrorApp
import com.android.appupdater.data.model.InstalledApp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ApkMirrorSelectorTest {
    private val phone = ApkMirrorSelector(
        sdk = 36,
        abis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
        densityDpi = 420,
        isTelevision = false,
        isAutomotive = false
    )

    @Test
    fun `keeps the device ABI when another ABI shares the installed decimal suffix`() {
        val release = release(
            "144.0",
            apk(2016120039, "firefox-144-0", architectures = listOf("armeabi-v7a")),
            apk(2016120041, "firefox-144-0-2", architectures = listOf("arm64-v8a")),
            apk(2016120043, "firefox-144-0-3", architectures = listOf("x86_64"))
        )

        assertEquals(2016120041, select(release, installed("143.0.2", 2016115639))?.apk?.versionCode)
    }

    @Test
    fun `keeps the installed variant among builds for the same ABIs`() {
        val release = release(
            "142.0.7444.48",
            apk(744404833, "chrome-142-0-7444-48", architectures = listOf("arm64-v8a", "armeabi-v7a"), minimumApi = 29),
            apk(744404838, "chrome-142-0-7444-48-2", architectures = listOf("arm64-v8a", "armeabi-v7a"), minimumApi = 29),
            apk(744404843, "chrome-142-0-7444-48-3", architectures = listOf("arm64-v8a"), minimumApi = 29)
        )

        assertEquals(744404833, select(release, installed("141.0.7390.122", 739012233, minSdk = 29))?.apk?.versionCode)
    }

    @Test
    fun `prefers the family of the installed minimum SDK`() {
        val release = release(
            "25.42.31",
            apk(254231141, "google-play-services-25-42-31", minimumApi = 31),
            apk(254231150, "google-play-services-25-42-31-2", minimumApi = 34)
        )

        assertEquals(254231141, select(release, installed("25.40.32", 254032040, minSdk = 31))?.apk?.versionCode)
    }

    @Test
    fun `waits for the installed family when the release lacks it`() {
        val release = release(
            "25.42.31",
            apk(254231035, "google-play-services-25-42-31", minimumApi = 23),
            apk(254231037, "google-play-services-25-42-31-2", minimumApi = 28)
        )

        assertNull(select(release, installed("25.40.32", 254032040, minSdk = 31)))
    }

    @Test
    fun `waits for the installed family when the release ships it only for another ABI`() {
        val release = release(
            "26.39.64",
            apk(
                263964013,
                "google-play-services-26-39-64-100400-994893731",
                architectures = listOf("arm64-v8a", "armeabi-v7a"),
                minimumApi = 28
            ),
            apk(263964038, "google-play-services-26-39-64-260800-994893731", architectures = listOf("x86_64"), minimumApi = 35)
        )

        assertNull(select(release, installed("26.39.33 (260400-995470952)", 263933035, minSdk = 35)))
    }

    @Test
    fun `moves to the lowest family when the app raised its minimum SDK`() {
        val release = release(
            "4.0",
            apk(4028, "app-4-0", minimumApi = 28),
            apk(4031, "app-4-0-2", minimumApi = 31)
        )

        assertEquals(4028L, select(release, installed("3.9", 3924, minSdk = 24))?.apk?.versionCode)
    }

    @Test
    fun `accepts a bundle whose minimum SDK differs from the one Google Play injected`() {
        val release = release("8.3.0", apk(830, "app-8-3-0", minimumApi = 24))

        assertEquals(830L, select(release, installed("8.2.0", 820, minSdk = 32))?.apk?.versionCode)
    }

    @Test
    fun `accepts a release that raised its minimum SDK`() {
        val release = release("3.0", apk(300, "app-3-0", minimumApi = 28))

        assertEquals(300L, select(release, installed("2.9", 290, minSdk = 24))?.apk?.versionCode)
    }

    @Test
    fun `rejects a release that requires a newer SDK than the device`() {
        val release = release("3.0", apk(300, "app-3-0", minimumApi = 37))

        assertNull(select(release, installed("2.9", 290)))
    }

    @Test
    fun `matches the flavour after the version number`() {
        val release = release("build-985-bundle-play", apk(985004, "poweramp-build-985-bundle-play"))

        assertEquals(985004L, select(release, installed("build-984-bundle-play", 984004))?.apk?.versionCode)
    }

    @Test
    fun `rejects another flavour`() {
        val release = release("build-985-bundle-uni", apk(985004, "poweramp-build-985-bundle-uni"))

        assertNull(select(release, installed("build-984-bundle-play", 984004)))
    }

    @Test
    fun `offers a newer alphanumeric version`() {
        val release = release("1.2.3b", apk(124, "app-1-2-3b"))

        assertEquals(124L, select(release, installed("1.2.3a", 123))?.apk?.versionCode)
    }

    @Test
    fun `offers a newer version with a v prefix`() {
        val release = release("v2.0.0", apk(20, "app-v2-0-0"))

        assertEquals(20L, select(release, installed("v1.0.0", 10))?.apk?.versionCode)
    }

    @Test
    fun `skips the installed version under another version code`() {
        val release = release("1.2.3A", apk(125, "app-1-2-3a"))

        assertNull(select(release, installed("1.2.3a", 123)))
    }

    @Test
    fun `skips a pre-release`() {
        val release = release("2.0-beta1", apk(200, "app-2-0-beta1"))

        assertNull(select(release, installed("1.9", 190)))
    }

    private fun select(release: ApkMirrorApp, installed: InstalledApp) = phone.select(release, installed, requiresTelevisionBuild = false)

    private fun installed(versionName: String, versionCode: Long, minSdk: Int = 26) = InstalledApp(
        packageName = PACKAGE,
        versionName = versionName,
        versionCode = versionCode,
        minSdk = minSdk,
        signatureSha1s = emptySet(),
        signatureSha256s = emptySet(),
        isEnabled = true,
        isTelevisionBuild = false,
        hasLanguageSplit = false
    )

    private fun release(versionName: String, vararg apks: ApkMirrorApk) =
        ApkMirrorApp(PACKAGE, versionName, "2026-10-01 10:00:00", apks.toList())

    private fun apk(
        versionCode: Long,
        slug: String,
        architectures: List<String> = listOf("arm64-v8a"),
        minimumApi: Int = 26
    ) = ApkMirrorApk(
        versionCode = versionCode,
        description = "",
        link = "/apk/developer/app/release/$slug-android-apk-download/",
        publishDate = "",
        architectures = architectures,
        densities = listOf("nodpi"),
        minimumApi = minimumApi,
        capabilities = emptyList(),
        signatureSha1s = emptyList(),
        signatureSha256s = emptyList()
    )

    private companion object {
        const val PACKAGE = "com.example.app"
    }
}
