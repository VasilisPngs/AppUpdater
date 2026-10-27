package com.vasilispngs.appupdater.data.installer

import android.content.Context
import android.content.pm.PackageManager
import android.text.format.Formatter
import android.util.Base64
import com.aurora.gplayapi.AndroidAppDeliveryData
import com.aurora.gplayapi.AndroidAppPatchData
import com.aurora.gplayapi.Constants.PatchFormat
import com.aurora.gplayapi.DeliveryResponse
import com.aurora.gplayapi.exceptions.GooglePlayException
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.vasilispngs.appupdater.data.api.SharedHttpClient
import com.vasilispngs.appupdater.data.play.PlayCatalog
import com.vasilispngs.appupdater.data.play.PlayHttpClient
import com.vasilispngs.appupdater.data.play.deliveryFailure
import com.vasilispngs.appupdater.data.play.playCertificateHash
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

class PlayInstaller(
    private val context: Context,
    private val catalog: PlayCatalog,
    private val httpClient: PlayHttpClient,
    private val packageInstaller: PackageInstallerManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val packageManager = context.packageManager

    suspend fun install(
        packageName: String,
        versionCode: Long,
        preapproved: Int?,
        onSize: (Long) -> Unit,
        onMeasured: (String) -> Unit,
        onProgress: (Float?) -> Unit
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            onProgress(null)
            val delivery = runInterruptible {
                val (session, details) = catalog.details(packageName)
                val purchases = PurchaseHelper(session).using(httpClient)
                val certificate = packageManager.playCertificateHash(packageName)
                val installedVersion = try {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode
                } catch (_: PackageManager.NameNotFoundException) {
                    0L
                }

                val app = deliver(purchases, packageName, versionCode, details.offerType, certificate)
                val libraries = details.dependencies.dependentLibraries
                    .map { library ->
                        library.packageName to if (versionCode == details.versionCode) library.versionCode else versionCode
                    }
                    .filter { (name, code) -> name.isNotBlank() && code > 0 && !isSharedLibraryInstalled(name, code) }
                    .map { (name, code) -> deliver(purchases, name, code, LIBRARY_OFFER_TYPE, certificate) }
                Delivery(app, libraries) { deltas(purchases, packageName, installedVersion, versionCode, details.offerType, certificate) }
            }

            val apks = delivery.app + delivery.libraries.flatten()
            onSize(apks.sumOf { if (it.compressedUrl.isEmpty()) it.size else it.compressedSize })
            val progress = Progress(apks.sumOf { it.size }.coerceAtLeast(1), onProgress)
            val compressed = mutableListOf<PlayApk>()
            val onCompressed: (PlayApk) -> Unit = { compressed += it }
            delivery.libraries.forEach { files -> val _ = packageInstaller.install(sources(files, progress, onCompressed)).getOrThrow() }
            val installed = packageInstaller.install(
                sources(delivery.app, progress, onCompressed),
                preapproved,
                packageName = packageName
            )
            val deltas = runInterruptible { delivery.deltas() }
            onMeasured(measurement(compressed, apks.size, deltas) { Formatter.formatShortFileSize(context, it) })
            val _ = installed.getOrThrow()
            Result.success(Unit)
        } catch (exception: CancellationException) {
            preapproved?.let(packageInstaller::abandon)
            throw exception
        } catch (exception: Exception) {
            preapproved?.let(packageInstaller::abandon)
            Result.failure(exception)
        }
    }

    private fun deliver(
        purchases: PurchaseHelper,
        packageName: String,
        versionCode: Long,
        offerType: Int,
        certificate: String?
    ): List<PlayApk> {
        val data = try {
            val _ = runCatching { purchases.acquire(packageName, versionCode, offerType) }
            purchases.getDeliveryResponse(
                packageName = packageName,
                updateVersionCode = versionCode,
                offerType = offerType,
                deliveryToken = purchases.getDeliveryToken(packageName, versionCode, offerType, certificate),
                certificateHash = certificate
            ).delivered()
        } catch (exception: GooglePlayException) {
            throw exception.deliveryFailure(packageName, versionCode)
        }
        if (data.downloadUrl.isEmpty()) throw IllegalStateException("Google Play returned no installable file.")
        return data.apks()
    }

    private fun deltas(
        purchases: PurchaseHelper,
        packageName: String,
        installedVersion: Long,
        versionCode: Long,
        offerType: Int,
        certificate: String?
    ): List<Delta> = PatchFormat.entries.map { format ->
        Delta(
            format.value,
            try {
                val response = purchases.getDeliveryResponse(
                    packageName = packageName,
                    installedVersionCode = installedVersion,
                    updateVersionCode = versionCode,
                    offerType = offerType,
                    patchFormat = format,
                    deliveryToken = purchases.getDeliveryToken(packageName, versionCode, offerType, certificate),
                    certificateHash = certificate
                )
                if (response.hasAppDeliveryData()) {
                    Result.success(response.appDeliveryData.patches())
                } else {
                    Result.failure(IllegalStateException("status ${response.status}"))
                }
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        )
    }

    private fun sources(apks: List<PlayApk>, progress: Progress, onCompressed: (PlayApk) -> Unit): List<ApkSource> =
        apks.map { apk ->
            ApkSource(apk.name, apk.size) { apk.verified(apk.download(onCompressed), progress::advance) }
        }

    private fun PlayApk.download(onCompressed: (PlayApk) -> Unit): InputStream {
        if (compressedUrl.isNotEmpty()) {
            val response = fetch(compressedUrl, identity = true)
            if (response.isSuccessful) {
                response.body.byteStream().gunzipped()?.let { stream ->
                    onCompressed(this)
                    return stream
                }
            } else {
                response.close()
            }
        }
        val response = fetch(url)
        if (!response.isSuccessful) {
            response.close()
            throw IOException("Google Play download failed (${response.code}).")
        }
        return response.body.byteStream()
    }

    private fun fetch(url: String, identity: Boolean = false): Response =
        SharedHttpClient.instance.newCall(
            Request.Builder().url(url).apply { if (identity) header("Accept-Encoding", "identity") }.build()
        ).execute()

    private fun isSharedLibraryInstalled(packageName: String, versionCode: Long): Boolean =
        packageManager.getSharedLibraries(PackageManager.PackageInfoFlags.of(0))
            .any { it.name == packageName && it.longVersion == versionCode }

    private class Delivery(val app: List<PlayApk>, val libraries: List<List<PlayApk>>, val deltas: () -> List<Delta>)

    private class Progress(private val totalBytes: Long, private val onProgress: (Float?) -> Unit) {
        private var writtenBytes = 0L
        private var reportedPercent = -1

        fun advance(bytes: Int) {
            writtenBytes += bytes
            val percent = (writtenBytes * PERCENT / totalBytes).toInt()
            if (percent != reportedPercent) {
                reportedPercent = percent
                onProgress(if (writtenBytes < totalBytes) percent / PERCENT.toFloat() else null)
            }
        }
    }

    private companion object {
        const val LIBRARY_OFFER_TYPE = 0
        const val PERCENT = 100
    }
}

