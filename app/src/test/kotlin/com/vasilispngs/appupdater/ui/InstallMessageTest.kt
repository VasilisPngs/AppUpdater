package com.vasilispngs.appupdater.ui

import com.vasilispngs.appupdater.data.model.InstallEvent
import com.vasilispngs.appupdater.data.model.InstallState
import com.vasilispngs.appupdater.data.model.installEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InstallMessageTest {
    private val resources = RuntimeEnvironment.getApplication().resources

    @Test
    fun `says a bundle installed a new app`() {
        assertEquals("Chrome was installed", message(InstallState.Success("Chrome", updated = false)))
    }

    @Test
    fun `says a bundle updated an app`() {
        assertEquals("Chrome was updated", message(InstallState.Success("Chrome", updated = true)))
    }

    @Test
    fun `says why a bundle failed to install`() {
        assertEquals(
            "Installation failed: This file is not an APK or an APK bundle.",
            message(InstallState.Error(FILE_NAME, "This file is not an APK or an APK bundle."))
        )
    }

    @Test
    fun `says nothing while a bundle installs`() {
        assertNull(InstallState.Installing(FILE_NAME).installEvent())
    }

    @Test
    fun `says which version Google Play updated an app to`() {
        assertEquals(
            "Chrome was updated to v142.0, the latest on Google Play",
            InstallEvent.FinishedFromPlay("Chrome", "142.0").text(resources)
        )
    }

    @Test
    fun `says why an update failed`() {
        assertEquals(
            "Update failed: Google Play: HTTP 429",
            InstallEvent.Failed("Google Play: HTTP 429").text(resources)
        )
    }

    private fun message(state: InstallState): String? = state.installEvent()?.text(resources)

    private companion object {
        const val FILE_NAME = "com.android.chrome_142.0.7444.48-744404833_apkmirror.com.apkm"
    }
}
