package de.drvlabs.contactgrouper.groups

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class EditableMembershipUpdatePlanTest {

    @Test
    fun `unchanged selection produces no membership updates`() {
        val editableGroup = Group(id = 1, name = "Family", color = Color.Red)
        val currentMemberships = listOf(
            GroupMembership(groupId = 1, contactId = 42L, assignedAt = 100L)
        )

        val plan = planEditableMembershipUpdate(
            currentMemberships = currentMemberships,
            groupsById = mapOf(editableGroup.id to editableGroup),
            selectedGroupIds = setOf(editableGroup.id)
        )

        assertEquals(emptyList<Int>(), plan.groupIdsToAdd)
        assertEquals(emptyList<GroupMembership>(), plan.membershipsToRemove)
    }

    @Test
    fun `removing all editable groups keeps read only memberships intact`() {
        val editableGroup = Group(id = 1, name = "Family", color = Color.Red)
        val readOnlyGroup = Group(
            id = 2,
            name = "Directory",
            color = Color.Blue,
            syncSource = GroupSyncSource.DEVICE,
            isReadOnly = true
        )
        val editableMembership = GroupMembership(groupId = 1, contactId = 42L, assignedAt = 100L)
        val readOnlyMembership = GroupMembership(
            groupId = 2,
            contactId = 42L,
            assignedAt = 200L,
            source = GroupSyncSource.DEVICE
        )

        val plan = planEditableMembershipUpdate(
            currentMemberships = listOf(editableMembership, readOnlyMembership),
            groupsById = mapOf(
                editableGroup.id to editableGroup,
                readOnlyGroup.id to readOnlyGroup
            ),
            selectedGroupIds = emptySet()
        )

        assertEquals(emptyList<Int>(), plan.groupIdsToAdd)
        assertEquals(listOf(editableMembership), plan.membershipsToRemove)
    }

    @Test
    fun `adding one editable group only schedules that membership`() {
        val family = Group(id = 1, name = "Family", color = Color.Red)
        val work = Group(id = 2, name = "Work", color = Color.Blue)
        val currentMembership = GroupMembership(groupId = 1, contactId = 42L, assignedAt = 100L)

        val plan = planEditableMembershipUpdate(
            currentMemberships = listOf(currentMembership),
            groupsById = mapOf(
                family.id to family,
                work.id to work
            ),
            selectedGroupIds = setOf(family.id, work.id)
        )

        assertEquals(listOf(work.id), plan.groupIdsToAdd)
        assertEquals(emptyList<GroupMembership>(), plan.membershipsToRemove)
    }
}
