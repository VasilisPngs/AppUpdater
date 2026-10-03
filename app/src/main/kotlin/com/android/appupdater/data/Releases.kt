package com.android.appupdater.data

private val PreReleaseMarker =
    Regex("(?:^|[^a-z])(alpha|beta|rc|canary|dev|preview)(?:[^a-z]|$)", RegexOption.IGNORE_CASE)

internal fun isStableRelease(value: String): Boolean =
    value.isBlank() || !PreReleaseMarker.containsMatchIn(value)
