package com.vasilispngs.appupdater.data.installer

import android.os.storage.StorageManager
import android.system.ErrnoException
import android.system.OsConstants
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.play.playUpdateFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.IOException
import java.io.InputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [NoRoomTest.Room::class])
class NoRoomTest {
    private val installer = PackageInstallerManager(RuntimeEnvironment.getApplication(), Dispatchers.Unconfined)

    @Test
    fun `says to free up space when the device has no room for the update`() = runTest {
        Room.bytes = APK_SIZE - 1

        val failure = installer.install(listOf(failingSource(IOException("write failed"))), packageName = PACKAGE).exceptionOrNull()

        assertEquals(Failure.Storage, failure?.playUpdateFailure())
    }

    @Test
    fun `keeps a lost connection when the device has room`() = runTest {
        Room.bytes = APK_SIZE

        val failure = installer.install(listOf(failingSource(IOException("Connection reset"))), packageName = PACKAGE).exceptionOrNull()

        assertEquals(Failure.PlayUnreachable, failure?.playUpdateFailure())
    }

    @Test
    fun `says to free up space when a bundle does not fit in the cache`() {
        assertEquals(Failure.Storage, IOException("write failed", ErrnoException("write", OsConstants.ENOSPC)).bundleFailure())
        assertEquals(Failure.BadFile, IOException("Truncated ZIP file").bundleFailure())
    }

    private fun failingSource(failure: IOException) =
        ApkSource("base.apk", APK_SIZE) { object : InputStream() { override fun read(): Int = throw failure } }

    @Implements(StorageManager::class)
    class Room {
        @Implementation
        fun getAllocatableBytes(storageUuid: UUID): Long = bytes.also { check(storageUuid == StorageManager.UUID_DEFAULT) }

        companion object {
            var bytes = 0L
        }
    }

    private companion object {
        const val PACKAGE = "com.example.app"
        const val APK_SIZE = 50_000_000L
    }
}
