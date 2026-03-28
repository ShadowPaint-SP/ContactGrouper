package de.drvlabs.contactgrouper.groups

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceMembershipAssignmentPolicyTest {

    @Test
    fun `initial import keeps highest device group id as newest membership`() {
        val assigned = DeviceMembershipAssignmentPolicy.assignTimestamps(
            newDeviceGroupIds = listOf(25L, 10L, 40L),
            existingDeviceMemberships = emptyList(),
            existingAllMemberships = emptyList(),
            now = 1_000L
        )

        assertEquals(997L, assigned.getValue(10L))
        assertEquals(998L, assigned.getValue(25L))
        assertEquals(999L, assigned.getValue(40L))
    }

    @Test
    fun `initial import stays older than existing local memberships`() {
        val assigned = DeviceMembershipAssignmentPolicy.assignTimestamps(
            newDeviceGroupIds = listOf(3L, 9L),
            existingDeviceMemberships = emptyList(),
            existingAllMemberships = listOf(
                GroupMembership(groupId = 1, contactId = 42L, assignedAt = 100L)
            ),
            now = 500L
        )

        assertEquals(98L, assigned.getValue(3L))
        assertEquals(99L, assigned.getValue(9L))
    }

    @Test
    fun `later device membership additions become newest`() {
        val assigned = DeviceMembershipAssignmentPolicy.assignTimestamps(
            newDeviceGroupIds = listOf(8L, 2L),
            existingDeviceMemberships = listOf(
                GroupMembership(groupId = 1, contactId = 7L, assignedAt = 12L, source = GroupSyncSource.DEVICE)
            ),
            existingAllMemberships = listOf(
                GroupMembership(groupId = 1, contactId = 7L, assignedAt = 12L, source = GroupSyncSource.DEVICE)
            ),
            now = 500L
        )

        assertEquals(500L, assigned.getValue(2L))
        assertEquals(501L, assigned.getValue(8L))
    }

    @Test
    fun `initial import deduplicates and scales for large device snapshots`() {
        val deviceGroupIds = buildList {
            for (id in 250L downTo 1L) {
                add(id)
                if (id % 25L == 0L) {
                    add(id)
                }
            }
        }

        val assigned = DeviceMembershipAssignmentPolicy.assignTimestamps(
            newDeviceGroupIds = deviceGroupIds,
            existingDeviceMemberships = emptyList(),
            existingAllMemberships = emptyList(),
            now = 1_000L
        )

        assertEquals(250, assigned.size)
        assertEquals(750L, assigned.getValue(1L))
        assertEquals(999L, assigned.getValue(250L))
        (1L..250L).forEach { deviceGroupId ->
            assertEquals(749L + deviceGroupId, assigned.getValue(deviceGroupId))
        }
    }

    @Test
    fun `later imports still deduplicate repeated group ids`() {
        val assigned = DeviceMembershipAssignmentPolicy.assignTimestamps(
            newDeviceGroupIds = listOf(8L, 2L, 8L, 2L),
            existingDeviceMemberships = listOf(
                GroupMembership(groupId = 1, contactId = 7L, assignedAt = 12L, source = GroupSyncSource.DEVICE)
            ),
            existingAllMemberships = listOf(
                GroupMembership(groupId = 1, contactId = 7L, assignedAt = 12L, source = GroupSyncSource.DEVICE)
            ),
            now = 800L
        )

        assertEquals(mapOf(2L to 800L, 8L to 801L), assigned)
    }
}
