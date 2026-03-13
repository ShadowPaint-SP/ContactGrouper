package de.drvlabs.contactgrouper.groups

object DeviceMembershipAssignmentPolicy {

    fun assignTimestamps(
        newDeviceGroupIds: List<Long>,
        existingDeviceMemberships: List<GroupMembership>,
        existingAllMemberships: List<GroupMembership>,
        now: Long
    ): Map<Long, Long> {
        val distinctIds = newDeviceGroupIds.distinct().sorted()
        if (distinctIds.isEmpty()) {
            return emptyMap()
        }

        return if (existingDeviceMemberships.isEmpty()) {
            val oldestExisting = existingAllMemberships.minOfOrNull { it.assignedAt } ?: now
            val base = oldestExisting - distinctIds.size
            distinctIds
                .mapIndexed { index, deviceGroupId ->
                    deviceGroupId to (base + index)
                }
                .toMap()
        } else {
            var timestamp = now
            distinctIds
                .associateWith { timestamp++ }
        }
    }
}
