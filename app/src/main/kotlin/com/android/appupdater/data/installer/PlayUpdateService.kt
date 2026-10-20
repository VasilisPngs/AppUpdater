package com.android.appupdater.data.installer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import com.android.appupdater.AppUpdaterApplication
import com.android.appupdater.MainActivity
import com.android.appupdater.R
import com.android.appupdater.data.model.InstallEvent
import com.android.appupdater.data.model.PlayUpdate
import com.android.appupdater.ui.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlayUpdateService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val running = HashMap<Int, Job>()

    override fun onStartJob(params: JobParameters): Boolean {
        val update = params.extras.toPlayUpdate()
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.updates), NotificationManager.IMPORTANCE_LOW)
        )
        setNotification(params, params.jobId, progress(update, null), JOB_END_NOTIFICATION_POLICY_REMOVE)
        running[params.jobId] = scope.launch {
            val event = (application as AppUpdaterApplication).container.installs.runPlayUpdate(
                update,
                onSize = { updateEstimatedNetworkBytes(params, it, 0) },
                onProgress = { setNotification(params, params.jobId, progress(update, it), JOB_END_NOTIFICATION_POLICY_REMOVE) }
            )
            notifications.notify(update.packageName, RESULT_ID, result(event))
            running.remove(params.jobId)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running.remove(params.jobId)?.cancel()
        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun progress(update: PlayUpdate, progress: Float?): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(update.appName)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setProgress(PERCENT, ((progress ?: 0f) * PERCENT).toInt(), progress == null)
            .setContentIntent(openApp())
            .setOngoing(true)
            .build()

    private fun result(event: InstallEvent): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(event.text(resources))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()

    private fun openApp(): PendingIntent =
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    companion object {
        private const val CHANNEL_ID = "updates"
        private const val RESULT_ID = 1
        private const val PERCENT = 100
        private const val PACKAGE_NAME = "package_name"
        private const val APP_NAME = "app_name"
        private const val VERSION_NAME = "version_name"
        private const val VERSION_CODE = "version_code"
        private const val LATEST = "latest"

        internal fun schedule(context: Context, update: PlayUpdate): Boolean =
            context.getSystemService(JobScheduler::class.java).schedule(
                JobInfo.Builder(update.packageName.hashCode(), ComponentName(context, PlayUpdateService::class.java))
                    .setUserInitiated(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(update.toExtras())
                    .build()
            ) == JobScheduler.RESULT_SUCCESS

        internal fun PlayUpdate.toExtras(): PersistableBundle = PersistableBundle().apply {
            putString(PACKAGE_NAME, packageName)
            putString(APP_NAME, appName)
            putString(VERSION_NAME, versionName)
            putLong(VERSION_CODE, versionCode)
            putBoolean(LATEST, latest)
        }

        internal fun PersistableBundle.toPlayUpdate(): PlayUpdate = PlayUpdate(
            packageName = getString(PACKAGE_NAME).orEmpty(),
            appName = getString(APP_NAME).orEmpty(),
            versionName = getString(VERSION_NAME).orEmpty(),
            versionCode = getLong(VERSION_CODE),
            latest = getBoolean(LATEST)
        )
    }
}
