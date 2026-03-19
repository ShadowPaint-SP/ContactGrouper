package de.drvlabs.contactgrouper.groups

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    @Test
    fun `invalid sync source falls back to local`() {
        val converters = Converters()

        assertEquals(GroupSyncSource.LOCAL, converters.stringToGroupSyncSource("BROKEN"))
    }
}
