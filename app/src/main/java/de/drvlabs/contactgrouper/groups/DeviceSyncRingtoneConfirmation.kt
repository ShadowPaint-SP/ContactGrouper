package de.drvlabs.contactgrouper.groups

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DeviceSyncRingtoneMode {
    ApplyImmediately,
    RequireConfirmation
}

data class DeviceSyncRingtoneConfirmation(
    val contactIds: Set<Long>,
    val groupIds: Set<Int>
) {
    val ringtoneCount: Int
        get() = contactIds.size

    val contactCount: Int
        get() = contactIds.size

    val groupCount: Int
        get() = groupIds.size
}

internal object DeviceSyncRingtonePreview {
    fun calculate(
        contactIds: Set<Long>,
        membershipsByContact: Map<Long, List<GroupMembership>>,
        groupsById: Map<Int, Group>,
        existingStatesByContact: Map<Long, ContactRingtoneState>,
        ringtoneUriForGroup: (Group) -> String? = { group -> group.ringtoneUri?.toString() }
    ): DeviceSyncRingtoneConfirmation? {
        val ringtoneContactIds = linkedSetOf<Long>()
        val involvedGroupIds = linkedSetOf<Int>()

        contactIds.forEach { contactId ->
            val memberships = membershipsByContact[contactId].orEmpty()
            val existingState = existingStatesByContact[contactId]
            val winnerGroupId = RingtoneResolution.resolveWinningMembershipId(memberships) { groupId ->
                groupsById[groupId]?.let(ringtoneUriForGroup) != null
            }
            val winnerMembership = winnerGroupId?.let { groupId ->
                memberships
                    .sortedByDescending { it.assignedAt }
                    .firstOrNull { it.groupId == groupId }
            }
            val winnerGroup = winnerGroupId?.let(groupsById::get)

            if (winnerMembership == null || winnerGroup == null) {
                val previousGroupId = existingState?.lastAppliedGroupId ?: return@forEach
                ringtoneContactIds += contactId
                involvedGroupIds += previousGroupId
                return@forEach
            }

            val winnerRingtoneUri = ringtoneUriForGroup(winnerGroup)
            val needsUpdate =
                existingState?.lastAppliedGroupId != winnerGroup.id ||
                    existingState.lastAppliedRingtoneUri != winnerRingtoneUri
            if (needsUpdate) {
                ringtoneContactIds += contactId
                involvedGroupIds += winnerGroup.id
            }
        }

        if (ringtoneContactIds.isEmpty()) {
            return null
        }

        return DeviceSyncRingtoneConfirmation(
            contactIds = ringtoneContactIds,
            groupIds = involvedGroupIds
        )
    }
}

internal class DeviceSyncRingtoneConfirmationController(
    private val loadPendingPreview: suspend () -> DeviceSyncRingtoneConfirmation?,
    private val applyRingtoneChanges: suspend (Set<Long>) -> Boolean
) {
    private val mutablePendingConfirmation =
        MutableStateFlow<DeviceSyncRingtoneConfirmation?>(null)
    val pendingConfirmation: StateFlow<DeviceSyncRingtoneConfirmation?> =
        mutablePendingConfirmation.asStateFlow()

    suspend fun handleDeviceSync(mode: DeviceSyncRingtoneMode): GroupMutationResult {
        val preview = loadPendingPreview()
        if (preview == null) {
            mutablePendingConfirmation.value = null
            return GroupMutationResult.Success
        }

        return when (mode) {
            DeviceSyncRingtoneMode.ApplyImmediately -> {
                mutablePendingConfirmation.value = null
                if (applyRingtoneChanges(preview.contactIds)) {
                    GroupMutationResult.Success
                } else {
                    GroupMutationResult.ProviderWriteFailed(GroupMutationAction.SYNC_DEVICE_GROUPS)
                }
            }

            DeviceSyncRingtoneMode.RequireConfirmation -> {
                mutablePendingConfirmation.value = preview
                GroupMutationResult.Success
            }
        }
    }

    suspend fun acceptPending(): GroupMutationResult {
        val preview = mutablePendingConfirmation.value ?: return GroupMutationResult.Success
        return if (applyRingtoneChanges(preview.contactIds)) {
            mutablePendingConfirmation.value = null
            GroupMutationResult.Success
        } else {
            GroupMutationResult.ProviderWriteFailed(GroupMutationAction.SYNC_DEVICE_GROUPS)
        }
    }

    fun cancelPending() {
        mutablePendingConfirmation.value = null
    }
}
