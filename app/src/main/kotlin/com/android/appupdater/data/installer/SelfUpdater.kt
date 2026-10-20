package com.android.appupdater.data.installer

import android.content.Context
import com.android.appupdater.data.api.SharedHttpClient
import com.android.appupdater.data.model.SelfUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

@Serializable
internal data class GitHubRelease(val assets: List<GitHubAsset>)

@Serializable
internal data class GitHubAsset(
    val name: String,
    val size: Long,
    val digest: String,
    @SerialName("browser_download_url") val url: String
)

internal const val UPDATED_MARKER = "self-update"

class SelfUpdater(
    private val context: Context,
    private val scope: CoroutineScope,
    private val packageInstaller: PackageInstallerManager,
    private val foreground: Flow<Boolean>
) {
    private val state = MutableStateFlow<SelfUpdate>(SelfUpdate.Checking)
    private val apk = File(context.cacheDir, APK_NAME)
    private val json = Json { ignoreUnknownKeys = true }
    private var release: GitHubAsset? = null
    private var work: Job? = null

    val status: StateFlow<SelfUpdate> = state.asStateFlow()

    fun check(automatic: Boolean) {
        if (work?.isActive == true) return
        work = scope.launch {
            state.value = SelfUpdate.Checking
            val asset = try {
                newerRelease()
            } catch (_: IOException) {
                state.value = SelfUpdate.CheckFailed
                return@launch
            } catch (_: SerializationException) {
                state.value = SelfUpdate.CheckFailed
                return@launch
            }
            release = asset
            state.value = if (asset == null) SelfUpdate.UpToDate else SelfUpdate.Available
            if (asset == null || !automatic || !context.packageManager.canRequestPackageInstalls()) {
                return@launch
            }
            if (download(asset)) {
                state.value = SelfUpdate.Ready
                foreground.first { !it }
                install()
            }
        }
    }

    fun update() {
        val asset = release ?: return
        val current = state.value
        if (current != SelfUpdate.Available && current != SelfUpdate.Ready && current != SelfUpdate.Failed) return
        work?.cancel()
        work = scope.launch {
            if (current == SelfUpdate.Ready || download(asset)) install()
        }
    }

    private suspend fun newerRelease(): GitHubAsset? = withContext(Dispatchers.IO) {
        apk.delete()
        val release = fetch(RELEASE_URL) { json.decodeFromString<GitHubRelease>(it.body.string()) }
        val asset = release.assets.firstOrNull { it.name == APK_NAME } ?: throw IOException("No $APK_NAME in the release")
        val installed = File(context.applicationInfo.sourceDir).inputStream().use(::sha256)
        asset.takeIf { it.digest != installed }
    }

    private suspend fun download(asset: GitHubAsset): Boolean {
        state.value = SelfUpdate.Downloading(0f)
        val downloaded = try {
            withContext(Dispatchers.IO) {
                fetch(asset.url) { response ->
                    val total = response.body.contentLength().takeIf { it > 0 } ?: asset.size
                    response.body.byteStream().use { input ->
                        apk.outputStream().use { output ->
                            var copied = 0L
                            sha256(input) { buffer, read ->
                                output.write(buffer, 0, read)
                                copied += read
                                state.value = SelfUpdate.Downloading(copied.toFloat() / total)
                            }
                        }
                    }
                } == asset.digest
            }
        } catch (_: IOException) {
            false
        }
        if (!downloaded) state.value = SelfUpdate.Failed
        return downloaded
    }

    private suspend fun install() {
        state.value = SelfUpdate.Installing
        val marker = File(context.noBackupFilesDir, UPDATED_MARKER)
        val result = withContext(Dispatchers.IO) {
            marker.createNewFile()
            packageInstaller.install(listOf(ApkSource(APK_NAME, apk.length()) { apk.inputStream() }))
        }
        if (result.isFailure) {
            marker.delete()
            state.value = SelfUpdate.Failed
        }
        apk.delete()
    }

    private fun <T> fetch(url: String, read: (Response) -> T): T =
        SharedHttpClient.instance.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            read(response)
        }

    private companion object {
        const val APK_NAME = "AppUpdater.apk"
        const val RELEASE_URL = "https://api.github.com/repos/VasilisPngs/AppUpdater/releases/tags/AppUpdater"
    }
}

internal fun sha256(input: InputStream, onRead: (ByteArray, Int) -> Unit = { _, _ -> }): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(COPY_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
        onRead(buffer, read)
    }
    return "sha256:" + digest.digest().toHexString()
}
