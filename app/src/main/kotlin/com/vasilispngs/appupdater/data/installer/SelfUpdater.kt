package com.vasilispngs.appupdater.data.installer

import android.content.Context
import com.vasilispngs.appupdater.data.HttpStatusException
import com.vasilispngs.appupdater.data.api.SharedHttpClient
import com.vasilispngs.appupdater.data.errorCode
import com.vasilispngs.appupdater.data.model.SelfUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

internal const val UPDATED_MARKER = "self-update"

private class Release(val url: String, val size: Long, val digest: String)

class SelfUpdater(
    private val context: Context,
    private val scope: CoroutineScope,
    private val packageInstaller: PackageInstallerManager
) {
    private val state = MutableStateFlow<SelfUpdate>(SelfUpdate.Idle)
    private val apk = File(context.cacheDir, APK_NAME)
    private var release: Release? = null
    private var work: Job? = null

    val status: StateFlow<SelfUpdate> = state.asStateFlow()

    fun check() {
        if (work?.isActive == true) return
        work = scope.launch {
            val newer = try {
                newerRelease()
            } catch (_: IOException) {
                null
            } catch (_: JSONException) {
                null
            }
            release = newer
            state.value = if (newer == null) SelfUpdate.Idle else SelfUpdate.Available
        }
    }

    fun update() {
        val newer = release ?: return
        if (state.value != SelfUpdate.Available && state.value !is SelfUpdate.Failed) return
        work = scope.launch {
            if (download(newer)) install()
        }
    }

    private suspend fun newerRelease(): Release? = withContext(Dispatchers.IO) {
        apk.delete()
        val assets = fetch(RELEASE_URL, GITHUB_HEADERS) { JSONObject(it.body.string()).getJSONArray("assets") }
        val asset = (0 until assets.length()).map(assets::getJSONObject).firstOrNull { it.getString("name") == APK_NAME }
            ?: throw IOException("No $APK_NAME in the release")
        val installed = File(context.applicationInfo.sourceDir).inputStream().use(::sha256)
        Release(asset.getString("browser_download_url"), asset.getLong("size"), asset.getString("digest"))
            .takeIf { it.digest != installed }
    }

    private suspend fun download(release: Release): Boolean {
        state.value = SelfUpdate.Downloading(0f)
        val downloaded = try {
            withContext(Dispatchers.IO) {
                fetch(release.url) { response ->
                    val total = response.body.contentLength().takeIf { it > 0 } ?: release.size
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
                } == release.digest
            }
        } catch (exception: IOException) {
            state.value = SelfUpdate.Failed(exception.errorCode())
            return false
        }
        if (!downloaded) state.value = SelfUpdate.Failed(null)
        return downloaded
    }

    private suspend fun install() {
        state.value = SelfUpdate.Installing
        val marker = File(context.noBackupFilesDir, UPDATED_MARKER)
        val failure = try {
            withContext(Dispatchers.IO) {
                marker.createNewFile()
                packageInstaller.install(listOf(ApkSource(APK_NAME, apk.length()) { apk.inputStream() }))
            }.exceptionOrNull()
        } catch (exception: IOException) {
            exception
        }
        if (failure != null) {
            marker.delete()
            state.value = SelfUpdate.Failed(failure.errorCode())
        }
        apk.delete()
    }

    private fun <T> fetch(url: String, headers: Map<String, String> = emptyMap(), read: (Response) -> T): T {
        val request = Request.Builder().url(url).headers(headers.toHeaders()).build()
        return SharedHttpClient.instance.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw HttpStatusException(response.code)
            read(response)
        }
    }

    private companion object {
        const val APK_NAME = "AppUpdater.apk"
        const val RELEASE_URL = "https://api.github.com/repos/VasilisPngs/AppUpdater/releases/tags/AppUpdater"
        val GITHUB_HEADERS = mapOf("Accept" to "application/vnd.github+json", "X-GitHub-Api-Version" to "2026-03-10")
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
