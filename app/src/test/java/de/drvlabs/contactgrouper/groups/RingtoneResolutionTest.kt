package de.drvlabs.contactgrouper.groups

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RingtoneResolutionTest {

    @Test
    fun `newest membership with ringtone wins`() {
        val winner = RingtoneResolution.resolveWinningMembershipId(
            memberships = listOf(
                GroupMembership(groupId = 1, contactId = 99L, assignedAt = 5L),
                GroupMembership(groupId = 2, contactId = 99L, assignedAt = 10L)
            ),
            hasRingtone = { groupId -> groupId in setOf(1, 2) }
        )

        assertEquals(2, winner)
    }

    @Test
    fun `memberships without ringtone are skipped`() {
        val winner = RingtoneResolution.resolveWinningMembershipId(
            memberships = listOf(
                GroupMembership(groupId = 1, contactId = 99L, assignedAt = 50L),
                GroupMembership(groupId = 2, contactId = 99L, assignedAt = 10L)
            ),
            hasRingtone = { groupId -> groupId == 2 }
        )

        assertEquals(2, winner)
    }

    @Test
    fun `no ringtone returns no winner`() {
        val winner = RingtoneResolution.resolveWinningMembershipId(
            memberships = listOf(
                GroupMembership(groupId = 1, contactId = 99L, assignedAt = 50L)
            ),
            hasRingtone = { false }
        )

        assertNull(winner)
    }
}
