package de.drvlabs.contactgrouper.groups

data class EditableMembershipUpdatePlan(
    val groupIdsToAdd: List<Int>,
    val membershipsToRemove: List<GroupMembership>
)

fun planEditableMembershipUpdate(
    currentMemberships: List<GroupMembership>,
    groupsById: Map<Int, Group>,
    selectedGroupIds: Set<Int>
): EditableMembershipUpdatePlan {
    val editableMemberships = currentMemberships.filter { membership ->
        groupsById[membership.groupId]?.isMembershipEditable == true
    }
    val existingEditableGroupIds = editableMemberships.map(GroupMembership::groupId).toSet()
    val selectedEditableGroupIds = selectedGroupIds.filterTo(linkedSetOf()) { groupId ->
        groupsById[groupId]?.isMembershipEditable == true
    }

    val groupIdsToAdd = groupsById.values
        .asSequence()
        .filter { it.id in selectedEditableGroupIds && it.id !in existingEditableGroupIds }
        .sortedBy { it.name.lowercase() }
        .map(Group::id)
        .toList()
    val membershipsToRemove = editableMemberships.filter { it.groupId !in selectedEditableGroupIds }

    return EditableMembershipUpdatePlan(
        groupIdsToAdd = groupIdsToAdd,
        membershipsToRemove = membershipsToRemove
    )
}