internal class PlayApk(
    val name: String,
    val url: String,
    val size: Long,
    val compressedUrl: String,
    val compressedSize: Long,
    val sha1: String,
    val sha256: String
)

internal class Delta(val format: Int, val patches: Result<List<AndroidAppPatchData>>)

internal fun AndroidAppDeliveryData.apks(): List<PlayApk> =
    listOf(PlayApk("base.apk", downloadUrl, downloadSize, compressedDownloadUrl, compressedSize, sha1.decodedHash(), sha256.decodedHash())) +
        splitDeliveryDataList.mapIndexed { index, split ->
            PlayApk(
                "${split.name.ifBlank { "split_$index" }}.apk",
                split.downloadUrl,
                split.downloadSize,
                split.compressedDownloadUrl,
                split.compressedSize,
                split.sha1.decodedHash(),
                split.sha256.decodedHash()
            )
        }

internal fun InputStream.gunzipped(): InputStream? {
    val buffered = BufferedInputStream(this, COPY_BUFFER_SIZE)
    buffered.mark(GZIP_MAGIC_BYTES)
    val magic = buffered.read() or (buffered.read() shl Byte.SIZE_BITS)
    buffered.reset()
    if (magic == GZIPInputStream.GZIP_MAGIC) return GZIPInputStream(buffered, COPY_BUFFER_SIZE)
    buffered.close()
    return null
}

internal fun PlayApk.verified(input: InputStream, onBytes: (Int) -> Unit): InputStream {
    val expected = sha256.ifBlank { sha1 }
    if (expected.isBlank()) {
        input.close()
        throw IOException("Google Play sent no checksum for $name.")
    }
    return VerifiedStream(input, MessageDigest.getInstance(if (sha256.isBlank()) "SHA-1" else "SHA-256"), expected, name, onBytes)
}

internal fun measurement(compressed: List<PlayApk>, files: Int, deltas: List<Delta>, size: (Long) -> String): String =
    (
        listOf("gzip ${size(compressed.sumOf { it.compressedSize })} for ${compressed.size} of $files files") +
            deltas.map { delta ->
                "delta format ${delta.format} " + delta.patches.fold(
                    onSuccess = { patches ->
                        if (patches.isEmpty()) "none" else "${size(patches.sumOf { it.maxPatchSize })} for ${patches.size}"
                    },
                    onFailure = { "failed ($it)" }
                )
            }
        ).joinToString(" · ", prefix = "Google Play: ")

private fun DeliveryResponse.delivered(): AndroidAppDeliveryData = when (status) {
    DELIVERED -> appDeliveryData
    NOT_PURCHASED -> throw GooglePlayException.AppNotPurchased(status)
    REMOVED -> throw GooglePlayException.AppRemoved(status)
    NOT_SUPPORTED, DEVICE_NOT_SUPPORTED -> throw GooglePlayException.AppNotSupported(status)
    else -> throw GooglePlayException.Unknown(status)
}

private fun AndroidAppDeliveryData.patches(): List<AndroidAppPatchData> =
    listOfNotNull(patchData.takeIf { hasPatchData() }) +
        splitDeliveryDataList.mapNotNull { split -> split.patchData.takeIf { split.hasPatchData() } }

private fun String.decodedHash(): String = Base64.decode(this, Base64.URL_SAFE).toHexString()

private const val DELIVERED = 1
private const val NOT_SUPPORTED = 2
private const val NOT_PURCHASED = 3
private const val REMOVED = 7
private const val DEVICE_NOT_SUPPORTED = 9
private const val GZIP_MAGIC_BYTES = 2

private class VerifiedStream(
    input: InputStream,
    private val digest: MessageDigest,
    private val expected: String,
    private val name: String,
    private val onBytes: (Int) -> Unit
) : FilterInputStream(input) {
    private var verified = false

    override fun read(): Int = super.read().also { byte ->
        if (byte == -1) {
            verify()
        } else {
            digest.update(byte.toByte())
            onBytes(1)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = super.read(buffer, offset, length).also { count ->
        if (count == -1) {
            verify()
        } else {
            digest.update(buffer, offset, count)
            onBytes(count)
        }
    }

    private fun verify() {
        if (verified) return
        verified = true
        if (!digest.digest().toHexString().equals(expected, ignoreCase = true)) {
            throw IOException("Google Play sent a damaged copy of $name.")
        }
    }
}
