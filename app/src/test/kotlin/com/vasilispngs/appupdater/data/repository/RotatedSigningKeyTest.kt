package com.vasilispngs.appupdater.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import com.aurora.gplayapi.device.DeviceRegistry
import com.aurora.gplayapi.engine.OkHttpEngine
import com.vasilispngs.appupdater.data.model.ApkMirrorApk
import com.vasilispngs.appupdater.data.model.ApkMirrorApp
import com.vasilispngs.appupdater.data.model.InstalledApp
import com.vasilispngs.appupdater.data.play.PlayCatalog
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class RotatedSigningKeyTest {
    private val context = RuntimeEnvironment.getApplication()
    private val engine = OkHttpEngine(OkHttpClient())
    private val repository = AppUpdateRepository(
        context,
        PlayCatalog(lazyOf(engine), lazyOf(DeviceRegistry.default))
    )
    private val selector = ApkMirrorSelector(
        sdk = 36,
        abis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
        densityDpi = 450,
        isTelevision = false,
        isAutomotive = false,
        isDesktop = false
    )

    @Test
    fun `matches Google Play against every key and APKMirror against the current key`() = runTest {
        val installed = installRotated()

        assertEquals(setOf(sha1(NEW_KEY)), installed.signatureSha1s)
        assertEquals(setOf(sha1(OLD_KEY), sha1(NEW_KEY)), installed.historySha1s)
    }

    @Test
    fun `skips a build signed only with the key the app rotated away from`() = runTest {
        val installed = installRotated()
        val legacy = apk(262640283, minimumApi = 24, key = OLD_KEY)

        assertNull(selector.select(release(legacy), installed, requiresTelevisionBuild = false))
        assertEquals(
            262640293L,
            selector.select(
                release(legacy, apk(262640293, minimumApi = 32, key = NEW_KEY)),
                installed,
                requiresTelevisionBuild = false
            )?.apk?.versionCode
        )
    }

    private suspend fun installRotated(): InstalledApp {
        shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                packageName = PACKAGE
                versionName = "1.57.262640093"
                longVersionCode = 262640093
                applicationInfo = ApplicationInfo().apply {
                    packageName = PACKAGE
                    minSdkVersion = 31
                }
                signingInfo = SigningInfo().also {
                    shadowOf(it).setSignatures(arrayOf(NEW_KEY))
                    shadowOf(it).setPastSigningCertificates(arrayOf(OLD_KEY, NEW_KEY))
                }
            }
        )
        return requireNotNull(repository.getInstalledApp(PACKAGE))
    }

    private fun release(vararg apks: ApkMirrorApk) =
        ApkMirrorApp(PACKAGE, "Google Play Services for AR", "1.57.2626402", "2026-10-30 22:59:00", apks.toList())

    private fun apk(versionCode: Long, minimumApi: Int, key: Signature) = ApkMirrorApk(
        versionCode = versionCode,
        description = "",
        link = "/apk/google-inc/arcore/google-play-services-for-ar-1-57-2626402-release/" +
            "google-play-services-for-ar-1-57-$versionCode-android-apk-download/",
        publishDate = "",
        architectures = listOf("arm64-v8a", "armeabi-v7a"),
        densities = listOf("nodpi"),
        minimumApi = minimumApi,
        capabilities = emptyList(),
        signatureSha1s = listOf(sha1(key)),
        signatureSha256s = emptyList()
    )

    private fun sha1(key: Signature): String = MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).toHexString()

    private companion object {
        const val PACKAGE = "com.google.ar.core"
        val OLD_KEY = Signature(byteArrayOf(1, 2, 3))
        val NEW_KEY = Signature(byteArrayOf(4, 5, 6))
    }
}
