package de.drvlabs.contactgrouper.groups

import androidx.compose.ui.graphics.Color
import de.drvlabs.contactgrouper.R
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupDeletionConfirmationTest {

    @Test
    fun `local-only group warns only about app removal`() {
        val confirmation = buildGroupDeletionConfirmation(
            Group(
                id = 1,
                name = "Friends",
                color = Color.Red
            )
        )

        assertEquals(R.string.delete_group_title, confirmation.titleResId)
        assertEquals(R.string.delete_group_message, confirmation.messageResId)
        assertEquals(R.string.groups_delete, confirmation.confirmLabelResId)
    }

    @Test
    fun `mirrored group warns about device-wide deletion`() {
        val confirmation = buildGroupDeletionConfirmation(
            Group(
                id = 2,
                name = "Work",
                color = Color.Blue,
                deviceGroupId = 99L
            )
        )

        assertEquals(R.string.delete_group_from_device_title, confirmation.titleResId)
        assertEquals(R.string.delete_group_from_device_message, confirmation.messageResId)
        assertEquals(R.string.delete_group_everywhere, confirmation.confirmLabelResId)
    }

    @Test
    fun `bulk delete warns when some selected groups are read-only`() {
        val confirmation = buildGroupDeletionConfirmation(
            listOf(
                Group(
                    id = 1,
                    name = "Friends",
                    color = Color.Red
                ),
                Group(
                    id = 2,
                    name = "Imported",
                    color = Color.Blue,
                    syncSource = GroupSyncSource.DEVICE,
                    isReadOnly = true
                )
            )
        )

        assertEquals(R.string.delete_selected_groups_title, confirmation.titleResId)
        assertEquals(R.plurals.delete_selected_groups_message, confirmation.messagePluralResId)
        assertEquals(R.string.delete_eligible, confirmation.confirmLabelResId)
        assertEquals(1, confirmation.deletableCount)
        assertEquals(1, confirmation.blockedCount)
    }

    @Test
    fun `bulk delete warns when any eligible group deletes from device`() {
        val confirmation = buildGroupDeletionConfirmation(
            listOf(
                Group(
                    id = 1,
                    name = "Friends",
                    color = Color.Red
                ),
                Group(
                    id = 2,
                    name = "Work",
                    color = Color.Blue,
                    deviceGroupId = 99L
                )
            )
        )

        assertEquals(R.string.delete_selected_groups_from_device_title, confirmation.titleResId)
        assertEquals(
            R.plurals.delete_selected_groups_from_device_message,
            confirmation.messagePluralResId
        )
        assertEquals(R.string.delete_eligible, confirmation.confirmLabelResId)
        assertEquals(2, confirmation.deletableCount)
        assertEquals(0, confirmation.blockedCount)
    }
}
