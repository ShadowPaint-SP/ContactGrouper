package de.drvlabs.contactgrouper.groups

internal data class GroupMergePlan(
    val survivingGroup: Group,
    val mergedMemberships: List<GroupMembership>,
    val affectedContactIds: Set<Long>
)

internal fun planGroupCollisionMerge(
    sourceGroup: Group,
    targetGroup: Group,
    sourceMemberships: List<GroupMembership>,
    targetMemberships: List<GroupMembership>
): GroupMergePlan {
    val survivingGroup = if (
        sourceGroup.ringtoneUri != null &&
        sourceGroup.ringtoneUri != targetGroup.ringtoneUri
    ) {
        targetGroup.copy(ringtoneUri = sourceGroup.ringtoneUri)
    } else {
        targetGroup
    }

    val targetMembershipsByContact = targetMemberships.associateBy(GroupMembership::contactId)
    val mergedMemberships = sourceMemberships.map { sourceMembership ->
        val targetMembership = targetMembershipsByContact[sourceMembership.contactId]
        GroupMembership(
            groupId = targetGroup.id,
            contactId = sourceMembership.contactId,
            assignedAt = maxOf(
                sourceMembership.assignedAt,
                targetMembership?.assignedAt ?: Long.MIN_VALUE
            ),
            source = survivingGroup.syncSource
        )
    }

    val affectedContactIds = mutableSetOf<Long>()
    affectedContactIds += sourceMemberships.map(GroupMembership::contactId)
    affectedContactIds += targetMemberships.map(GroupMembership::contactId)

    return GroupMergePlan(
        survivingGroup = survivingGroup,
        mergedMemberships = mergedMemberships,
        affectedContactIds = affectedContactIds
    )
}
