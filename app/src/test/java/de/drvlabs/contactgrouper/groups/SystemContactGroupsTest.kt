package de.drvlabs.contactgrouper.groups

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemContactGroupsTest {

    @Test
    fun `isReservedSystemGroupName matches My Contacts ignoring case and whitespace`() {
        assertTrue(isReservedSystemGroupName("My Contacts"))
        assertTrue(isReservedSystemGroupName(" my contacts "))
        assertTrue(isReservedSystemGroupName("MY CONTACTS"))
        assertFalse(isReservedSystemGroupName("Friends"))
    }

    @Test
    fun `withoutReservedSystemGroups removes My Contacts group and memberships`() {
        val snapshot = DeviceGroupSnapshot(
            groups = listOf(
                deviceGroupRecord(deviceGroupId = 1, title = "My Contacts"),
                deviceGroupRecord(deviceGroupId = 2, title = "Friends")
            ),
            memberships = listOf(
                DeviceGroupMembershipRecord(deviceGroupId = 1, contactId = 10),
                DeviceGroupMembershipRecord(deviceGroupId = 2, contactId = 10)
            )
        )

        val filtered = snapshot.withoutReservedSystemGroups()

        assertEquals(listOf(2L), filtered.groups.map(DeviceGroupRecord::deviceGroupId))
        assertEquals(
            listOf(DeviceGroupMembershipRecord(deviceGroupId = 2, contactId = 10)),
            filtered.memberships
        )
    }

    private fun deviceGroupRecord(deviceGroupId: Long, title: String): DeviceGroupRecord {
        return DeviceGroupRecord(
            deviceGroupId = deviceGroupId,
            title = title,
            accountName = null,
            accountType = null,
            dataSet = null,
            isReadOnly = false,
            isVisible = true
        )
    }
}
