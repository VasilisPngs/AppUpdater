package com.vasilispngs.appupdater.data.installer

import android.content.Context
import android.content.pm.PackageManager
import com.aurora.gplayapi.model.AppFile
import com.vasilispngs.appupdater.data.api.SharedHttpClient
import com.vasilispngs.appupdater.data.model.ErrorCode
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.FailureException
import com.vasilispngs.appupdater.data.play.PlayCatalog
import com.vasilispngs.appupdater.data.play.playCertificateHash
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

class PlayInstaller(
    context: Context,
    private val catalog: PlayCatalog,
    private val packageInstaller: PackageInstallerManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val packageManager = context.packageManager

    suspend fun install(
        packageName: String,
        versionCode: Long,
        preapproved: Int?,
        onSize: (Long) -> Unit,
        onProgress: (Float?) -> Unit
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            onProgress(null)
            val details = catalog.details(packageName)
            val certificate = packageManager.playCertificateHash(packageName)
            val app = catalog.files(packageName, versionCode, details.offerType, certificate).apks()
            val libraries = details.dependencies.dependentLibraries
                .mapNotNull { library ->
                    val sameVersion = library.versionCode == details.versionCode
                    val code = if (sameVersion) versionCode else library.versionCode
                    if (library.packageName.isBlank() || code <= 0 || isSharedLibraryInstalled(library.packageName, code)) {
                        null
                    } else {
                        val libraryCertificate = packageManager.playCertificateHash(library.packageName)
                            ?: certificate.takeIf { sameVersion }
                        catalog.files(library.packageName, code, LIBRARY_OFFER_TYPE, libraryCertificate).apks()
                    }
                }

            val apks = app + libraries.flatten()
            onSize(apks.sumOf { if (it.compressedUrl.isEmpty()) it.size else it.compressedSize })
            val progress = Progress(apks.sumOf { it.size }.coerceAtLeast(1), onProgress)
            libraries.forEach { files -> val _ = packageInstaller.install(sources(files, progress)).getOrThrow() }
            val _ = packageInstaller.install(sources(app, progress), preapproved, packageName = packageName).getOrThrow()
            Result.success(Unit)
        } catch (exception: CancellationException) {
            preapproved?.let(packageInstaller::abandon)
            throw exception
        } catch (exception: Exception) {
            preapproved?.let(packageInstaller::abandon)
            Result.failure(exception)
        }
    }

    private fun sources(apks: List<AppFile>, progress: Progress): List<ApkSource> {
        val batch = progress.batch(apks.size)
        return apks.mapIndexed { index, apk ->
            ApkSource(apk.name, apk.size) {
                if (index == 0) batch.reset()
                apk.verified(apk.download()) { batch.advance(index, it) }
            }
        }
    }

    private fun AppFile.download(): InputStream {
        if (compressedUrl.isNotEmpty()) {
            runCatching {
                val response = fetch(compressedUrl, identity = true)
                if (response.isSuccessful) {
                    response.body.byteStream().gunzipped()
                } else {
                    response.close()
                    null
                }
            }.getOrNull()?.let { return it }
        }
        val response = fetch(url)
        if (!response.isSuccessful) {
            response.close()
            throw FailureException(Failure.PlayUnavailable, code = ErrorCode.Http(response.code))
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

    private companion object {
        const val LIBRARY_OFFER_TYPE = 0
    }
}

internal class Progress(private val totalBytes: Long, private val onProgress: (Float?) -> Unit) {
    private val batches = mutableListOf<LongArray>()
    private var reportedPercent = -1

    fun batch(count: Int): Batch {
        val slots = LongArray(count)
        batches += slots
        return Batch(slots)
    }

    private fun publish() {
        val writtenBytes = batches.sumOf(LongArray::sum)
        val percent = (writtenBytes * PERCENT / totalBytes).toInt()
        if (percent != reportedPercent) {
            reportedPercent = percent
            onProgress(if (writtenBytes < totalBytes) percent / PERCENT.toFloat() else null)
        }
    }

    inner class Batch(private val slots: LongArray) {
        fun reset() {
            if (slots.any { it != 0L }) {
                slots.fill(0L)
                publish()
            }
        }

        fun advance(index: Int, bytes: Int) {
            slots[index] += bytes
            publish()
        }
    }

    private companion object {
        const val PERCENT = 100
    }
}

internal fun List<AppFile>.apks(): List<AppFile> {
    if (none { it.type == AppFile.Type.BASE && it.url.isNotEmpty() }) throw FailureException(Failure.NotOffered)
    return filter { it.type == AppFile.Type.BASE || it.type == AppFile.Type.SPLIT }
}

internal fun InputStream.gunzipped(): InputStream? {
    val buffered = BufferedInputStream(this, COPY_BUFFER_SIZE)
    return try {
        buffered.mark(GZIP_MAGIC_BYTES)
        val magic = buffered.read() or (buffered.read() shl Byte.SIZE_BITS)
        buffered.reset()
        if (magic == GZIPInputStream.GZIP_MAGIC) {
            GZIPInputStream(buffered, COPY_BUFFER_SIZE)
        } else {
            buffered.close()
            null
        }
    } catch (exception: Throwable) {
        buffered.close()
        throw exception
    }
}

internal fun AppFile.verified(input: InputStream, onBytes: (Int) -> Unit): InputStream {
    val expected = sha256.ifBlank { sha1 }
    if (expected.isBlank()) {
        input.close()
        throw FailureException(Failure.Unverified)
    }
    return VerifiedStream(input, MessageDigest.getInstance(if (sha256.isBlank()) "SHA-1" else "SHA-256"), expected, onBytes)
}

private const val GZIP_MAGIC_BYTES = 2

private class VerifiedStream(
    input: InputStream,
    digest: MessageDigest,
    private val expected: String,
    private val onBytes: (Int) -> Unit
) : DigestInputStream(input, digest) {
    private var verified = false

    override fun read(): Int = super.read().also { byte ->
        if (byte == -1) {
            verify()
        } else {
            onBytes(1)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = super.read(buffer, offset, length).also { count ->
        if (count == -1) {
            verify()
        } else {
            onBytes(count)
        }
    }

    private fun verify() {
        if (verified) return
        verified = true
        if (!messageDigest.digest().toHexString().equals(expected, ignoreCase = true)) {
            throw FailureException(Failure.Unverified)
        }
    }
}
