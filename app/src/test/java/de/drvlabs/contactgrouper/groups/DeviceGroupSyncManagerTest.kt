package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
import android.database.ContentObserver
import de.drvlabs.contactgrouper.AppErrorKind
import de.drvlabs.contactgrouper.AppErrorOrigin
import de.drvlabs.contactgrouper.AppErrorReporter
import de.drvlabs.contactgrouper.settings.AppSettings
import de.drvlabs.contactgrouper.settings.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceGroupSyncManagerTest {

    @Test
    fun `syncNow returns provider failure when source throws runtime exception`() = runBlocking {
        val syncManager = DeviceGroupSyncManager(
            contentResolver = noOpContentResolver(),
            source = object : DeviceGroupSource {
                override suspend fun loadSnapshot(): DeviceGroupSnapshot {
                    throw IllegalStateException("boom")
                }
            },
            repository = FakeGroupsRepository(),
            settingsRepository = FakeSettingsRepository()
        )

        assertEquals(
            GroupMutationResult.ProviderWriteFailed(GroupMutationAction.SYNC_DEVICE_GROUPS),
            syncManager.syncNow()
        )
    }

    @Test
    fun `syncNow returns permission denied when source throws security exception`() = runBlocking {
        val syncManager = DeviceGroupSyncManager(
            contentResolver = noOpContentResolver(),
            source = object : DeviceGroupSource {
                override suspend fun loadSnapshot(): DeviceGroupSnapshot {
                    throw SecurityException("denied")
                }
            },
            repository = FakeGroupsRepository(),
            settingsRepository = FakeSettingsRepository()
        )

        assertEquals(GroupMutationResult.PermissionDenied, syncManager.syncNow())
    }

    @Test
    fun `syncNow returns provider failure when repository throws runtime exception`() = runBlocking {
        val syncManager = DeviceGroupSyncManager(
            contentResolver = noOpContentResolver(),
            source = object : DeviceGroupSource {
                override suspend fun loadSnapshot(): DeviceGroupSnapshot {
                    return DeviceGroupSnapshot(emptyList(), emptyList())
                }
            },
            repository = FakeGroupsRepository { _, _ ->
                throw IllegalArgumentException("broken repository")
            },
            settingsRepository = FakeSettingsRepository()
        )

        assertEquals(
            GroupMutationResult.ProviderWriteFailed(GroupMutationAction.SYNC_DEVICE_GROUPS),
            syncManager.syncNow()
        )
    }

    @Test
    fun `start reports startup fatal error when immediate sync throws`() {
        val reporter = AppErrorReporter()
        val syncManager = DeviceGroupSyncManager(
            contentResolver = noOpContentResolver(),
            source = object : DeviceGroupSource {
                override suspend fun loadSnapshot(): DeviceGroupSnapshot {
                    throw IllegalStateException("startup sync exploded")
                }
            },
            repository = FakeGroupsRepository(),
            settingsRepository = FakeSettingsRepository(),
            appErrorReporter = reporter,
            contentObserverFactory = { onChange ->
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        super.onChange(selfChange)
                        onChange()
                    }
                }
            },
            registerObservers = {},
            unregisterObserver = {}
        )

        syncManager.start()

        waitForError(reporter)
        syncManager.stop()

        assertEquals(AppErrorKind.StartupFatal, reporter.currentError.value?.kind)
        assertEquals(AppErrorOrigin.DeviceGroupSync, reporter.currentError.value?.origin)
        assertTrue(
            reporter.currentError.value?.technicalDetails?.contains("startup sync exploded") == true
        )
    }

    @Test
    fun `syncNow requests confirmation mode when auto sync setting is off`() = runBlocking {
        var capturedMode: DeviceSyncRingtoneMode? = null
        val syncManager = DeviceGroupSyncManager(
            contentResolver = noOpContentResolver(),
            source = object : DeviceGroupSource {
                override suspend fun loadSnapshot(): DeviceGroupSnapshot {
                    return DeviceGroupSnapshot(emptyList(), emptyList())
                }
            },
            repository = FakeGroupsRepository { _, mode ->
                capturedMode = mode
                GroupMutationResult.Success
            },
            settingsRepository = FakeSettingsRepository(
                AppSettings(autoSyncDeviceGroupChanges = false)
            )
        )

        syncManager.syncNow()

        assertEquals(DeviceSyncRingtoneMode.RequireConfirmation, capturedMode)
    }

    @Test
    fun `syncNow applies immediately when auto sync setting is on`() = runBlocking {
        var capturedMode: DeviceSyncRingtoneMode? = null
        val syncManager = DeviceGroupSyncManager(
            contentResolver = noOpContentResolver(),
            source = object : DeviceGroupSource {
                override suspend fun loadSnapshot(): DeviceGroupSnapshot {
                    return DeviceGroupSnapshot(emptyList(), emptyList())
                }
            },
            repository = FakeGroupsRepository { _, mode ->
                capturedMode = mode
                GroupMutationResult.Success
            },
            settingsRepository = FakeSettingsRepository(
                AppSettings(autoSyncDeviceGroupChanges = true)
            )
        )

        syncManager.syncNow()

        assertEquals(DeviceSyncRingtoneMode.ApplyImmediately, capturedMode)
    }

    private class FakeGroupsRepository(
        private val syncBlock: suspend (
            DeviceGroupSnapshot,
            DeviceSyncRingtoneMode
        ) -> GroupMutationResult = { _, _ ->
            GroupMutationResult.Success
        }
    ) : GroupsRepository {
        override val pendingDeviceSyncRingtoneConfirmation:
            StateFlow<DeviceSyncRingtoneConfirmation?> = MutableStateFlow(null)

        override fun observeGroups(): Flow<List<Group>> = emptyFlow()

        override fun observeMemberships(): Flow<List<GroupMembership>> = emptyFlow()

        override suspend fun getGroup(groupId: Int): Group? = null

        override suspend fun createLocalGroup(name: String, ringtoneUri: android.net.Uri?): GroupMutationResult {
            error("unused")
        }

        override suspend fun assignContactsToGroups(
            groupIds: List<Int>,
            contactIds: List<Long>
        ): GroupMutationResult {
            error("unused")
        }

        override suspend fun setContactGroups(
            contactId: Long,
            groupIds: List<Int>
        ): GroupMutationResult {
            error("unused")
        }

        override suspend fun removeContactFromGroup(
            groupId: Int,
            contactId: Long
        ): GroupMutationResult {
            error("unused")
        }

        override suspend fun changeGroupRingtone(
            groupId: Int,
            ringtoneUri: android.net.Uri?
        ): GroupMutationResult {
            error("unused")
        }

        override suspend fun deleteGroup(groupId: Int): GroupMutationResult {
            error("unused")
        }

        override suspend fun syncDeviceGroups(
            snapshot: DeviceGroupSnapshot,
            ringtoneMode: DeviceSyncRingtoneMode
        ): GroupMutationResult {
            return syncBlock(snapshot, ringtoneMode)
        }

        override suspend fun acceptPendingDeviceSyncRingtoneChanges(): GroupMutationResult {
            error("unused")
        }

        override fun cancelPendingDeviceSyncRingtoneChanges() = Unit
    }

    private class FakeSettingsRepository(
        initialSettings: AppSettings = AppSettings()
    ) : AppSettingsRepository {
        private val mutableSettings = MutableStateFlow(initialSettings)
        override val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

        override fun setAutoSyncDeviceGroupChanges(enabled: Boolean) {
            mutableSettings.value = mutableSettings.value.copy(autoSyncDeviceGroupChanges = enabled)
        }

        override fun setHasSeenMultipleGroupsRingtoneInfo(seen: Boolean) {
            mutableSettings.value =
                mutableSettings.value.copy(hasSeenMultipleGroupsRingtoneInfo = seen)
        }
    }

    private fun noOpContentResolver(): ContentResolver {
        return object : ContentResolver(null) {
        }
    }

    private fun waitForError(reporter: AppErrorReporter) {
        repeat(50) {
            if (reporter.currentError.value != null) {
                return
            }
            Thread.sleep(20)
        }
        error("Timed out waiting for app error to be reported")
    }
}
