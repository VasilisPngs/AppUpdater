package com.android.appupdater.data.installer

import android.content.Context
import android.content.pm.PackageManager
import com.android.appupdater.data.api.SharedHttpClient
import com.android.appupdater.data.isStableRelease
import com.android.appupdater.data.play.PlayCatalog
import com.android.appupdater.data.play.PlayHttpClient
import com.android.appupdater.data.play.playCertificateHash
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.gplayapi.exceptions.GooglePlayException
import com.aurora.gplayapi.helpers.PurchaseHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

class PlayInstaller(
    private val context: Context,
    private val catalog: PlayCatalog,
    private val httpClient: PlayHttpClient
) {
    private val packageInstaller = PackageInstallerManager(context)
    private val packageManager = context.packageManager

    suspend fun install(
        packageName: String,
        versionCode: Long,
        onProgress: (Float?) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "play_$packageName")
        try {
            require(versionCode > installedVersionCode(packageName)) {
                "The version code must be newer than the installed version."
            }
            onProgress(null)
            directory.deleteRecursively()

            val (session, details) = catalog.details(packageName)
            val purchases = PurchaseHelper(session).using(httpClient)
            val certificate = packageManager.playCertificateHash(packageName)

            val appFiles = deliver(purchases, packageName, versionCode, details.offerType, certificate)
            val libraryFiles = details.dependencies.dependentLibraries
                .map { library ->
                    library.packageName to if (versionCode == details.versionCode) library.versionCode else versionCode
                }
                .filter { (name, code) -> name.isNotBlank() && code > 0 && !isSharedLibraryInstalled(name, code) }
                .map { (name, code) -> name to deliver(purchases, name, code, LIBRARY_OFFER_TYPE, certificate) }

            val progress = Progress(
                (appFiles + libraryFiles.flatMap { it.second }).sumOf { it.size }.coerceAtLeast(1),
                onProgress
            )
            val libraries = libraryFiles.map { (name, files) -> download(files, File(directory, name), progress) }
            val app = download(appFiles, File(directory, packageName), progress)
            verify(app, packageName, versionCode)

            onProgress(null)
            libraries.forEach { packageInstaller.install(it.map(::source)).getOrThrow() }
            packageInstaller.install(app.map(::source)).getOrThrow()
            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        } finally {
            directory.deleteRecursively()
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
            throw IllegalStateException("Google Play does not offer $packageName $versionCode.", exception)
        }
        return files.filter { it.type == PlayFile.Type.BASE || it.type == PlayFile.Type.SPLIT }
            .ifEmpty { throw IllegalStateException("Google Play returned no installable file.") }
    }

    private fun download(files: List<PlayFile>, directory: File, progress: Progress): List<Pair<PlayFile, File>> {
        directory.mkdirs()
        return files.mapIndexed { index, playFile ->
            val target = File(directory, playFile.name.ifBlank { "split_$index.apk" })
            val request = Request.Builder().url(playFile.url).build()
            SharedHttpClient.instance.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Google Play download failed (${response.code})." }
                response.body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(COPY_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            progress.advance(read)
                        }
                    }
                }
            }
            playFile to target
        }
    }

    private fun verify(files: List<Pair<PlayFile, File>>, packageName: String, versionCode: Long) {
        val base = files.firstOrNull { it.first.type == PlayFile.Type.BASE }?.second
            ?: throw IllegalStateException("Google Play returned no base APK.")
        val archive = packageManager.getPackageArchiveInfo(base.path, PackageManager.PackageInfoFlags.of(0))
            ?: throw IllegalStateException("Google Play returned an unreadable APK.")
        check(archive.packageName == packageName && archive.longVersionCode == versionCode) {
            "Google Play returned a different package or version."
        }
        val versionName = archive.versionName.orEmpty()
        check(isStableRelease(versionName)) {
            "$versionName is a pre-release. Only stable versions are installed."
        }
    }

    private fun source(file: Pair<PlayFile, File>): ApkSource =
        ApkSource(file.second.name, file.second.length()) { file.second.inputStream() }

    private fun installedVersionCode(packageName: String): Long =
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode

    private fun isSharedLibraryInstalled(packageName: String, versionCode: Long): Boolean =
        packageManager.getSharedLibraries(PackageManager.PackageInfoFlags.of(0))
            .any { it.name == packageName && it.longVersion == versionCode }

    private class Progress(private val totalBytes: Long, private val onProgress: (Float?) -> Unit) {
        private var writtenBytes = 0L
        private var reportedPercent = -1

        fun advance(bytes: Int) {
            writtenBytes += bytes
            val percent = (writtenBytes * 100 / totalBytes).toInt()
            if (percent != reportedPercent) {
                reportedPercent = percent
                onProgress(percent / 100f)
            }
        }
    }

    private companion object {
        const val LIBRARY_OFFER_TYPE = 0
    }
}
