package com.android.appupdater.data.play

import android.content.Context
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthHelper
import org.json.JSONObject
import java.net.UnknownHostException
import java.util.Locale

class PlayAuthProvider(private val context: Context, val httpClient: PlayHttpClient) {

    private var session: AuthData? = null

    @Synchronized
    fun session(): AuthData = session ?: create()

    @Synchronized
    fun renew(stale: AuthData): AuthData = session.takeIf { it !== stale } ?: create()

    private fun create(): AuthData {
        val properties = PlayDeviceProperties.build(context)
        val payload = JSONObject(properties.stringPropertyNames().associateWith(properties::getProperty))
            .toString()
            .toByteArray()

        val response = try {
            httpClient.postAuth(DISPENSER_URL, payload)
        } catch (exception: UnknownHostException) {
            throw IllegalStateException(
                "This device could not resolve $DISPENSER_HOST. A VPN, private DNS or content blocker is filtering it.",
                exception
            )
        }
        if (!response.isSuccessful) throw IllegalStateException(dispenserError(response.code))

        val credentials = JSONObject(String(response.responseBytes))
        val email = credentials.optString("email")
        val token = credentials.optString("authToken")
        check(email.isNotEmpty() && token.isNotEmpty()) { "The account dispenser returned no credentials." }

        return AuthHelper.using(httpClient).build(
            email = email,
            token = token,
            tokenType = AuthHelper.Token.AUTH,
            isAnonymous = true,
            properties = properties,
            locale = Locale.getDefault()
        ).also { session = it }
    }

    private fun dispenserError(code: Int): String = when (code) {
        400 -> "The account dispenser rejected the device configuration."
        403 -> "The account dispenser refused this network."
        429 -> "The account dispenser is rate limiting this network. Retry in ten minutes."
        in 500..599 -> "The account dispenser is unavailable ($code)."
        else -> "The account dispenser failed ($code)."
    }

    private companion object {
        const val DISPENSER_HOST = "auroraoss.com"
        const val DISPENSER_URL = "https://auroraoss.com/api/auth/"
    }
}
