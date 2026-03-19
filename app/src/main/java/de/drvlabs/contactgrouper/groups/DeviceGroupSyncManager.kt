package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
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
    private val repository: GroupsRepository
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
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                scheduleSync()
            }
        }
        observer = contentObserver
        contentResolver.registerContentObserver(
            ContactsContract.Groups.CONTENT_URI,
            true,
            contentObserver
        )
        contentResolver.registerContentObserver(
            ContactsContract.Data.CONTENT_URI,
            true,
            contentObserver
        )
        scheduleSync(immediate = true)
    }

    fun stop() {
        if (!started) {
            return
        }
        started = false
        syncJob?.cancel()
        syncJob = null
        observer?.let(contentResolver::unregisterContentObserver)
        observer = null
        scope?.cancel()
        scope = null
    }

    suspend fun syncNow(): GroupMutationResult {
        syncJob?.cancel()
        return withContext(Dispatchers.IO) {
            performSync()
        }
    }

    private fun scheduleSync(immediate: Boolean = false) {
        val activeScope = scope ?: return
        syncJob?.cancel()
        syncJob = activeScope.launch {
            if (!immediate) {
                delay(400)
            }
            performSync()
        }
    }

    private suspend fun performSync(): GroupMutationResult {
        return try {
            syncMutex.withLock {
                repository.syncDeviceGroups(source.loadSnapshot())
            }
        } catch (throwable: Throwable) {
            when (throwable) {
                is CancellationException -> throw throwable
                is SecurityException -> GroupMutationResult.PermissionDenied
                is Exception -> {
                    GroupSyncDiagnostics.reportFailure(
                        operation = "performSync",
                        throwable = throwable
                    )
                    GroupMutationResult.ProviderWriteFailed(
                        GroupMutationAction.SYNC_DEVICE_GROUPS
                    )
                }

                else -> throw throwable
            }
        }
    }
}
