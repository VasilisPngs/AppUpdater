package com.vasilispngs.appupdater.data.installer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vasilispngs.appupdater.MainActivity
import com.vasilispngs.appupdater.R
import java.io.File

class UpdatedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED || !File(context.noBackupFilesDir, UPDATED_MARKER).delete()) return
        context.createUpdatesChannel()
        context.getSystemService(NotificationManager::class.java).notify(
            UPDATED_ID,
            Notification.Builder(context, UPDATES_CHANNEL)
                .setSmallIcon(R.drawable.ic_updates)
                .setContentTitle(context.getString(R.string.app_updated))
                .setContentIntent(context.openApp())
                .setAutoCancel(true)
                .build()
        )
    }

    private companion object {
        const val UPDATED_ID = 2
    }
}

internal fun Context.createUpdatesChannel() {
    getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(UPDATES_CHANNEL, getString(R.string.updates), NotificationManager.IMPORTANCE_LOW)
    )
}

internal fun Context.openApp(): PendingIntent =
    PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

internal const val UPDATES_CHANNEL = "updates"
