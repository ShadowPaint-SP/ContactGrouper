package de.drvlabs.contactgrouper.groups

import androidx.room.InvalidationTracker
import de.drvlabs.contactgrouper.AppErrorKind
import de.drvlabs.contactgrouper.AppErrorOrigin
import de.drvlabs.contactgrouper.AppErrorReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomGroupsRepositoryTest {

    @Test
    fun `createLocalGroup reports unexpected exception and returns existing mutation result`() =
        runBlocking {
            val reporter = AppErrorReporter()
            val repository = RoomGroupsRepository(
                database = ThrowingGroupDatabase(),
                ringtoneGateway = NoOpContactRingtoneGateway,
                deviceGroupWriteGateway = NoOpDeviceGroupWriteGateway,
                appErrorReporter = reporter
            )

            val result = repository.createLocalGroup("Family", null)
            val error = reporter.currentError.value

            assertEquals(GroupMutationResult.InvalidRequest, result)
            assertEquals(AppErrorKind.RuntimeUnexpected, error?.kind)
            assertEquals(AppErrorOrigin.GroupMutation, error?.origin)
            assertTrue(error?.technicalDetails?.contains("insertGroup blew up") == true)
        }

    @Test
    fun `createLocalGroup rejects My Contacts system group name before writing`() = runBlocking {
        val repository = RoomGroupsRepository(
            database = ThrowingGroupDatabase(),
            ringtoneGateway = NoOpContactRingtoneGateway,
            deviceGroupWriteGateway = NoOpDeviceGroupWriteGateway
        )

        val result = repository.createLocalGroup(" my contacts ", null)

        assertEquals(GroupMutationResult.ReservedSystemGroupName, result)
    }

    private class ThrowingGroupDatabase : GroupDatabase() {
        override val groupDao: GroupDao = object : GroupDao {
            override suspend fun insertGroup(group: Group): Long {
                throw IllegalStateException("insertGroup blew up")
            }

            override suspend fun updateGroup(group: Group) = error("unused")

            override suspend fun deleteGroup(group: Group) = error("unused")

            override fun getAllGroups(): Flow<List<Group>> = emptyFlow()

            override fun observeGroupById(groupId: Int): Flow<Group?> = emptyFlow()

            override suspend fun getGroupById(groupId: Int): Group? = null

            override suspend fun getGroupByDeviceGroupId(deviceGroupId: Long): Group? = null

            override suspend fun getGroupsByIds(groupIds: List<Int>): List<Group> = emptyList()

            override suspend fun getGroupsBySource(source: GroupSyncSource): List<Group> =
                emptyList()
        }

        override val membershipDao: GroupMembershipDao = object : GroupMembershipDao {
            override suspend fun upsertMembership(membership: GroupMembership) = error("unused")

            override suspend fun upsertMemberships(memberships: List<GroupMembership>) =
                error("unused")

            override suspend fun deleteMembership(groupId: Int, contactId: Long) = error("unused")

            override suspend fun deleteMembershipsForGroup(groupId: Int) = error("unused")

            override fun observeAllMemberships(): Flow<List<GroupMembership>> = emptyFlow()

            override suspend fun getAllMemberships(): List<GroupMembership> = emptyList()

            override suspend fun getMembershipsBySource(source: GroupSyncSource): List<GroupMembership> =
                emptyList()

            override suspend fun getMembershipsForGroup(groupId: Int): List<GroupMembership> =
                emptyList()

            override suspend fun getMembershipsForContact(contactId: Long): List<GroupMembership> =
                emptyList()

            override suspend fun getMembershipsForContacts(contactIds: List<Long>): List<GroupMembership> =
                emptyList()
        }

        override val contactRingtoneStateDao: ContactRingtoneStateDao =
            object : ContactRingtoneStateDao {
                override suspend fun upsert(state: ContactRingtoneState) = error("unused")

                override suspend fun getByContactId(contactId: Long): ContactRingtoneState? = null

                override suspend fun getByContactIds(contactIds: List<Long>): List<ContactRingtoneState> =
                    emptyList()
            }

        override fun createInvalidationTracker(): InvalidationTracker {
            return InvalidationTracker(
                this,
                mapOf(),
                mapOf(),
                "Group",
                "GroupMembership",
                "ContactRingtoneState"
            )
        }

        override fun clearAllTables() = Unit
    }

    private object NoOpContactRingtoneGateway : ContactRingtoneGateway {
        override suspend fun getCurrentRingtone(contactId: Long): String? = null

        override suspend fun applyRingtone(contactId: Long, ringtoneUri: String?): Boolean = true
    }

    private object NoOpDeviceGroupWriteGateway : DeviceGroupWriteGateway {
        override suspend fun findAccountForContact(
            contactId: Long,
            cache: ContactAccountLookupCache?
        ): ContactAccount? = null

        override suspend fun ensureGroup(title: String, account: ContactAccount?): Long? = null

        override suspend fun addContactToGroup(
            contactId: Long,
            group: Group,
            cache: ContactAccountLookupCache?
        ): Boolean = false

        override suspend fun removeContactFromGroup(contactId: Long, deviceGroupId: Long): Boolean =
            false

        override suspend fun deleteGroup(deviceGroupId: Long): Boolean = false
    }
}
