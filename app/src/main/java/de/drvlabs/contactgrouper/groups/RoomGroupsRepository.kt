package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class RoomGroupsRepository(
    private val database: GroupDatabase,
    private val ringtoneGateway: ContactRingtoneGateway,
    private val deviceGroupWriteGateway: DeviceGroupWriteGateway,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : GroupsRepository {

    private data class MirrorOutcome(
        val group: Group,
        val mirroredToDevice: Boolean
    )

    override fun observeGroups(): Flow<List<Group>> = database.groupDao.getAllGroups()

    override fun observeMemberships(): Flow<List<GroupMembership>> =
        database.membershipDao.observeAllMemberships()

    override suspend fun getGroup(groupId: Int): Group? = database.groupDao.getGroupById(groupId)

    override suspend fun createLocalGroup(name: String, ringtoneUri: Uri?): GroupMutationResult {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return GroupMutationResult.InvalidRequest
        }

        return runMutation {
            database.groupDao.insertGroup(
                Group(
                    name = trimmedName,
                    ringtoneUri = ringtoneUri,
                    color = randomColor(),
                    syncSource = GroupSyncSource.LOCAL
                )
            )
            GroupMutationResult.Success
        }
    }

    override suspend fun assignContactsToGroups(
        groupIds: List<Int>,
        contactIds: List<Long>
    ): GroupMutationResult {
        return runMutation {
            val distinctContactIds = contactIds.distinct()
            val originalTargetGroups = groupIds
                .distinct()
                .mapNotNull { database.groupDao.getGroupById(it) }
                .filter { it.isMembershipEditable }
            if (originalTargetGroups.isEmpty() || distinctContactIds.isEmpty()) {
                return@runMutation GroupMutationResult.InvalidRequest
            }

            val lookupCache = ContactAccountLookupCache()
            var providerWriteFailed = false
            val targetGroups = originalTargetGroups.map { group ->
                val mirrorOutcome = ensureMirrorForLocalGroup(group, distinctContactIds, lookupCache)
                if (!mirrorOutcome.mirroredToDevice) {
                    providerWriteFailed = true
                }
                mirrorOutcome.group
            }
            val affectedContacts = mutableSetOf<Long>()

            database.withTransaction {
                var timestamp = clock()
                distinctContactIds.forEach { contactId ->
                    targetGroups.forEach { group ->
                        database.membershipDao.upsertMembership(
                            GroupMembership(
                                groupId = group.id,
                                contactId = contactId,
                                assignedAt = timestamp++,
                                source = group.syncSource
                            )
                        )
                        affectedContacts += contactId
                    }
                }
            }

            targetGroups.forEach { group ->
                val deviceGroupId = group.deviceGroupId ?: return@forEach
                distinctContactIds.forEach { contactId ->
                    val synced = deviceGroupWriteGateway.addContactToGroup(
                        contactId = contactId,
                        group = group,
                        cache = lookupCache
                    )
                    if (!synced && deviceGroupId == group.deviceGroupId) {
                        providerWriteFailed = true
                    }
                }
            }

            if (!refreshRingtones(affectedContacts)) {
                providerWriteFailed = true
            }

            if (providerWriteFailed) {
                GroupMutationResult.ProviderWriteFailed(GroupMutationAction.ASSIGN_CONTACTS)
            } else {
                GroupMutationResult.Success
            }
        }
    }

    override suspend fun removeContactFromGroup(
        groupId: Int,
        contactId: Long
    ): GroupMutationResult {
        return runMutation {
            val group = database.groupDao.getGroupById(groupId) ?: return@runMutation GroupMutationResult.InvalidRequest
            if (!group.isMembershipEditable) {
                return@runMutation GroupMutationResult.Conflict
            }

            database.membershipDao.deleteMembership(groupId, contactId)
            var providerWriteFailed = false
            group.deviceGroupId?.let { deviceGroupId ->
                if (!deviceGroupWriteGateway.removeContactFromGroup(contactId, deviceGroupId)) {
                    providerWriteFailed = true
                }
            }
            if (!refreshRingtones(setOf(contactId))) {
                providerWriteFailed = true
            }

            if (providerWriteFailed) {
                GroupMutationResult.ProviderWriteFailed(GroupMutationAction.REMOVE_MEMBERSHIP)
            } else {
                GroupMutationResult.Success
            }
        }
    }

    override suspend fun changeGroupRingtone(
        groupId: Int,
        ringtoneUri: Uri?
    ): GroupMutationResult {
        return runMutation {
            val group = database.groupDao.getGroupById(groupId) ?: return@runMutation GroupMutationResult.InvalidRequest
            database.groupDao.updateGroup(group.copy(ringtoneUri = ringtoneUri))

            val affectedContacts = database.membershipDao
                .getMembershipsForGroup(groupId)
                .map { it.contactId }
                .toSet()

            if (refreshRingtones(affectedContacts)) {
                GroupMutationResult.Success
            } else {
                GroupMutationResult.ProviderWriteFailed(GroupMutationAction.CHANGE_RINGTONE)
            }
        }
    }

    override suspend fun deleteGroup(groupId: Int): GroupMutationResult {
        return runMutation {
            val group = database.groupDao.getGroupById(groupId) ?: return@runMutation GroupMutationResult.InvalidRequest
            if (!group.canDelete) {
                return@runMutation GroupMutationResult.Conflict
            }

            group.deviceGroupId?.let { deviceGroupId ->
                if (!deviceGroupWriteGateway.deleteGroup(deviceGroupId)) {
                    return@runMutation GroupMutationResult.ProviderWriteFailed(
                        GroupMutationAction.DELETE_GROUP
                    )
                }
            }

            val affectedContacts = database.membershipDao
                .getMembershipsForGroup(groupId)
                .map { it.contactId }
                .toSet()

            database.groupDao.deleteGroup(group)

            if (refreshRingtones(affectedContacts)) {
                GroupMutationResult.Success
            } else {
                GroupMutationResult.ProviderWriteFailed(GroupMutationAction.DELETE_GROUP)
            }
        }
    }

    override suspend fun syncDeviceGroups(snapshot: DeviceGroupSnapshot): GroupMutationResult {
        return runMutation {
            val affectedContacts = mutableSetOf<Long>()

            database.withTransaction {
                val groupDao = database.groupDao
                val membershipDao = database.membershipDao
                val mirroredLocalDeviceGroupIds = groupDao
                    .getGroupsBySource(GroupSyncSource.LOCAL)
                    .mapNotNull { it.deviceGroupId }
                    .toSet()
                val importableSnapshotGroups = snapshot.groups
                    .filter { it.deviceGroupId !in mirroredLocalDeviceGroupIds }
                val importableSnapshotMemberships = snapshot.memberships
                    .filter { it.deviceGroupId !in mirroredLocalDeviceGroupIds }
                val existingDeviceGroups = groupDao.getGroupsBySource(GroupSyncSource.DEVICE)
                val existingDeviceGroupsByDeviceId =
                    existingDeviceGroups.associateBy { it.deviceGroupId }

                val upsertedGroupsByDeviceId = mutableMapOf<Long, Group>()
                importableSnapshotGroups.forEach { deviceGroup ->
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

                val snapshotDeviceIds = importableSnapshotGroups.map { it.deviceGroupId }.toSet()
                existingDeviceGroups
                    .filter { it.deviceGroupId !in snapshotDeviceIds }
                    .forEach { staleGroup ->
                        affectedContacts += membershipDao
                            .getMembershipsForGroup(staleGroup.id)
                            .map { it.contactId }
                        groupDao.deleteGroup(staleGroup)
                    }

                val existingDeviceMemberships =
                    membershipDao.getMembershipsBySource(GroupSyncSource.DEVICE)
                val existingDeviceMembershipKeys =
                    existingDeviceMemberships.map { it.groupId to it.contactId }.toSet()
                val incomingMemberships = importableSnapshotMemberships.mapNotNull { membership ->
                    val group =
                        upsertedGroupsByDeviceId[membership.deviceGroupId] ?: return@mapNotNull null
                    group.id to membership.contactId
                }.toSet()

                val membershipsByContact = membershipDao.getAllMemberships().groupBy { it.contactId }
                val existingDeviceMembershipsByContact =
                    existingDeviceMemberships.groupBy { it.contactId }
                val incomingByContact = importableSnapshotMemberships
                    .groupBy { it.contactId }
                    .mapValues { (_, memberships) ->
                        memberships.distinctBy { it.deviceGroupId }.sortedBy { it.deviceGroupId }
                    }

                incomingByContact.forEach { (contactId, contactMemberships) ->
                    val existingForContact = existingDeviceMembershipsByContact[contactId].orEmpty()
                    val existingAllForContact = membershipsByContact[contactId].orEmpty()
                    val newMemberships = contactMemberships.filter { membership ->
                        val localGroup =
                            upsertedGroupsByDeviceId[membership.deviceGroupId] ?: return@filter false
                        (localGroup.id to contactId) !in existingDeviceMembershipKeys
                    }
                    if (newMemberships.isEmpty()) {
                        return@forEach
                    }

                    val timestampsByDeviceGroupId = DeviceMembershipAssignmentPolicy.assignTimestamps(
                        newDeviceGroupIds = newMemberships.map { it.deviceGroupId },
                        existingDeviceMemberships = existingForContact,
                        existingAllMemberships = existingAllForContact,
                        now = clock()
                    )

                    newMemberships.forEach { membership ->
                        val group =
                            upsertedGroupsByDeviceId[membership.deviceGroupId] ?: return@forEach
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

            if (refreshRingtones(affectedContacts)) {
                GroupMutationResult.Success
            } else {
                GroupMutationResult.ProviderWriteFailed(GroupMutationAction.SYNC_DEVICE_GROUPS)
            }
        }
    }

    private suspend fun refreshRingtones(contactIds: Set<Long>): Boolean {
        if (contactIds.isEmpty()) {
            return true
        }

        val distinctContactIds = contactIds.toList().distinct()
        val membershipsByContact = database.membershipDao
            .getMembershipsForContacts(distinctContactIds)
            .groupBy { it.contactId }
        val groupIds = membershipsByContact.values
            .flatten()
            .map(GroupMembership::groupId)
            .distinct()
        val groupsById = if (groupIds.isEmpty()) {
            emptyMap()
        } else {
            database.groupDao
                .getGroupsByIds(groupIds)
                .associateBy { it.id }
        }
        val existingStatesByContact = database.contactRingtoneStateDao
            .getByContactIds(distinctContactIds)
            .associateBy { it.contactId }

        var allUpdatesSucceeded = true
        distinctContactIds.forEach { contactId ->
            val updated = refreshRingtone(
                contactId = contactId,
                memberships = membershipsByContact[contactId].orEmpty(),
                groupsById = groupsById,
                existingState = existingStatesByContact[contactId]
            )
            allUpdatesSucceeded = allUpdatesSucceeded && updated
        }
        return allUpdatesSucceeded
    }

    private suspend fun refreshRingtone(
        contactId: Long,
        memberships: List<GroupMembership>,
        groupsById: Map<Int, Group>,
        existingState: ContactRingtoneState?
    ): Boolean {
        val winner = RingtoneResolution.resolveWinningMembership(groupsById, memberships)
        val stateDao = database.contactRingtoneStateDao

        if (winner == null) {
            if (existingState?.lastAppliedGroupId != null) {
                val applied = ringtoneGateway.applyRingtone(
                    contactId,
                    existingState.baselineRingtoneUri
                )
                if (!applied) {
                    return false
                }
                stateDao.upsert(
                    existingState.copy(
                        lastAppliedGroupId = null,
                        lastAppliedRingtoneUri = null
                    )
                )
            }
            return true
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
            val applied = ringtoneGateway.applyRingtone(contactId, winnerRingtoneUri)
            if (!applied) {
                return false
            }
        }

        stateDao.upsert(
            ContactRingtoneState(
                contactId = contactId,
                baselineRingtoneUri = baseline,
                lastAppliedGroupId = winner.group.id,
                lastAppliedRingtoneUri = winnerRingtoneUri
            )
        )
        return true
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

    private suspend fun ensureMirrorForLocalGroup(
        group: Group,
        contactIds: List<Long>,
        cache: ContactAccountLookupCache
    ): MirrorOutcome {
        if (group.syncSource != GroupSyncSource.LOCAL || group.deviceGroupId != null) {
            return MirrorOutcome(group = group, mirroredToDevice = true)
        }

        var account: ContactAccount? = null
        for (contactId in contactIds) {
            account = deviceGroupWriteGateway.findAccountForContact(contactId, cache)
            if (account != null) {
                break
            }
        }
        val deviceGroupId = deviceGroupWriteGateway.ensureGroup(group.name, account)
            ?: return MirrorOutcome(group = group, mirroredToDevice = false)
        val mirroredGroup = group.copy(
            deviceGroupId = deviceGroupId,
            accountName = account?.accountName,
            accountType = account?.accountType,
            dataSet = account?.dataSet
        )
        database.groupDao.updateGroup(mirroredGroup)
        return MirrorOutcome(group = mirroredGroup, mirroredToDevice = true)
    }

    private suspend fun runMutation(
        block: suspend () -> GroupMutationResult
    ): GroupMutationResult {
        return try {
            block()
        } catch (_: SecurityException) {
            GroupMutationResult.PermissionDenied
        }
    }
}
