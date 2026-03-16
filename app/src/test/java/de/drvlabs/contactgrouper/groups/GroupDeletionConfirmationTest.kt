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
}
