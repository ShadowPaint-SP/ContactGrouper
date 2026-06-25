package de.drvlabs.contactgrouper.contacts

import androidx.compose.ui.graphics.Color
import de.drvlabs.contactgrouper.groups.Group
import org.junit.Assert.assertEquals
import org.junit.Test

class BulkGroupMembershipSelectionTest {

    @Test
    fun `initial states reflect none partial and full selected memberships`() {
        val family = Group(id = 1, name = "Family", color = Color.Red)
        val work = Group(id = 2, name = "Work", color = Color.Blue)
        val friends = Group(id = 3, name = "Friends", color = Color.Green)
        val contacts = listOf(
            contact(id = 10L, groupIds = listOf(family.id, work.id)),
            contact(id = 20L, groupIds = listOf(work.id))
        )

        val states = initialBulkGroupMembershipStates(
            selectedContacts = contacts,
            editableGroups = listOf(family, work, friends)
        )

        assertEquals(BulkGroupMembershipState.Partial, states[family.id])
        assertEquals(BulkGroupMembershipState.Selected, states[work.id])
        assertEquals(BulkGroupMembershipState.Unselected, states[friends.id])
    }

    @Test
    fun `state click cycle moves partial to unchecked and unchecked to selected`() {
        assertEquals(
            BulkGroupMembershipState.Unselected,
            nextBulkGroupMembershipState(BulkGroupMembershipState.Partial)
        )
        assertEquals(
            BulkGroupMembershipState.Selected,
            nextBulkGroupMembershipState(BulkGroupMembershipState.Unselected)
        )
        assertEquals(
            BulkGroupMembershipState.Unselected,
            nextBulkGroupMembershipState(BulkGroupMembershipState.Selected)
        )
    }

    @Test
    fun `final selections apply checked to all unchecked to none and preserve partial per contact`() {
        val family = Group(id = 1, name = "Family", color = Color.Red)
        val work = Group(id = 2, name = "Work", color = Color.Blue)
        val friends = Group(id = 3, name = "Friends", color = Color.Green)
        val contacts = listOf(
            contact(id = 10L, groupIds = listOf(family.id, work.id)),
            contact(id = 20L, groupIds = listOf(work.id))
        )

        val selections = buildBulkContactGroupSelections(
            selectedContacts = contacts,
            editableGroups = listOf(family, work, friends),
            groupStates = mapOf(
                family.id to BulkGroupMembershipState.Partial,
                work.id to BulkGroupMembershipState.Unselected,
                friends.id to BulkGroupMembershipState.Selected
            )
        )

        assertEquals(listOf(family.id, friends.id), selections[10L])
        assertEquals(listOf(friends.id), selections[20L])
    }

    private fun contact(id: Long, groupIds: List<Int>): Contact {
        return Contact(
            id = id,
            displayName = "Contact $id",
            photoUri = null,
            thumbnailUri = null,
            customRingtone = null,
            groupIds = groupIds
        )
    }
}
