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
    fun `next older membership with ringtone wins when newest loses ringtone`() {
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
    fun `multiple fallbacks keep walking down the membership list`() {
        val winner = RingtoneResolution.resolveWinningMembershipId(
            memberships = listOf(
                GroupMembership(groupId = 1, contactId = 99L, assignedAt = 30L),
                GroupMembership(groupId = 2, contactId = 99L, assignedAt = 20L),
                GroupMembership(groupId = 3, contactId = 99L, assignedAt = 10L)
            ),
            hasRingtone = { groupId -> groupId == 3 }
        )

        assertEquals(3, winner)
    }

    @Test
    fun `no eligible ringtone source returns no winner`() {
        val winner = RingtoneResolution.resolveWinningMembershipId(
            memberships = listOf(
                GroupMembership(groupId = 1, contactId = 99L, assignedAt = 50L)
            ),
            hasRingtone = { false }
        )

        assertNull(winner)
    }

    @Test
    fun `primary membership still tracks newest group order`() {
        val winner = RingtoneResolution.resolvePrimaryMembershipId(
            listOf(
                GroupMembership(groupId = 1, contactId = 99L, assignedAt = 5L),
                GroupMembership(groupId = 2, contactId = 99L, assignedAt = 10L)
            )
        )

        assertEquals(2, winner)
    }
}
