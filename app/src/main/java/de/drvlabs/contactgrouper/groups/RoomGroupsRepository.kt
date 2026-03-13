package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class RoomGroupsRepository(
    private val database: GroupDatabase,
    private val ringtoneGateway: ContactRingtoneGateway,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : GroupsRepository {

    override fun observeGroups(): Flow<List<Group>> = database.groupDao.getAllGroups()

    override fun observeMemberships(): Flow<List<GroupMembership>> =
        database.membershipDao.observeAllMemberships()

    override suspend fun getGroup(groupId: Int): Group? = database.groupDao.getGroupById(groupId)

    override suspend fun createLocalGroup(name: String, ringtoneUri: Uri?): Int {
        val groupId = database.groupDao.insertGroup(
            Group(
                name = name,
                ringtoneUri = ringtoneUri,
                color = randomColor(),
                syncSource = GroupSyncSource.LOCAL
            )
        )
        return groupId.toInt()
    }

    override suspend fun assignContactsToGroups(groupIds: List<Int>, contactIds: List<Long>) {
        val targetGroups = groupIds
            .distinct()
            .mapNotNull { database.groupDao.getGroupById(it) }
            .filter { it.isMembershipEditable }
        if (targetGroups.isEmpty() || contactIds.isEmpty()) {
            return
        }

        val orderedGroupIds = targetGroups.map { it.id }
        val affectedContacts = mutableSetOf<Long>()

        database.withTransaction {
            var timestamp = clock()
            contactIds.distinct().forEach { contactId ->
                orderedGroupIds.forEach { groupId ->
                    database.membershipDao.upsertMembership(
                        GroupMembership(
                            groupId = groupId,
                            contactId = contactId,
                            assignedAt = timestamp++,
                            source = GroupSyncSource.LOCAL
                        )
                    )
                    affectedContacts += contactId
                }
            }
        }

        refreshRingtones(affectedContacts)
    }

    override suspend fun removeContactFromGroup(groupId: Int, contactId: Long) {
        val group = database.groupDao.getGroupById(groupId) ?: return
        if (!group.isMembershipEditable) {
            return
        }

        database.membershipDao.deleteMembership(groupId, contactId)
        refreshRingtones(setOf(contactId))
    }

    override suspend fun changeGroupRingtone(groupId: Int, ringtoneUri: Uri?) {
        val group = database.groupDao.getGroupById(groupId) ?: return
        database.groupDao.updateGroup(group.copy(ringtoneUri = ringtoneUri))

        val affectedContacts = database.membershipDao
            .getMembershipsForGroup(groupId)
            .map { it.contactId }
            .toSet()
        refreshRingtones(affectedContacts)
    }

    override suspend fun deleteGroup(groupId: Int) {
        val group = database.groupDao.getGroupById(groupId) ?: return
        if (!group.isMembershipEditable) {
            return
        }

        val affectedContacts = database.membershipDao
            .getMembershipsForGroup(groupId)
            .map { it.contactId }
            .toSet()

        database.groupDao.deleteGroup(group)
        refreshRingtones(affectedContacts)
    }

    override suspend fun syncDeviceGroups(snapshot: DeviceGroupSnapshot) {
        val affectedContacts = mutableSetOf<Long>()

        database.withTransaction {
            val groupDao = database.groupDao
            val membershipDao = database.membershipDao
            val existingDeviceGroups = groupDao.getGroupsBySource(GroupSyncSource.DEVICE)
            val existingDeviceGroupsByDeviceId = existingDeviceGroups.associateBy { it.deviceGroupId }

            val upsertedGroupsByDeviceId = mutableMapOf<Long, Group>()
            snapshot.groups.forEach { deviceGroup ->
                val existingGroup = existingDeviceGroupsByDeviceId[deviceGroup.deviceGroupId]
                if (existingGroup == null) {
                    val insertedId = groupDao.insertGroup(
                        Group(
                            name = deviceGroup.title,
                            color = colorForDeviceGroup(deviceGroup.deviceGroupId),
                            syncSource = GroupSyncSource.DEVICE,
                            deviceGroupId = deviceGroup.deviceGroupId,
                            accountName = deviceGroup.accountName,
                            accountType = deviceGroup.accountType,
                            dataSet = deviceGroup.dataSet,
                            isReadOnly = deviceGroup.isReadOnly,
                            isVisible = deviceGroup.isVisible
                        )
                    ).toInt()
                    upsertedGroupsByDeviceId[deviceGroup.deviceGroupId] =
                        groupDao.getGroupById(insertedId)!!
                } else {
                    val updatedGroup = existingGroup.copy(
                        name = deviceGroup.title,
                        syncSource = GroupSyncSource.DEVICE,
                        deviceGroupId = deviceGroup.deviceGroupId,
                        accountName = deviceGroup.accountName,
                        accountType = deviceGroup.accountType,
                        dataSet = deviceGroup.dataSet,
                        isReadOnly = deviceGroup.isReadOnly,
                        isVisible = deviceGroup.isVisible
                    )
                    if (updatedGroup != existingGroup) {
                        groupDao.updateGroup(updatedGroup)
                    }
                    upsertedGroupsByDeviceId[deviceGroup.deviceGroupId] = updatedGroup
                }
            }

            val snapshotDeviceIds = snapshot.groups.map { it.deviceGroupId }.toSet()
            existingDeviceGroups
                .filter { it.deviceGroupId !in snapshotDeviceIds }
                .forEach { staleGroup ->
                    affectedContacts += membershipDao.getMembershipsForGroup(staleGroup.id).map { it.contactId }
                    groupDao.deleteGroup(staleGroup)
                }

            val existingDeviceMemberships = membershipDao.getMembershipsBySource(GroupSyncSource.DEVICE)
            val existingDeviceMembershipKeys = existingDeviceMemberships.map { it.groupId to it.contactId }.toSet()
            val incomingMemberships = snapshot.memberships.mapNotNull { membership ->
                val group = upsertedGroupsByDeviceId[membership.deviceGroupId] ?: return@mapNotNull null
                group.id to membership.contactId
            }.toSet()

            val membershipsByContact = membershipDao.getAllMemberships().groupBy { it.contactId }
            val existingDeviceMembershipsByContact = existingDeviceMemberships.groupBy { it.contactId }
            val incomingByContact = snapshot.memberships
                .groupBy { it.contactId }
                .mapValues { (_, memberships) ->
                    memberships.distinctBy { it.deviceGroupId }.sortedBy { it.deviceGroupId }
                }

            incomingByContact.forEach { (contactId, contactMemberships) ->
                val existingForContact = existingDeviceMembershipsByContact[contactId].orEmpty()
                val existingAllForContact = membershipsByContact[contactId].orEmpty()
                val newMemberships = contactMemberships.filter { membership ->
                    val localGroup = upsertedGroupsByDeviceId[membership.deviceGroupId] ?: return@filter false
                    (localGroup.id to contactId) !in existingDeviceMembershipKeys
                }
                if (newMemberships.isEmpty()) {
                    return@forEach
                }

                val timestampsByDeviceGroupId = if (existingForContact.isEmpty()) {
                    DeviceMembershipAssignmentPolicy.assignTimestamps(
                        newDeviceGroupIds = newMemberships.map { it.deviceGroupId },
                        existingDeviceMemberships = existingForContact,
                        existingAllMemberships = existingAllForContact,
                        now = clock()
                    )
                } else {
                    DeviceMembershipAssignmentPolicy.assignTimestamps(
                        newDeviceGroupIds = newMemberships.map { it.deviceGroupId },
                        existingDeviceMemberships = existingForContact,
                        existingAllMemberships = existingAllForContact,
                        now = clock()
                    )
                }

                newMemberships.forEach { membership ->
                    val group = upsertedGroupsByDeviceId[membership.deviceGroupId] ?: return@forEach
                    membershipDao.upsertMembership(
                        GroupMembership(
                            groupId = group.id,
                            contactId = contactId,
                            assignedAt = timestampsByDeviceGroupId.getValue(membership.deviceGroupId),
                            source = GroupSyncSource.DEVICE
                        )
                    )
                    affectedContacts += contactId
                }
            }

            existingDeviceMemberships
                .filter { (it.groupId to it.contactId) !in incomingMemberships }
                .forEach { membership ->
                    membershipDao.deleteMembership(membership.groupId, membership.contactId)
                    affectedContacts += membership.contactId
                }
        }

        refreshRingtones(affectedContacts)
    }

    private suspend fun refreshRingtones(contactIds: Set<Long>) {
        contactIds.forEach { contactId ->
            refreshRingtone(contactId)
        }
    }

    private suspend fun refreshRingtone(contactId: Long) {
        val memberships = database.membershipDao.getMembershipsForContact(contactId)
        val groupsById = memberships
            .mapNotNull { membership -> database.groupDao.getGroupById(membership.groupId) }
            .associateBy { it.id }

        val winner = RingtoneResolution.resolveWinningMembership(groupsById, memberships)
        val stateDao = database.contactRingtoneStateDao
        val existingState = stateDao.getByContactId(contactId)

        if (winner == null) {
            if (existingState?.lastAppliedGroupId != null) {
                ringtoneGateway.applyRingtone(contactId, existingState.baselineRingtoneUri)
                stateDao.upsert(
                    existingState.copy(
                        lastAppliedGroupId = null,
                        lastAppliedRingtoneUri = null
                    )
                )
            }
            return
        }

        val winnerRingtoneUri = winner.group.ringtoneUri?.toString()
        val baseline = if (existingState?.lastAppliedGroupId == null) {
            ringtoneGateway.getCurrentRingtone(contactId)
        } else {
            existingState.baselineRingtoneUri
        }

        if (
            existingState?.lastAppliedGroupId != winner.group.id ||
            existingState.lastAppliedRingtoneUri != winnerRingtoneUri
        ) {
            ringtoneGateway.applyRingtone(contactId, winnerRingtoneUri)
        }

        stateDao.upsert(
            ContactRingtoneState(
                contactId = contactId,
                baselineRingtoneUri = baseline,
                lastAppliedGroupId = winner.group.id,
                lastAppliedRingtoneUri = winnerRingtoneUri
            )
        )
    }

    private fun randomColor(): Color {
        return Color(Random.nextLong(0xFFFFFF)).copy(alpha = 0.5f)
    }

    private fun colorForDeviceGroup(deviceGroupId: Long): Color {
        val red = ((deviceGroupId * 53) % 180 + 50).toInt()
        val green = ((deviceGroupId * 97) % 180 + 50).toInt()
        val blue = ((deviceGroupId * 193) % 180 + 50).toInt()
        return Color(red, green, blue, 180)
    }
}
