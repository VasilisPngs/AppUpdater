package com.vasilispngs.appupdater.data.play

import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.device.DeviceProfile
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.AppFile
import com.aurora.gplayapi.model.Restriction
import com.aurora.gplayapi.net.HttpEngine
import com.aurora.gplayapi.session.DefaultPlaySessions
import java.util.Locale

class PlayCatalog(engine: Lazy<HttpEngine>, device: Lazy<DeviceProfile>) {
    private val sessions by lazy { DefaultPlaySessions(engine.value, device = device::value, locale = { Locale.getDefault() }) }
    private val client by lazy { PlayClient(engine.value, sessions) }

    suspend fun lookup(packageNames: List<String>): List<App> =
        request { apps.details(packageNames) }.filter { it.displayName.isNotEmpty() && it.restriction in OFFERED }

    suspend fun details(packageName: String): App = request { apps.details(packageName) }

    suspend fun files(packageName: String, versionCode: Long, offerType: Int, certificateHash: String?): List<AppFile> =
        request {
            delivery.resolve(
                packageName = packageName,
                versionCode = versionCode,
                offerType = offerType,
                certificateHash = certificateHash
            )
        }

    suspend fun delivers(packageName: String, versionCode: Long, offerType: Int, certificateHash: String?): Boolean = try {
        files(packageName, versionCode, offerType, certificateHash).any { it.type == AppFile.Type.BASE }
    } catch (exception: PlayException) {
        if (exception.isUnavailable) false else throw exception
    }

    private suspend fun <T> request(call: suspend PlayClient.() -> T): T = try {
        client.call()
    } catch (exception: PlayException.Unauthorized) {
        sessions.invalidateAnonymous()
        throw exception
    }

    private companion object {
        val OFFERED = setOf(Restriction.AVAILABLE, Restriction.UNAVAILABLE_UNLESS_PURCHASED)
    }
}
