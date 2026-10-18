package com.android.appupdater.data.installer

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import java.util.zip.ZipEntry

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-xxhdpi")
class SplitSelectionTest {
    private val installer = RuntimeEnvironment.getApplication().let { BundleInstaller(it, PackageInstallerManager(it)) }

    @Before
    fun setUp() {
        ShadowBuild.setSupportedAbis(arrayOf("arm64-v8a"))
    }

    @Test
    fun `keeps only the device configuration of feature modules in an APKMirror bundle`() {
        assertEquals(
            listOf(
                "base.apk",
                "split_phonesky_data_loader.apk",
                "split_config.arm64_v8a.apk",
                "split_phonesky_data_loader.config.arm64_v8a.apk",
                "split_config.xxhdpi.apk",
                "split_config.en.apk"
            ),
            select(
                "base.apk",
                "split_config.arm64_v8a.apk",
                "split_config.armeabi_v7a.apk",
                "split_config.x86.apk",
                "split_config.mdpi.apk",
                "split_config.xxhdpi.apk",
                "split_config.de.apk",
                "split_config.en.apk",
                "split_phonesky_data_loader.apk",
                "split_phonesky_data_loader.config.arm64_v8a.apk",
                "split_phonesky_data_loader.config.armeabi_v7a.apk",
                "split_phonesky_data_loader.config.x86.apk"
            )
        )
    }

    @Test
    fun `keeps only the device configuration of feature modules in an XAPK`() {
        assertEquals(
            listOf(
                "com.zhiliaoapp.musically.apk",
                "df_kakao.apk",
                "config.arm64_v8a.apk",
                "df_kakao.config.arm64_v8a.apk",
                "config.xxhdpi.apk"
            ),
            select(
                "com.zhiliaoapp.musically.apk",
                "config.arm64_v8a.apk",
                "config.x86.apk",
                "config.xxhdpi.apk",
                "df_kakao.apk",
                "df_kakao.config.arm64_v8a.apk",
                "df_kakao.config.x86.apk"
            )
        )
    }

    @Test
    fun `keeps a base named after a package with a config segment`() {
        assertEquals(
            listOf("com.config.app.apk", "config.arm64_v8a.apk"),
            select("com.config.app.apk", "config.arm64_v8a.apk", "config.x86.apk")
        )
    }

    private fun select(vararg names: String): List<String> =
        installer.selectSplits(names.map(::ZipEntry)).map(ZipEntry::getName)
}
