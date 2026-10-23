package com.android.appupdater.data.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.icu.util.ULocale
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface Preapproval {
    data class Approved(val sessionId: Int) : Preapproval
    data object Declined : Preapproval
    data object Unavailable : Preapproval
}

class PackageInstallerManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val sessionLock = Mutex()
    private val pendingConfirmation = MutableStateFlow<Intent?>(null)

    val confirmation: StateFlow<Intent?> = pendingConfirmation.asStateFlow()

    fun confirmationShown(intent: Intent) {
        pendingConfirmation.compareAndSet(intent, null)
    }

    suspend fun preapprove(packageName: String): Preapproval = sessionLock.withLock {
        withContext(ioDispatcher) { request(packageName) }
    }

    suspend fun install(sources: List<ApkSource>, preapproved: Int? = null): Result<String> = sessionLock.withLock {
        withContext(ioDispatcher) { commit(sources, preapproved) }
    }

    fun abandon(sessionId: Int) {
        val _ = runCatching { context.packageManager.packageInstaller.abandonSession(sessionId) }
    }

    private suspend fun request(packageName: String): Preapproval {
        val details = try {
            preapprovalDetails(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return Preapproval.Unavailable
        }
        val installer = context.packageManager.packageInstaller
        val sessionId = try {
            installer.createSession(sessionParams().apply { setAppPackageName(packageName) })
        } catch (_: Exception) {
            return Preapproval.Unavailable
        }

        val result = CompletableDeferred<Preapproval>()
        val action = "${context.packageName}.PACKAGE_PREAPPROVAL_$sessionId"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION ->
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                            ?.let { pendingConfirmation.value = it }
                    else -> result.complete(preapproval(intent, sessionId))
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
        return try {
            installer.openSession(sessionId).use { it.requestUserPreapproval(details, statusReceiver(action, sessionId)) }
            result.await().also { if (it == Preapproval.Unavailable) abandon(sessionId) }
        } catch (exception: CancellationException) {
            abandon(sessionId)
            throw exception
        } catch (_: Exception) {
            abandon(sessionId)
            Preapproval.Unavailable
        } finally {
            pendingConfirmation.value = null
            val _ = runCatching { context.unregisterReceiver(receiver) }
        }
    }

    private suspend fun commit(sources: List<ApkSource>, preapproved: Int?): Result<String> {
        if (sources.isEmpty()) {
            preapproved?.let(::abandon)
            return Result.failure(IllegalArgumentException("No APK files to install."))
        }

        val installer = context.packageManager.packageInstaller
        val sessionId = preapproved ?: try {
            installer.createSession(sessionParams().apply { setSize(sources.sumOf { it.size.coerceAtLeast(0) }) })
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
        val session = try {
            installer.openSession(sessionId)
        } catch (exception: Exception) {
            abandon(sessionId)
            return Result.failure(exception)
        }

        val result = CompletableDeferred<Result<String>>()
        val action = "${context.packageName}.PACKAGE_INSTALL_$sessionId"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION ->
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                            ?.let { pendingConfirmation.value = it }
                    else -> result.complete(installResult(intent))
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
        return try {
            sources.forEach { source ->
                source.openStream().use { input ->
                    session.openWrite(source.name, 0, source.size).use { output ->
                        input.copyTo(output, COPY_BUFFER_SIZE)
                        session.fsync(output)
                    }
                }
            }

            session.commit(statusReceiver(action, sessionId))
            session.close()
            result.await()
        } catch (exception: CancellationException) {
            val _ = runCatching { session.abandon() }
            throw exception
        } catch (exception: Exception) {
            val _ = runCatching { session.abandon() }
            Result.failure(exception)
        } finally {
            pendingConfirmation.value = null
            val _ = runCatching { context.unregisterReceiver(receiver) }
        }
    }

    internal fun preapprovalDetails(packageName: String): PackageInstaller.PreapprovalDetails {
        val packageManager = context.packageManager
        val info = packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        val iconSize = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        return PackageInstaller.PreapprovalDetails.Builder()
            .setPackageName(packageName)
            .setLabel(packageManager.getApplicationLabel(info))
            .setLocale(ULocale.forLocale(context.resources.configuration.locales[0]))
            .setIcon(packageManager.getApplicationIcon(info).toBitmap(iconSize, iconSize))
            .build()
    }

    internal fun preapproval(status: Intent, sessionId: Int): Preapproval =
        when (status.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_SUCCESS -> Preapproval.Approved(sessionId)
            PackageInstaller.STATUS_FAILURE_ABORTED -> Preapproval.Declined
            else -> Preapproval.Unavailable
        }

    internal fun installResult(status: Intent): Result<String> =
        if (status.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE) == PackageInstaller.STATUS_SUCCESS) {
            Result.success(status.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME).orEmpty())
        } else {
            Result.failure(IllegalStateException(failureMessage(status.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))))
        }

    private fun sessionParams(): PackageInstaller.SessionParams =
        PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }

    private fun statusReceiver(action: String, sessionId: Int): IntentSender =
        PendingIntent.getBroadcast(
            context,
            sessionId,
            Intent(action).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        ).intentSender

    private fun failureMessage(message: String?): String {
        val library = message?.let { MISSING_LIBRARY_PATTERN.find(it)?.groupValues?.get(1) }
        return when {
            library != null -> "Requires the shared library $library, install it first."
            !message.isNullOrBlank() -> message
            else -> INSTALLATION_FAILED
        }
    }

    private companion object {
        val MISSING_LIBRARY_PATTERN = Regex("shared library ([^\\s;]+)")
    }
}

internal const val INSTALLATION_FAILED = "Installation failed"
