package de.drvlabs.contactgrouper.groups

import androidx.compose.ui.graphics.Color
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

        assertEquals("Delete Group?", confirmation.title)
        assertEquals(
            "Deleting this group will remove it from this app. Contacts will stay on your device.",
            confirmation.message
        )
        assertEquals("Delete Group", confirmation.confirmLabel)
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

        assertEquals("Delete Group From Device?", confirmation.title)
        assertEquals(
            "Deleting this group will remove it from this app and from the device's contact groups.",
            confirmation.message
        )
        assertEquals("Delete Everywhere", confirmation.confirmLabel)
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

        assertEquals("Delete Selected Groups?", confirmation.title)
        assertEquals(
            "Deleting 1 group will remove it from this app. Contacts will stay on your device.\n\n1 selected group is read-only and will not be deleted.",
            confirmation.message
        )
        assertEquals("Delete Eligible", confirmation.confirmLabel)
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

        assertEquals("Delete Selected Groups From Device?", confirmation.title)
        assertEquals(
            "Deleting 2 groups will remove them from this app and from the device's contact groups.",
            confirmation.message
        )
        assertEquals("Delete Eligible", confirmation.confirmLabel)
    }
}
