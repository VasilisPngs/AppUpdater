package com.android.appupdater.data

private val PreReleaseMarker =
    Regex("(?:alpha|beta|(?<![a-z])(?:canary|dev|preview))(?![a-z])", RegexOption.IGNORE_CASE)

internal fun isStableRelease(value: String): Boolean =
    value.isBlank() || !PreReleaseMarker.containsMatchIn(value)
