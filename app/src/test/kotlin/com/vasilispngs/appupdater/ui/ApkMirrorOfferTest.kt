package com.vasilispngs.appupdater.ui

import com.vasilispngs.appupdater.data.model.AppUpdateInfo
import com.vasilispngs.appupdater.data.model.PlayVersion
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkMirrorOfferTest {
    private val updates = listOf(
        update(MIRROR_APP, 200, apkMirrorUrl = "https://www.apkmirror.com/apk/developer/app/app-2-0-release/"),
        update(PLAY_APP, 300, playUpdate = PlayVersion("3.0", 300))
    )

    @Test
    fun `installs without asking the APKMirror build it offers`() {
        assertTrue(updates.offersFromApkMirror(MIRROR_APP, 200))
    }

    @Test
    fun `asks for another build of an app it offers from APKMirror`() {
        assertFalse(updates.offersFromApkMirror(MIRROR_APP, 201))
    }

    @Test
    fun `asks for an app it offers only from Google Play`() {
        assertFalse(updates.offersFromApkMirror(PLAY_APP, 300))
    }

    @Test
    fun `asks for an app it offers no update for`() {
        assertFalse(updates.offersFromApkMirror(OTHER_APP, 200))
    }

    private fun update(
        packageName: String,
        versionCode: Long,
        apkMirrorUrl: String? = null,
        playUpdate: PlayVersion? = null
    ) = AppUpdateInfo(
        packageName = packageName,
        appName = packageName,
        newVersionName = "",
        newVersionCode = versionCode,
        publishedAt = null,
        apkMirrorUrl = apkMirrorUrl,
        playAvailable = playUpdate != null,
        playUpdate = playUpdate
    )

    private companion object {
        const val MIRROR_APP = "com.example.mirror"
        const val PLAY_APP = "com.example.play"
        const val OTHER_APP = "com.example.other"
    }
}
