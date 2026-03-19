package de.drvlabs.contactgrouper.groups

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceGroupWriteGatewayTest {

    @Test
    fun `account scoped group requires exact raw contact match`() {
        val rawContacts = listOf(
            ContactAccount(
                rawContactId = 10L,
                accountName = "personal@example.com",
                accountType = "com.google",
                dataSet = null
            )
        )
        val group = Group(
            id = 1,
            name = "Work",
            color = Color.Red,
            syncSource = GroupSyncSource.DEVICE,
            deviceGroupId = 50L,
            accountName = "work@example.com",
            accountType = "com.google"
        )

        assertNull(selectRawContactForGroup(rawContacts, group))
    }

    @Test
    fun `unscoped group falls back to first raw contact`() {
        val firstRawContact = ContactAccount(
            rawContactId = 10L,
            accountName = "personal@example.com",
            accountType = "com.google",
            dataSet = null
        )
        val rawContacts = listOf(
            firstRawContact,
            ContactAccount(
                rawContactId = 11L,
                accountName = "work@example.com",
                accountType = "com.google",
                dataSet = null
            )
        )
        val group = Group(
            id = 1,
            name = "Local only",
            color = Color.Red
        )

        assertEquals(firstRawContact, selectRawContactForGroup(rawContacts, group))
    }
}
