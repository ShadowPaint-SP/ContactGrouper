package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import de.drvlabs.contactgrouper.AppError
import de.drvlabs.contactgrouper.AppErrorKind
import de.drvlabs.contactgrouper.AppErrorOrigin
import de.drvlabs.contactgrouper.AppErrorReporter
import de.drvlabs.contactgrouper.R
import de.drvlabs.contactgrouper.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DeviceGroupSyncManager(
    private val contentResolver: ContentResolver,
    private val source: DeviceGroupSource,
    private val repository: GroupsRepository,
    private val settingsRepository: AppSettingsRepository,
    private val appErrorReporter: AppErrorReporter = AppErrorReporter(),
    private val getString: (Int) -> String = { "" },
    private val contentObserverFactory: (() -> Unit) -> ContentObserver = { onChange ->
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                onChange()
            }
        }
    },
    private val registerObservers: (ContentObserver) -> Unit = { observer ->
        contentResolver.registerContentObserver(
            ContactsContract.Groups.CONTENT_URI,
            true,
            observer
        )
        contentResolver.registerContentObserver(
            ContactsContract.Data.CONTENT_URI,
            true,
            observer
        )
    },
    private val unregisterObserver: (ContentObserver) -> Unit = { observer ->
        contentResolver.unregisterContentObserver(observer)
    }
) {
    private var scope: CoroutineScope? = null
    private var syncJob: Job? = null
    private var started = false
    private var observer: ContentObserver? = null
    private val syncMutex = Mutex()

    fun start() {
        if (started) {
            return
        }
        started = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val contentObserver = contentObserverFactory {
            scheduleSync()
        }
        observer = contentObserver
        try {
            registerObservers(contentObserver)
            scheduleSync(immediate = true, errorKind = AppErrorKind.StartupFatal)
        } catch (throwable: Throwable) {
            reportSyncFailure(
                throwable = throwable,
                errorKind = AppErrorKind.StartupFatal,
                operation = "registerObservers"
            )
        }
    }

    fun stop() {
        if (!started) {
            return
        }
        started = false
        syncJob?.cancel()
        syncJob = null
        observer?.let {
            runCatching {
                unregisterObserver(it)
            }
        }
        observer = null
        scope?.cancel()
        scope = null
    }

    suspend fun syncNow(): GroupMutationResult {
        syncJob?.cancel()
        return withContext(Dispatchers.IO) {
            performSync(
                errorKind = AppErrorKind.RuntimeUnexpected,
                operation = "manualSync"
            )
        }
    }

    private fun scheduleSync(
        immediate: Boolean = false,
        errorKind: AppErrorKind = AppErrorKind.RuntimeUnexpected
    ) {
        val activeScope = scope ?: return
        syncJob?.cancel()
        syncJob = activeScope.launch {
            if (!immediate) {
                delay(400)
            }
            performSync(
                errorKind = errorKind,
                operation = if (immediate) "startupSync" else "observerSync"
            )
        }
    }

    private suspend fun performSync(
        errorKind: AppErrorKind,
        operation: String
    ): GroupMutationResult {
        return try {
            syncMutex.withLock {
                repository.syncDeviceGroups(
                    snapshot = source.loadSnapshot(),
                    ringtoneMode = if (
                        settingsRepository.settings.value.autoSyncDeviceGroupChanges
                    ) {
                        DeviceSyncRingtoneMode.ApplyImmediately
                    } else {
                        DeviceSyncRingtoneMode.RequireConfirmation
                    }
                )
            }
        } catch (throwable: Throwable) {
            when (throwable) {
                is CancellationException -> throw throwable
                is SecurityException -> {
                    reportSyncFailure(throwable, errorKind, operation)
                    GroupMutationResult.PermissionDenied
                }
                is Exception -> {
                    GroupSyncDiagnostics.reportFailure(
                        operation = operation,
                        throwable = throwable
                    )
                    reportSyncFailure(throwable, errorKind, operation)
                    GroupMutationResult.ProviderWriteFailed(
                        GroupMutationAction.SYNC_DEVICE_GROUPS
                    )
                }

                else -> throw throwable
            }
        }
    }

    private fun reportSyncFailure(
        throwable: Throwable,
        errorKind: AppErrorKind,
        operation: String
    ) {
        val error = when (errorKind) {
            AppErrorKind.StartupFatal -> AppError.startupFatal(
                origin = AppErrorOrigin.DeviceGroupSync,
                title = getString(R.string.app_error_start_failed_title),
                userMessage = getString(R.string.app_error_groups_startup_message),
                throwable = throwable,
                heading = getString(R.string.app_error_groups_startup_heading),
                context = mapOf("operation" to operation)
            )

            AppErrorKind.RuntimeUnexpected -> AppError.runtimeUnexpected(
                origin = AppErrorOrigin.DeviceGroupSync,
                title = getString(R.string.app_error_group_sync_failed_title),
                userMessage = getString(R.string.app_error_groups_refresh_message),
                throwable = throwable,
                heading = getString(R.string.app_error_groups_refresh_heading),
                context = mapOf("operation" to operation)
            )
        }

        appErrorReporter.report(error)
    }
}
