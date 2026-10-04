package com.android.appupdater.data.installer

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object InstallConfirmation {
    private val pending = MutableStateFlow<Intent?>(null)

    val intent: StateFlow<Intent?> = pending.asStateFlow()

    internal fun request(intent: Intent) {
        pending.value = intent
    }

    internal fun clear() {
        pending.value = null
    }

    fun consume(intent: Intent): Boolean = pending.compareAndSet(intent, null)
}
