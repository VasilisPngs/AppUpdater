package com.vasilispngs.appupdater.data.play

import android.content.Context
import android.os.Build
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthHelper
import com.vasilispngs.appupdater.data.model.Failure
import com.vasilispngs.appupdater.data.model.FailureException
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.File
import java.net.UnknownHostException
import java.util.Locale

class PlayAuthProvider(private val context: Context, val httpClient: PlayHttpClient) {

    private val store = File(context.noBackupFilesDir, SESSION_FILE)
    private var session: AuthData? = null

    @Synchronized
    fun session(): AuthData = session ?: restore() ?: create()

    @Synchronized
    fun renew(stale: AuthData): AuthData = session.takeIf { it !== stale } ?: create()

    private fun restore(): AuthData? = runCatching {
        val saved = JSONObject(store.readText())
        if (saved.getString(KEY_FINGERPRINT) != Build.FINGERPRINT) return null
        Json.decodeFromString(AuthData.serializer(), saved.getString(KEY_SESSION))
    }.getOrNull()?.also { session = it }

    private fun save(auth: AuthData) {
        val _ = runCatching {
            store.writeText(
                JSONObject()
                    .put(KEY_FINGERPRINT, Build.FINGERPRINT)
                    .put(KEY_SESSION, Json.encodeToString(AuthData.serializer(), auth))
                    .toString()
            )
        }
    }

    private fun create(): AuthData {
        val properties = PlayDeviceProperties.build(context)
        val payload = JSONObject(properties.stringPropertyNames().associateWith(properties::getProperty))
            .toString()
            .toByteArray()

        val response = try {
            httpClient.postAuth(DISPENSER_URL, payload)
        } catch (exception: UnknownHostException) {
            throw FailureException(Failure.SignInUnreachable(DISPENSER_HOST), exception)
        }
        if (!response.isSuccessful) throw FailureException(dispenserFailure(response.code))

        val credentials = JSONObject(String(response.responseBytes))
        val email = credentials.optString("email")
        val token = credentials.optString("authToken")
        if (email.isEmpty() || token.isEmpty()) throw FailureException(Failure.SignInFailed)

        return AuthHelper.using(httpClient).build(
            email = email,
            token = token,
            tokenType = AuthHelper.Token.AUTH,
            isAnonymous = true,
            properties = properties,
            locale = Locale.getDefault()
        ).also {
            session = it
            save(it)
        }
    }

    private fun dispenserFailure(code: Int): Failure = when (code) {
        400 -> Failure.SignInRejected
        403 -> Failure.SignInRefused
        429 -> Failure.SignInLimited
        in 500..599 -> Failure.SignInUnavailable
        else -> Failure.SignInFailed
    }

    private companion object {
        const val DISPENSER_HOST = "auroraoss.com"
        const val DISPENSER_URL = "https://auroraoss.com/api/auth/"
        const val SESSION_FILE = "play_session.json"
        const val KEY_FINGERPRINT = "fingerprint"
        const val KEY_SESSION = "session"
    }
}
