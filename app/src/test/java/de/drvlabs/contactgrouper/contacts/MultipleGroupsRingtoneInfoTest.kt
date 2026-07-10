package de.drvlabs.contactgrouper.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultipleGroupsRingtoneInfoTest {

    @Test
    fun `newest assigned ringtone group controls contact detail assignment info`() {
        val info = findInfo(
            contacts = listOf(contact(id = 42L, displayName = "Jane Doe", groupIds = listOf(1))),
            assignments = linkedMapOf(42L to listOf(1, 2, 3)),
            groupNames = mapOf(
                1 to "Family",
                2 to "Friends",
                3 to "Work"
            ),
            ringtoneGroupIds = setOf(1, 2, 3)
        )

        assertEquals(
            MultipleGroupsRingtoneInfo(
                contactName = "Jane Doe",
                controllingGroupName = "Work"
            ),
            info
        )
    }

    @Test
    fun `resolver falls back to older ringtone group when newest assigned group is silent`() {
        val info = findInfo(
            contacts = listOf(contact(id = 42L, displayName = "Jane Doe", groupIds = listOf(1))),
            assignments = linkedMapOf(42L to listOf(1, 2)),
            groupNames = mapOf(
                1 to "Family",
                2 to "Work"
            ),
            ringtoneGroupIds = setOf(1)
        )

        assertEquals(
            MultipleGroupsRingtoneInfo(
                contactName = "Jane Doe",
                controllingGroupName = "Family"
            ),
            info
        )
    }

    @Test
    fun `bulk assignment returns first changed contact that ends with multiple groups and ringtone`() {
        val info = findInfo(
            contacts = listOf(
                contact(id = 10L, displayName = "No Change", groupIds = listOf(1)),
                contact(id = 20L, displayName = "Sam Taylor", groupIds = listOf(1))
            ),
            assignments = linkedMapOf(
                10L to listOf(1),
                20L to listOf(1, 2)
            ),
            groupNames = mapOf(
                1 to "Family",
                2 to "Work"
            ),
            ringtoneGroupIds = setOf(2)
        )

        assertEquals(
            MultipleGroupsRingtoneInfo(
                contactName = "Sam Taylor",
                controllingGroupName = "Work"
            ),
            info
        )
    }

    @Test
    fun `seen flag prevents triggering again`() {
        val info = findInfo(
            contacts = listOf(contact(id = 42L, displayName = "Jane Doe", groupIds = listOf(1))),
            assignments = linkedMapOf(42L to listOf(1, 2)),
            groupNames = mapOf(
                1 to "Family",
                2 to "Work"
            ),
            ringtoneGroupIds = setOf(2),
            hasSeen = true
        )

        assertNull(info)
    }

    @Test
    fun `unchanged assignment does not trigger info`() {
        val info = findInfo(
            contacts = listOf(
                contact(id = 42L, displayName = "Jane Doe", groupIds = listOf(2, 1))
            ),
            assignments = linkedMapOf(42L to listOf(1, 2)),
            groupNames = mapOf(
                1 to "Family",
                2 to "Work"
            ),
            ringtoneGroupIds = setOf(2)
        )

        assertNull(info)
    }

    @Test
    fun `assignment without effective ringtone does not trigger info`() {
        val info = findInfo(
            contacts = listOf(contact(id = 42L, displayName = "Jane Doe", groupIds = listOf(1))),
            assignments = linkedMapOf(42L to listOf(1, 2)),
            groupNames = mapOf(
                1 to "Family",
                2 to "Work"
            ),
            ringtoneGroupIds = emptySet()
        )

        assertNull(info)
    }

    private fun findInfo(
        contacts: List<Contact>,
        assignments: Map<Long, List<Int>>,
        groupNames: Map<Int, String>,
        ringtoneGroupIds: Set<Int>,
        hasSeen: Boolean = false
    ): MultipleGroupsRingtoneInfo? {
        return findMultipleGroupsRingtoneInfoAfterAssignments(
            contacts = contacts,
            assignedEditableGroupIdsByContact = assignments,
            hasSeenMultipleGroupsRingtoneInfo = hasSeen,
            isMembershipEditable = { groupId -> groupId in groupNames },
            groupName = groupNames::get,
            hasRingtone = ringtoneGroupIds::contains
        )
    }

    private fun contact(
        id: Long,
        displayName: String,
        groupIds: List<Int>
    ): Contact {
        return Contact(
            id = id,
            displayName = displayName,
            photoUri = null,
            thumbnailUri = null,
            customRingtone = null,
            groupIds = groupIds
        )
    }
}
