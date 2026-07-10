package de.drvlabs.contactgrouper.contacts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.drvlabs.contactgrouper.R
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupMembership
import de.drvlabs.contactgrouper.groups.RingtoneResolution

internal data class MultipleGroupsRingtoneInfo(
    val contactName: String,
    val controllingGroupName: String
)

internal fun findMultipleGroupsRingtoneInfoAfterAssignments(
    contacts: List<Contact>,
    groups: List<Group>,
    assignedEditableGroupIdsByContact: Map<Long, List<Int>>,
    hasSeenMultipleGroupsRingtoneInfo: Boolean
): MultipleGroupsRingtoneInfo? {
    val groupsById = groups.associateBy(Group::id)
    return findMultipleGroupsRingtoneInfoAfterAssignments(
        contacts = contacts,
        assignedEditableGroupIdsByContact = assignedEditableGroupIdsByContact,
        hasSeenMultipleGroupsRingtoneInfo = hasSeenMultipleGroupsRingtoneInfo,
        isMembershipEditable = { groupId ->
            groupsById[groupId]?.isMembershipEditable == true
        },
        groupName = { groupId -> groupsById[groupId]?.name },
        hasRingtone = { groupId -> groupsById[groupId]?.ringtoneUri != null }
    )
}

internal fun findMultipleGroupsRingtoneInfoAfterAssignments(
    contacts: List<Contact>,
    assignedEditableGroupIdsByContact: Map<Long, List<Int>>,
    hasSeenMultipleGroupsRingtoneInfo: Boolean,
    isMembershipEditable: (Int) -> Boolean,
    groupName: (Int) -> String?,
    hasRingtone: (Int) -> Boolean
): MultipleGroupsRingtoneInfo? {
    if (hasSeenMultipleGroupsRingtoneInfo) {
        return null
    }

    val contactsById = contacts.associateBy(Contact::id)
    assignedEditableGroupIdsByContact.forEach { (contactId, selectedEditableGroupIds) ->
        val contact = contactsById[contactId] ?: return@forEach
        val currentEditableGroupIds = contact.groupIds
            .filter(isMembershipEditable)
            .toSet()
        val selectedEditableGroupIdSet = selectedEditableGroupIds
            .filter(isMembershipEditable)
            .toSet()
        if (currentEditableGroupIds == selectedEditableGroupIdSet) {
            return@forEach
        }

        val finalGroupIds = finalGroupIdsAfterSetContactGroups(
            currentGroupIdsNewestFirst = contact.groupIds,
            selectedEditableGroupIds = selectedEditableGroupIds,
            isMembershipEditable = isMembershipEditable,
            groupName = groupName
        )
        if (finalGroupIds.size < 2) {
            return@forEach
        }

        val winningGroupId = RingtoneResolution.resolveWinningMembershipId(
            memberships = finalGroupIds.mapIndexed { index, groupId ->
                GroupMembership(
                    groupId = groupId,
                    contactId = contactId,
                    assignedAt = (finalGroupIds.size - index).toLong()
                )
            },
            hasRingtone = hasRingtone
        ) ?: return@forEach

        val winningGroupName = groupName(winningGroupId) ?: return@forEach
        return MultipleGroupsRingtoneInfo(
            contactName = contact.displayName,
            controllingGroupName = winningGroupName
        )
    }

    return null
}

private fun finalGroupIdsAfterSetContactGroups(
    currentGroupIdsNewestFirst: List<Int>,
    selectedEditableGroupIds: List<Int>,
    isMembershipEditable: (Int) -> Boolean,
    groupName: (Int) -> String?
): List<Int> {
    val currentEditableGroupIds = currentGroupIdsNewestFirst
        .filter(isMembershipEditable)
        .toSet()
    val selectedEditableGroupIdSet = selectedEditableGroupIds
        .filter(isMembershipEditable)
        .toSet()
    val addedGroupIds = selectedEditableGroupIdSet
        .filter { groupId -> groupId !in currentEditableGroupIds }
        .sortedBy { groupId -> groupName(groupId).orEmpty().lowercase() }
    val removedEditableGroupIds = currentEditableGroupIds - selectedEditableGroupIdSet
    val retainedCurrentGroupIds = currentGroupIdsNewestFirst.filter { groupId ->
        groupId !in removedEditableGroupIds && groupId !in addedGroupIds
    }

    return (addedGroupIds.asReversed() + retainedCurrentGroupIds).distinct()
}

@Composable
internal fun MultipleGroupsRingtoneInfoDialog(
    info: MultipleGroupsRingtoneInfo,
    onAcknowledge: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onAcknowledge,
        title = {
            Text(stringResource(R.string.multiple_groups_ringtone_info_title))
        },
        text = {
            Text(
                stringResource(
                    R.string.multiple_groups_ringtone_info_message,
                    info.contactName,
                    info.controllingGroupName
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onAcknowledge) {
                Text(stringResource(R.string.action_ok))
            }
        }
    )
}
