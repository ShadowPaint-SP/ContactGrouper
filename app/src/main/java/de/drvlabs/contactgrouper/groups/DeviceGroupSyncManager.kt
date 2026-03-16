package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
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
    private val syncMutex = Mutex()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            scheduleSync()
        }
    }

    fun start() {
        if (started) {
            return
        }
        started = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
        scheduleSync(immediate = true)
    }

    fun stop() {
        if (!started) {
            return
        }
        started = false
        syncJob?.cancel()
        syncJob = null
        contentResolver.unregisterContentObserver(observer)
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
        return syncMutex.withLock {
            repository.syncDeviceGroups(source.loadSnapshot())
        }
    }
}
