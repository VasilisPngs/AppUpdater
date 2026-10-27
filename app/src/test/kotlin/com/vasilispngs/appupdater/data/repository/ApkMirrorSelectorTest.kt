package com.vasilispngs.appupdater.data.repository

import com.vasilispngs.appupdater.data.model.ApkMirrorApk
import com.vasilispngs.appupdater.data.model.ApkMirrorApp
import com.vasilispngs.appupdater.data.model.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApkMirrorSelectorTest {
    private val phone = ApkMirrorSelector(
        sdk = 36,
        abis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
        densityDpi = 420,
        isTelevision = false,
        isAutomotive = false,
        isDesktop = false
    )

    private val television = ApkMirrorSelector(
        sdk = 36,
        abis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
        densityDpi = 320,
        isTelevision = true,
        isAutomotive = false,
        isDesktop = false
    )

    private val desktop = ApkMirrorSelector(
        sdk = 36,
        abis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
        densityDpi = 160,
        isTelevision = false,
        isAutomotive = false,
        isDesktop = true
    )

    @Test
    fun `keeps the device ABI when another ABI shares the installed decimal suffix`() {
        val release = release(
            "144.0",
            apk(2016120039, "firefox-144-0", architectures = listOf("armeabi-v7a")),
            apk(2016120041, "firefox-144-0-2", architectures = listOf("arm64-v8a")),
            apk(2016120043, "firefox-144-0-3", architectures = listOf("x86_64"))
        )

        assertEquals(2016120041L, select(release, installed("143.0.2", 2016115639))?.apk?.versionCode)
    }

    @Test
    fun `keeps the installed variant among builds for the same ABIs`() {
        val release = release(
            "142.0.7444.48",
            apk(744404833, "chrome-142-0-7444-48", architectures = listOf("arm64-v8a", "armeabi-v7a"), minimumApi = 29),
            apk(744404838, "chrome-142-0-7444-48-2", architectures = listOf("arm64-v8a", "armeabi-v7a"), minimumApi = 29),
            apk(744404843, "chrome-142-0-7444-48-3", architectures = listOf("arm64-v8a"), minimumApi = 29)
        )

        assertEquals(744404833L, select(release, installed("141.0.7390.122", 739012233, minSdk = 29))?.apk?.versionCode)
    }

    @Test
    fun `prefers the family of the installed minimum SDK`() {
        val release = release(
            "25.42.31",
            apk(254231141, "google-play-services-25-42-31", minimumApi = 31),
            apk(254231150, "google-play-services-25-42-31-2", minimumApi = 34)
        )

        assertEquals(254231141L, select(release, installed("25.40.32", 254032040, minSdk = 31))?.apk?.versionCode)
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

    @Test
    fun `skips the installed version when the release name shortens its build number`() {
        val release = release("1.57.2626400", apk(262640093, "app-1-57-262640093"))

        assertNull(select(release, installed("1.57.262640083", 262640083)))
    }

    @Test
    fun `skips an open beta build inside a stable release`() {
        val release = release(
            "20261013_00_RC00",
            apk(325565063, "messages-android_20261013_00_rc00-phone_dynamic"),
            apk(325565100, "messages-android_20261013_00_rc00-phone-openbeta_dynamic")
        )

        assertEquals(
            325565063L,
            select(release, installed("messages.android_20261006_00_RC00.phone_dynamic", 325500063))?.apk?.versionCode
        )
    }

    @Test
    fun `offers a build signed with the installed key`() {
        val release = release("2.0", apk(200, "app-2-0", sha256s = listOf(INSTALLED_KEY)))

        assertEquals(200L, select(release, installed("1.9", 190, sha256s = setOf(INSTALLED_KEY)))?.apk?.versionCode)
    }

    @Test
    fun `rejects a build signed with another key`() {
        val release = release("2.0", apk(200, "app-2-0", sha256s = listOf(OTHER_KEY)))

        assertNull(select(release, installed("1.9", 190, sha256s = setOf(INSTALLED_KEY))))
    }

    @Test
    fun `compares SHA-1 signatures when SHA-256 ones are missing`() {
        val release = release("2.0", apk(200, "app-2-0", sha1s = listOf(OTHER_KEY)))

        assertNull(select(release, installed("1.9", 190, sha1s = setOf(INSTALLED_KEY))))
    }

    @Test
    fun `prefers the build for the device density`() {
        val release = release(
            "2.0",
            apk(200, "app-2-0", densities = listOf("320dpi")),
            apk(201, "app-2-0-2", densities = listOf("480dpi")),
            apk(202, "app-2-0-3", densities = listOf("nodpi"))
        )

        assertEquals(201L, select(release, installed("1.9", 190))?.apk?.versionCode)
    }

    @Test
    fun `prefers a universal build over one for another density`() {
        val release = release(
            "2.0",
            apk(203, "app-2-0-4", densities = listOf("320dpi")),
            apk(202, "app-2-0-3", densities = listOf("nodpi"))
        )

        assertEquals(202L, select(release, installed("1.9", 190))?.apk?.versionCode)
    }

    @Test
    fun `prefers a build for every ABI over one for another density`() {
        val release = release(
            "2.0",
            apk(201, "app-2-0-90h", densities = listOf("160dpi")),
            apk(200, "app-2-0-90", architectures = listOf("universal"), densities = listOf("120-480dpi"))
        )

        assertEquals(200L, select(release, installed("1.9", 190))?.apk?.versionCode)
    }

    @Test
    fun `keeps the television build of an app installed from its television variant`() {
        val release = release(
            "2.0",
            apk(201, "app-2-0-2"),
            apk(200, "app-2-0", capabilities = listOf("leanback"))
        )

        assertEquals(200L, television.select(release, installed("1.9", 190), requiresTelevisionBuild = true)?.apk?.versionCode)
    }

    @Test
    fun `skips television-only builds on phones`() {
        val release = release("2.0", apk(200, "app-2-0", capabilities = listOf("leanback_standalone")))

        assertNull(select(release, installed("1.9", 190)))
    }

    @Test
    fun `skips Wear OS builds`() {
        val release = release("2.0", apk(200, "app-2-0", capabilities = listOf("wear_standalone")))

        assertNull(select(release, installed("1.9", 190)))
    }

    @Test
    fun `skips Android Automotive builds on phones`() {
        val release = release("2.0", apk(200, "app-2-0", capabilities = listOf("android.hardware.type.automotive")))

        assertNull(select(release, installed("1.9", 190)))
    }

    @Test
    fun `skips APKMirror's desktop builds on phones`() {
        val release = release("100.0.982605956", apk(2099000023, "youtube-desktop-100-0-982605956"))

        assertNull(select(release.copy(name = "YouTube (Desktop)"), installed("21.42.248", 1561304986)))
        assertEquals(2099000023L, select(release, installed("21.42.248", 1561304986))?.apk?.versionCode)
    }

    @Test
    fun `offers APKMirror's desktop builds on desktops`() {
        val release = release("100.0.982605956", apk(2099000023, "youtube-desktop-100-0-982605956")).copy(name = "YouTube (Desktop)")

        assertEquals(
            2099000023L,
            desktop.select(release, installed("100.0.982000000", 2099000022), requiresTelevisionBuild = false)?.apk?.versionCode
        )
    }

    private fun select(release: ApkMirrorApp, installed: InstalledApp) = phone.select(release, installed, requiresTelevisionBuild = false)

    private fun installed(
        versionName: String,
        versionCode: Long,
        minSdk: Int = 26,
        sha1s: Set<String> = emptySet(),
        sha256s: Set<String> = emptySet()
    ) = InstalledApp(
        packageName = PACKAGE,
        versionName = versionName,
        versionCode = versionCode,
        minSdk = minSdk,
        signatureSha1s = sha1s,
        signatureSha256s = sha256s,
        isEnabled = true,
        isTelevisionBuild = false
    )

    private fun release(versionName: String, vararg apks: ApkMirrorApk) =
        ApkMirrorApp(PACKAGE, "App", versionName, "2026-10-01 10:00:00", apks.toList())

    private fun apk(
        versionCode: Long,
        slug: String,
        architectures: List<String> = listOf("arm64-v8a"),
        minimumApi: Int = 26,
        densities: List<String> = listOf("nodpi"),
        capabilities: List<String> = emptyList(),
        sha1s: List<String> = emptyList(),
        sha256s: List<String> = emptyList()
    ) = ApkMirrorApk(
        versionCode = versionCode,
        description = "",
        link = "/apk/developer/app/release/$slug-android-apk-download/",
        publishDate = "",
        architectures = architectures,
        densities = densities,
        minimumApi = minimumApi,
        capabilities = capabilities,
        signatureSha1s = sha1s,
        signatureSha256s = sha256s
    )

    private companion object {
        const val PACKAGE = "com.example.app"
        const val INSTALLED_KEY = "installed"
        const val OTHER_KEY = "other"
    }
}
