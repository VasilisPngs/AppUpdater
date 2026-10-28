package com.vasilispngs.appupdater.ui

import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.InstallEvent
import com.vasilispngs.appupdater.data.model.InstallState
import com.vasilispngs.appupdater.data.model.installEvent
import com.vasilispngs.appupdater.data.repository.ScanProblem
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
    fun `says why a bundle failed to install in the system installer's words`() {
        assertEquals("There's a problem with the app file", message(InstallState.Error(FILE_NAME, Failure.BadFile)))
        assertEquals("This app is not compatible with your device", message(InstallState.Error(FILE_NAME, Failure.Incompatible)))
        assertEquals("App not installed", message(InstallState.Error(FILE_NAME, Failure.NotInstalled)))
    }

    @Test
    fun `names the shared library to install first`() {
        assertEquals(
            "This app needs the shared library com.google.android.trichromelibrary_744404833. Install it first.",
            message(InstallState.Error(FILE_NAME, Failure.MissingLibrary("com.google.android.trichromelibrary_744404833")))
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
        assertEquals("The latest on Google Play is v142.0", InstallEvent.FinishedFromPlay("Chrome", "142.0").latestOnPlay(resources))
    }

    @Test
    fun `says why an update failed`() {
        assertEquals(
            "Google Play isn't available right now. Try again later.",
            InstallEvent.Failed(Failure.PlayUnavailable).text(resources)
        )
        assertEquals(
            "Couldn't reach auroraoss.com. Check your connection and any VPN, Private DNS, or content blocker.",
            InstallEvent.Failed(Failure.SignInUnreachable("auroraoss.com")).text(resources)
        )
    }

    @Test
    fun `lists the apps Google Play could not check`() {
        assertEquals("Couldn't check Chrome on Google Play", ScanProblem.PlayUnchecked(listOf("Chrome")).text(resources))
        assertEquals(
            "Couldn't check these apps on Google Play: Chrome, Maps, YouTube",
            ScanProblem.PlayUnchecked(listOf("Chrome", "Maps", "YouTube")).text(resources)
        )
    }

    private fun message(state: InstallState): String? = state.installEvent()?.text(resources)

    private companion object {
        const val FILE_NAME = "com.android.chrome_142.0.7444.48-744404833_apkmirror.com.apkm"
    }
}
