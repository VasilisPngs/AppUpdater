package com.vasilispngs.appupdater.data.installer

import com.aurora.gplayapi.AndroidAppDeliveryData
import com.aurora.gplayapi.SplitDeliveryData
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Base64
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

        val error = assertThrows(IOException::class.java) { val _ = stream.readBytes() }
        assertEquals("Google Play sent a damaged copy of base.apk.", error.message)
    }

    @Test
    fun `refuses a Google Play file that ends early`() {
        val stream = apk(sha256 = APK.digest("SHA-256").toHexString()).verified(ByteArrayInputStream(APK.copyOf(APK.size - 1))) {}

        assertThrows(IOException::class.java) { val _ = stream.readBytes() }
    }

    @Test
    fun `checks SHA-1 when Google Play sends no SHA-256`() {
        val stream = apk(sha1 = APK.digest("SHA-1").toHexString()).verified(ByteArrayInputStream(APK)) {}

        assertArrayEquals(APK, stream.readBytes())
    }

    @Test
    fun `refuses a Google Play file without a checksum`() {
        val error = assertThrows(IOException::class.java) { apk().verified(ByteArrayInputStream(APK)) {} }

        assertEquals("Google Play sent no checksum for base.apk.", error.message)
    }

    @Test
    fun `reads the full and compressed files of a Google Play delivery`() {
        val sha256 = APK.digest("SHA-256")
        val sha1 = APK.digest("SHA-1")
        val apks = AndroidAppDeliveryData.newBuilder()
            .setDownloadUrl("https://example.com/base")
            .setDownloadSize(1000)
            .setCompressedDownloadUrl("https://example.com/base.gz")
            .setCompressedSize(600)
            .setSha256(sha256.base64Url())
            .addSplitDeliveryData(
                SplitDeliveryData.newBuilder()
                    .setName("config.arm64_v8a")
                    .setDownloadUrl("https://example.com/split")
                    .setDownloadSize(500)
                    .setSha1(sha1.base64Url())
            )
            .build()
            .apks()

        assertEquals(listOf("base.apk", "config.arm64_v8a.apk"), apks.map { it.name })
        assertEquals(listOf("https://example.com/base", "https://example.com/split"), apks.map { it.url })
        assertEquals(listOf("https://example.com/base.gz", ""), apks.map { it.compressedUrl })
        assertEquals(listOf(1000L, 500L), apks.map { it.size })
        assertEquals(sha256.toHexString(), apks[0].sha256)
        assertEquals(sha1.toHexString(), apks[1].sha1)
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
        PlayApk("base.apk", "https://example.com/base", APK.size.toLong(), "", 0, sha1, sha256)

    private fun ByteArray.digest(algorithm: String): ByteArray = MessageDigest.getInstance(algorithm).digest(this)

    private fun ByteArray.base64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(this)

    private companion object {
        val APK = ByteArray(200_000) { (it % 251).toByte() }
    }
}
