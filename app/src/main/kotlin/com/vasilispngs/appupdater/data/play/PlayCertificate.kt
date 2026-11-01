package com.vasilispngs.appupdater.data.play

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.util.Base64
import java.security.MessageDigest

internal fun PackageManager.playCertificateHash(packageName: String): String? = runCatching {
    val signingInfo = getPackageInfo(
        packageName,
        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
    ).signingInfo ?: return null
    val digest = MessageDigest.getInstance("SHA-1").digest(signingInfo.history().last().toByteArray())
    Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}.getOrNull()

internal fun SigningInfo.history(): Array<Signature> =
    if (hasMultipleSigners()) apkContentsSigners else signingCertificateHistory
