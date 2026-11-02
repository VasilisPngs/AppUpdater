package com.vasilispngs.appupdater.data.play

import com.aurora.gplayapi.PlayClient
import com.aurora.gplayapi.PlayException
import com.aurora.gplayapi.model.App
import com.aurora.gplayapi.model.AppFile
import com.aurora.gplayapi.model.Restriction

class PlayCatalog(playClient: Lazy<PlayClient>) {
    private val client by playClient

    suspend fun lookup(packageNames: List<String>): List<App> =
        client.apps.details(packageNames).filter { it.displayName.isNotEmpty() && it.restriction in OFFERED }

    suspend fun details(packageName: String): App = client.apps.details(packageName)

    suspend fun files(packageName: String, versionCode: Long, offerType: Int, certificateHash: String?): List<AppFile> =
        client.delivery.resolve(
            packageName = packageName,
            versionCode = versionCode,
            offerType = offerType,
            certificateHash = certificateHash
        )

    suspend fun delivers(packageName: String, versionCode: Long, offerType: Int, certificateHash: String?): Boolean = try {
        files(packageName, versionCode, offerType, certificateHash).any { it.type == AppFile.Type.BASE }
    } catch (exception: PlayException) {
        if (exception.isUnavailable) false else throw exception
    }

    private companion object {
        val OFFERED = setOf(Restriction.AVAILABLE, Restriction.UNAVAILABLE_UNLESS_PURCHASED)
    }
}
