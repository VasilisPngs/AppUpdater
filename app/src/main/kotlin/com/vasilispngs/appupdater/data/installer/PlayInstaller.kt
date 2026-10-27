package com.vasilispngs.appupdater.data.installer

import android.content.Context
import android.content.pm.PackageManager
import android.text.format.Formatter
import com.aurora.gplayapi.AndroidAppDeliveryData
import com.aurora.gplayapi.data.models.PlayFile
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
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

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

                val app = deliver(purchases, packageName, versionCode, details.offerType, certificate)
                val libraries = details.dependencies.dependentLibraries
                    .map { library ->
                        library.packageName to if (versionCode == details.versionCode) library.versionCode else versionCode
                    }
                    .filter { (name, code) -> name.isNotBlank() && code > 0 && !isSharedLibraryInstalled(name, code) }
                    .map { (name, code) -> deliver(purchases, name, code, LIBRARY_OFFER_TYPE, certificate) }
                Delivery(app, libraries, offers(purchases, packageName, versionCode, details.offerType, certificate))
            }

            val size = (delivery.app + delivery.libraries.flatten()).sumOf { it.size }.coerceAtLeast(1)
            onSize(size)
            val progress = Progress(size, onProgress)
            delivery.libraries.forEach { files -> val _ = packageInstaller.install(sources(files, progress)).getOrThrow() }
            var encoding: String? = null
            val installed = packageInstaller.install(
                sources(delivery.app, progress) { encoding = it },
                preapproved,
                packageName = packageName
            )
            delivery.offers?.let { offers ->
                onMeasured(deliveryOffers(offers, encoding) { Formatter.formatShortFileSize(context, it) })
            }
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
    ): List<PlayFile> {
        val files = try {
            purchases.purchase(
                packageName = packageName,
                versionCode = versionCode,
                offerType = offerType,
                certificateHash = certificate
            )
        } catch (exception: GooglePlayException) {
            throw exception.deliveryFailure(packageName, versionCode)
        }
        return files.filter { it.type == PlayFile.Type.BASE || it.type == PlayFile.Type.SPLIT }
            .ifEmpty { throw IllegalStateException("Google Play returned no installable file.") }
    }

    private fun offers(
        purchases: PurchaseHelper,
        packageName: String,
        versionCode: Long,
        offerType: Int,
        certificate: String?
    ): AndroidAppDeliveryData? = try {
        purchases.getDeliveryResponse(
            packageName = packageName,
            installedVersionCode = packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode,
            updateVersionCode = versionCode,
            offerType = offerType,
            deliveryToken = purchases.getDeliveryToken(packageName, versionCode, offerType, certificate),
            certificateHash = certificate
        ).takeIf { it.hasAppDeliveryData() }?.appDeliveryData
    } catch (_: Exception) {
        null
    }

    private fun sources(files: List<PlayFile>, progress: Progress, onEncoding: (String?) -> Unit = {}): List<ApkSource> =
        files.mapIndexed { index, file ->
            ApkSource(file.name.ifBlank { "split_$index.apk" }, file.size) {
                val response = SharedHttpClient.instance.newCall(Request.Builder().url(file.url).build()).execute()
                if (!response.isSuccessful) {
                    response.close()
                    throw IOException("Google Play download failed (${response.code}).")
                }
                if (file.type == PlayFile.Type.BASE) onEncoding(response.networkResponse?.header("Content-Encoding"))
                file.verified(response.body.byteStream(), progress::advance)
            }
        }

    private fun isSharedLibraryInstalled(packageName: String, versionCode: Long): Boolean =
        packageManager.getSharedLibraries(PackageManager.PackageInfoFlags.of(0))
            .any { it.name == packageName && it.longVersion == versionCode }

    private class Delivery(val app: List<PlayFile>, val libraries: List<List<PlayFile>>, val offers: AndroidAppDeliveryData?)

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

internal fun PlayFile.verified(input: InputStream, onBytes: (Int) -> Unit): InputStream {
    val expected = sha256.ifBlank { sha1 }
    if (expected.isBlank()) {
        input.close()
        throw IOException("Google Play sent no checksum for $name.")
    }
    return VerifiedStream(input, MessageDigest.getInstance(if (sha256.isBlank()) "SHA-1" else "SHA-256"), expected, name, onBytes)
}

internal fun deliveryOffers(data: AndroidAppDeliveryData, encoding: String?, size: (Long) -> String): String {
    val splits = data.splitDeliveryDataList
    val compressed = listOfNotNull(data.compressedSize.takeIf { data.compressedDownloadUrl.isNotEmpty() }) +
        splits.mapNotNull { split -> split.compressedSize.takeIf { split.compressedDownloadUrl.isNotEmpty() } }
    val compressedData = listOfNotNull(data.compressedAppData.takeIf { data.hasCompressedAppData() }) +
        splits.mapNotNull { split -> split.compressedAppData.takeIf { split.hasCompressedAppData() } }
    val patches = listOfNotNull(data.patchData.takeIf { data.hasPatchData() }) +
        splits.mapNotNull { split -> split.patchData.takeIf { split.hasPatchData() } }
    return listOf(
        "full ${size(data.downloadSize + splits.sumOf { it.downloadSize })} in ${splits.size + 1} files",
        if (compressed.isEmpty()) "compressed none" else "compressed ${size(compressed.sum())} for ${compressed.size}",
        if (compressedData.isEmpty()) {
            "compressed data none"
        } else {
            "compressed data ${size(compressedData.sumOf { it.size })} for ${compressedData.size}, type ${compressedData.map { it.type }.distinct().joinToString("/")}"
        },
        if (patches.isEmpty()) {
            "delta none"
        } else {
            "delta ${size(patches.sumOf { it.maxPatchSize })} for ${patches.size}, format ${patches.map { it.patchFormat }.distinct().joinToString("/")}"
        },
        "download ${encoding ?: "identity"}"
    ).joinToString(" · ", prefix = "Google Play offers: ")
}

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
