package de.drvlabs.contactgrouper.contacts

import de.drvlabs.contactgrouper.groups.Group

enum class BulkGroupMembershipState {
    Unselected,
    Partial,
    Selected
}

fun initialBulkGroupMembershipStates(
    selectedContacts: List<Contact>,
    editableGroups: List<Group>
): Map<Int, BulkGroupMembershipState> {
    if (selectedContacts.isEmpty()) {
        return editableGroups.associate { it.id to BulkGroupMembershipState.Unselected }
    }

    val selectedContactCount = selectedContacts.size
    return editableGroups.associate { group ->
        val membershipCount = selectedContacts.count { contact -> group.id in contact.groupIds }
        val state = when (membershipCount) {
            0 -> BulkGroupMembershipState.Unselected
            selectedContactCount -> BulkGroupMembershipState.Selected
            else -> BulkGroupMembershipState.Partial
        }
        group.id to state
    }
}

fun nextBulkGroupMembershipState(
    state: BulkGroupMembershipState
): BulkGroupMembershipState {
    return when (state) {
        BulkGroupMembershipState.Partial -> BulkGroupMembershipState.Unselected
        BulkGroupMembershipState.Unselected -> BulkGroupMembershipState.Selected
        BulkGroupMembershipState.Selected -> BulkGroupMembershipState.Unselected
    }
}

fun buildBulkContactGroupSelections(
    selectedContacts: List<Contact>,
    editableGroups: List<Group>,
    groupStates: Map<Int, BulkGroupMembershipState>
): Map<Long, List<Int>> {
    val editableGroupIds = editableGroups.map(Group::id)
    return selectedContacts.associate { contact ->
        val selectedGroupIds = editableGroupIds.filter { groupId ->
            when (groupStates[groupId]) {
                BulkGroupMembershipState.Selected -> true
                BulkGroupMembershipState.Partial -> groupId in contact.groupIds
                BulkGroupMembershipState.Unselected,
                null -> false
            }
        }
        contact.id to selectedGroupIds
    }
}
