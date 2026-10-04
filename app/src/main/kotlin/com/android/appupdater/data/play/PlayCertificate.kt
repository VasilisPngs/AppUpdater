package com.android.appupdater.data.play

import android.content.pm.PackageManager
import android.util.Base64
import java.security.MessageDigest

internal fun PackageManager.playCertificateHash(packageName: String): String? = runCatching {
    val signingInfo = getPackageInfo(
        packageName,
        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
    ).signingInfo ?: return null
    val certificates = if (signingInfo.hasMultipleSigners()) {
        signingInfo.apkContentsSigners
    } else {
        signingInfo.signingCertificateHistory
    }
    val digest = MessageDigest.getInstance("SHA-1").digest(certificates.last().toByteArray())
    Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}.getOrNull()
