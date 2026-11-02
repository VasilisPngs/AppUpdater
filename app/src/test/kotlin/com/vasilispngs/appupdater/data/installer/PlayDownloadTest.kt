package com.vasilispngs.appupdater.data.installer

import com.aurora.gplayapi.model.AppFile
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.FailureException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
class PlayDownloadTest {

    @Test
    fun `passes on a Google Play file whose SHA-256 matches and counts every byte`() {
        val counted = mutableListOf<Int>()
        val stream = apk(sha256 = APK.digest("SHA-256").toHexString()).verified(ByteArrayInputStream(APK)) { counted += it }

        assertArrayEquals(APK, stream.readBytes())
        assertEquals(APK.size, counted.sum())
    }

    @Test
    fun `refuses a damaged copy of a Google Play file`() {
        val damaged = APK.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }
        val stream = apk(sha256 = APK.digest("SHA-256").toHexString()).verified(ByteArrayInputStream(damaged)) {}

        val error = assertThrows(FailureException::class.java) { val _ = stream.readBytes() }
        assertEquals(Failure.Unverified, error.failure)
    }

    @Test
    fun `refuses a Google Play file that ends early`() {
        val stream = apk(sha256 = APK.digest("SHA-256").toHexString()).verified(ByteArrayInputStream(APK.copyOf(APK.size - 1))) {}

        assertThrows(FailureException::class.java) { val _ = stream.readBytes() }
    }

    @Test
    fun `checks SHA-1 when Google Play sends no SHA-256`() {
        val stream = apk(sha1 = APK.digest("SHA-1").toHexString()).verified(ByteArrayInputStream(APK)) {}

        assertArrayEquals(APK, stream.readBytes())
    }

    @Test
    fun `refuses a Google Play file without a checksum`() {
        val error = assertThrows(FailureException::class.java) { apk().verified(ByteArrayInputStream(APK)) {} }

        assertEquals(Failure.Unverified, error.failure)
    }

    @Test
    fun `installs the APKs of a Google Play delivery and leaves its expansion files`() {
        val files = listOf(
            AppFile(name = "base.apk", url = "https://example.com/base", type = AppFile.Type.BASE),
            AppFile(name = "main.1.com.example.obb", url = "https://example.com/obb", type = AppFile.Type.OBB),
            AppFile(name = "config.arm64_v8a.apk", url = "https://example.com/split", type = AppFile.Type.SPLIT)
        )

        assertEquals(listOf("base.apk", "config.arm64_v8a.apk"), files.apks().map { it.name })
    }

    @Test
    fun `says Google Play does not offer a delivery without a base APK`() {
        val files = listOf(AppFile(name = "config.arm64_v8a.apk", url = "https://example.com/split", type = AppFile.Type.SPLIT))

        val error = assertThrows(FailureException::class.java) { val _ = files.apks() }
        assertEquals(Failure.NotOffered, error.failure)
    }

    @Test
    fun `unpacks a gzip download from Google Play`() {
        val gzip = ByteArrayOutputStream().also { output -> GZIPOutputStream(output).use { it.write(APK) } }.toByteArray()

        assertArrayEquals(APK, checkNotNull(ByteArrayInputStream(gzip).gunzipped()).readBytes())
    }

    @Test
    fun `leaves a download that is not gzip to the full file`() {
        assertNull(ByteArrayInputStream(APK).gunzipped())
    }

    private fun apk(sha1: String = "", sha256: String = "") =
        AppFile(name = "base.apk", url = "https://example.com/base", size = APK.size.toLong(), sha1 = sha1, sha256 = sha256)

    private fun ByteArray.digest(algorithm: String): ByteArray = MessageDigest.getInstance(algorithm).digest(this)

    private companion object {
        val APK = ByteArray(200_000) { (it % 251).toByte() }
    }
}
