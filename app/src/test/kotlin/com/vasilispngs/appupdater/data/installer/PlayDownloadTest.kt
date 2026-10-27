package com.vasilispngs.appupdater.data.installer

import com.aurora.gplayapi.AndroidAppDeliveryData
import com.aurora.gplayapi.AndroidAppPatchData
import com.aurora.gplayapi.SplitDeliveryData
import com.aurora.gplayapi.data.models.PlayFile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class PlayDownloadTest {

    @Test
    fun `passes on a Google Play file whose SHA-256 matches and counts every byte`() {
        val counted = mutableListOf<Int>()
        val stream = file(sha256 = APK.digest("SHA-256")).verified(ByteArrayInputStream(APK)) { counted += it }

        assertArrayEquals(APK, stream.readBytes())
        assertEquals(APK.size, counted.sum())
    }

    @Test
    fun `refuses a damaged copy of a Google Play file`() {
        val damaged = APK.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }
        val stream = file(sha256 = APK.digest("SHA-256")).verified(ByteArrayInputStream(damaged)) {}

        val error = assertThrows(IOException::class.java) { val _ = stream.readBytes() }
        assertEquals("Google Play sent a damaged copy of base.apk.", error.message)
    }

    @Test
    fun `refuses a Google Play file that ends early`() {
        val stream = file(sha256 = APK.digest("SHA-256")).verified(ByteArrayInputStream(APK.copyOf(APK.size - 1))) {}

        assertThrows(IOException::class.java) { val _ = stream.readBytes() }
    }

    @Test
    fun `checks SHA-1 when Google Play sends no SHA-256`() {
        val stream = file(sha1 = APK.digest("SHA-1")).verified(ByteArrayInputStream(APK)) {}

        assertArrayEquals(APK, stream.readBytes())
    }

    @Test
    fun `refuses a Google Play file without a checksum`() {
        val error = assertThrows(IOException::class.java) { file().verified(ByteArrayInputStream(APK)) {} }

        assertEquals("Google Play sent no checksum for base.apk.", error.message)
    }

    @Test
    fun `lists the sizes Google Play offers`() {
        val data = AndroidAppDeliveryData.newBuilder()
            .setDownloadSize(1000)
            .setCompressedDownloadUrl("https://example.com/base")
            .setCompressedSize(600)
            .setPatchData(AndroidAppPatchData.newBuilder().setPatchFormat(3).setMaxPatchSize(200))
            .addSplitDeliveryData(SplitDeliveryData.newBuilder().setDownloadSize(500))
            .build()

        assertEquals(
            "Google Play offers: full 1500 B in 2 files · compressed 600 B for 1 · compressed data none · " +
                "delta 200 B for 1, format 3 · download gzip",
            deliveryOffers(data, "gzip") { "$it B" }
        )
    }

    private fun file(sha1: String = "", sha256: String = "") =
        PlayFile(name = "base.apk", size = APK.size.toLong(), sha1 = sha1, sha256 = sha256)

    private fun ByteArray.digest(algorithm: String): String = MessageDigest.getInstance(algorithm).digest(this).toHexString()

    private companion object {
        val APK = ByteArray(200_000) { (it % 251).toByte() }
    }
}
