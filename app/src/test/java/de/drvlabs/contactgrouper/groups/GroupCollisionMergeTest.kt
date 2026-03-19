package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupCollisionMergeTest {

    @Test
    fun `collision merge keeps target identity and carries local ringtone`() {
        val sourceGroup = Group(
            id = 1,
            name = "Family",
            color = Color.Red,
            ringtoneUri = Uri.EMPTY,
            syncSource = GroupSyncSource.LOCAL
        )
        val targetGroup = Group(
            id = 2,
            name = "Family",
            color = Color.Blue,
            syncSource = GroupSyncSource.DEVICE,
            deviceGroupId = 88L
        )

        val mergePlan = planGroupCollisionMerge(
            sourceGroup = sourceGroup,
            targetGroup = targetGroup,
            sourceMemberships = listOf(
                GroupMembership(groupId = 1, contactId = 10L, assignedAt = 100L),
                GroupMembership(groupId = 1, contactId = 20L, assignedAt = 150L)
            ),
            targetMemberships = listOf(
                GroupMembership(groupId = 2, contactId = 20L, assignedAt = 120L, source = GroupSyncSource.DEVICE),
                GroupMembership(groupId = 2, contactId = 30L, assignedAt = 130L, source = GroupSyncSource.DEVICE)
            )
        )

        assertEquals(2, mergePlan.survivingGroup.id)
        assertEquals(sourceGroup.ringtoneUri, mergePlan.survivingGroup.ringtoneUri)
        assertEquals(setOf(10L, 20L, 30L), mergePlan.affectedContactIds)

        val mergedExistingContact = mergePlan.mergedMemberships.first { it.contactId == 20L }
        assertEquals(2, mergedExistingContact.groupId)
        assertEquals(150L, mergedExistingContact.assignedAt)
        assertEquals(GroupSyncSource.DEVICE, mergedExistingContact.source)
    }

    @Test
    fun `collision merge preserves target ringtone when source has none`() {
        val sourceGroup = Group(
            id = 1,
            name = "Family",
            color = Color.Red,
            syncSource = GroupSyncSource.LOCAL
        )
        val targetGroup = Group(
            id = 2,
            name = "Family",
            color = Color.Blue,
            ringtoneUri = Uri.EMPTY,
            syncSource = GroupSyncSource.DEVICE,
            deviceGroupId = 88L
        )

        val mergePlan = planGroupCollisionMerge(
            sourceGroup = sourceGroup,
            targetGroup = targetGroup,
            sourceMemberships = emptyList(),
            targetMemberships = emptyList()
        )

        assertEquals(targetGroup.ringtoneUri, mergePlan.survivingGroup.ringtoneUri)
    }
}
