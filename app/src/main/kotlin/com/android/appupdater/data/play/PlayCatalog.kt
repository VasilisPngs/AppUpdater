package com.android.appupdater.data.play

import com.aurora.gplayapi.data.models.App
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.exceptions.GooglePlayException
import com.aurora.gplayapi.helpers.AppDetailsHelper

class PlayCatalog(private val authProvider: PlayAuthProvider) {

    fun lookup(packageNames: List<String>): List<App> {
        if (packageNames.isEmpty()) return emptyList()
        return withSession { auth -> detailsHelper(auth).getAppByPackageName(packageNames) }
            .second
            .filter { it.displayName.isNotEmpty() }
    }

    fun details(packageName: String): Pair<AuthData, App> =
        withSession { auth -> detailsHelper(auth).getAppByPackageName(packageName) }

    private fun detailsHelper(auth: AuthData): AppDetailsHelper =
        AppDetailsHelper(auth).using(authProvider.httpClient)

    private fun <T> withSession(block: (AuthData) -> T): Pair<AuthData, T> {
        val session = authProvider.session()
        return try {
            session to block(session)
        } catch (_: GooglePlayException.AuthException) {
            val renewed = authProvider.renew(session)
            renewed to block(renewed)
        }
    }
}
