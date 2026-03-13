package de.drvlabs.contactgrouper.groups

data class EffectiveGroupMembership(
    val group: Group,
    val membership: GroupMembership
)

object RingtoneResolution {

    fun resolvePrimaryMembershipId(memberships: List<GroupMembership>): Int? {
        return memberships
            .sortedByDescending { it.assignedAt }
            .firstOrNull()
            ?.groupId
    }

    fun resolveWinningMembershipId(
        memberships: List<GroupMembership>,
        hasRingtone: (groupId: Int) -> Boolean
    ): Int? {
        return memberships
            .sortedByDescending { it.assignedAt }
            .firstOrNull { membership -> hasRingtone(membership.groupId) }
            ?.groupId
    }

    fun resolveWinningMembership(
        groupsById: Map<Int, Group>,
        memberships: List<GroupMembership>
    ): EffectiveGroupMembership? {
        val winnerGroupId = resolveWinningMembershipId(memberships) { groupId ->
            groupsById[groupId]?.ringtoneUri != null
        } ?: return null
        val winner = memberships
            .sortedByDescending { it.assignedAt }
            .firstOrNull { it.groupId == winnerGroupId }
            ?: return null

        val winnerGroup = groupsById[winner.groupId] ?: return null
        return EffectiveGroupMembership(
            group = winnerGroup,
            membership = winner
        )
    }
}
