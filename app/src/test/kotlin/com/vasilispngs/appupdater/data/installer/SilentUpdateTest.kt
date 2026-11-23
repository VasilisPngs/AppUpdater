package com.vasilispngs.appupdater.data.installer

import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Looper
import com.vasilispngs.appupdater.data.model.ErrorCode
import com.vasilispngs.appupdater.data.model.FailureException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowPackageInstaller
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [SilentUpdateTest.Installer::class, SilentUpdateTest.Session::class])
class SilentUpdateTest {
    private val dispatcher = StandardTestDispatcher()
    private val installer = PackageInstallerManager(RuntimeEnvironment.getApplication(), dispatcher)

    @After
    fun reset() {
        Installer.requested.clear()
        Session.statuses.clear()
    }

    @Test
    fun `asks the user when the system refuses to update without asking`() = runTest(dispatcher) {
        Session.statuses += listOf(status(PackageInstaller.STATUS_FAILURE_ABORTED, "INSTALL_FAILED_ABORTED: Permission denied"))
        Session.statuses += listOf(status(PackageInstaller.STATUS_SUCCESS).putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, PACKAGE))

        assertEquals(PACKAGE, install().getOrThrow())
        assertEquals(
            listOf(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED, PackageInstaller.SessionParams.USER_ACTION_REQUIRED),
            Installer.requested
        )
    }

    @Test
    fun `does not ask again after the user cancels`() = runTest(dispatcher) {
        Session.statuses += listOf(
            status(PackageInstaller.STATUS_PENDING_USER_ACTION).putExtra(Intent.EXTRA_INTENT, Intent(CONFIRM)),
            status(PackageInstaller.STATUS_FAILURE_ABORTED, "INSTALL_FAILED_ABORTED: User rejected permissions")
        )

        val failure = install().exceptionOrNull() as FailureException

        assertEquals(ErrorCode.Named("INSTALL_FAILED_ABORTED"), failure.code)
        assertEquals(listOf(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED), Installer.requested)
    }

    private suspend fun TestScope.install(): Result<String> {
        val apk = ApkSource("base.apk", 3) { "apk".byteInputStream() }
        val result = async { installer.install(listOf(apk), packageName = PACKAGE) }
        while (!result.isCompleted) {
            shadowOf(Looper.getMainLooper()).idle()
            yield()
        }
        return result.await()
    }

    private fun status(code: Int, message: String? = null) =
        Intent().putExtra(PackageInstaller.EXTRA_STATUS, code).putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, message)

    @Implements(PackageInstaller::class)
    class Installer : ShadowPackageInstaller() {
        @Implementation
        override fun createSession(params: PackageInstaller.SessionParams): Int {
            requested += ReflectionHelpers.getField<Int>(params, "requireUserAction")
            return super.createSession(params)
        }

        companion object {
            val requested = mutableListOf<Int>()
        }
    }

    @Implements(PackageInstaller.Session::class)
    class Session : ShadowPackageInstaller.ShadowSession() {
        @Implementation
        override fun commit(statusReceiver: IntentSender) {
            statuses.removeFirst().forEach { statusReceiver.sendIntent(RuntimeEnvironment.getApplication(), 0, it, null, null) }
        }

        companion object {
            val statuses = ArrayDeque<List<Intent>>()
        }
    }

    private companion object {
        const val PACKAGE = "com.example.app"
        const val CONFIRM = "android.content.pm.action.CONFIRM_INSTALL"
    }
}
