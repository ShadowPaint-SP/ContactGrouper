package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
            repository = FakeGroupsRepository()
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
            repository = FakeGroupsRepository()
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
            repository = FakeGroupsRepository {
                throw IllegalArgumentException("broken repository")
            }
        )

        assertEquals(
            GroupMutationResult.ProviderWriteFailed(GroupMutationAction.SYNC_DEVICE_GROUPS),
            syncManager.syncNow()
        )
    }

    private class FakeGroupsRepository(
        private val syncBlock: suspend (DeviceGroupSnapshot) -> GroupMutationResult = {
            GroupMutationResult.Success
        }
    ) : GroupsRepository {
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

        override suspend fun syncDeviceGroups(snapshot: DeviceGroupSnapshot): GroupMutationResult {
            return syncBlock(snapshot)
        }
    }

    private fun noOpContentResolver(): ContentResolver {
        return object : ContentResolver(null) {
        }
    }
}
