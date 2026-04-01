package de.drvlabs.contactgrouper.contacts

import android.content.ContentResolver
import android.database.ContentObserver
import de.drvlabs.contactgrouper.AppErrorKind
import de.drvlabs.contactgrouper.AppErrorOrigin
import de.drvlabs.contactgrouper.AppErrorReporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsDataSourceTest {

    @Test
    fun `observeContacts reports startup fatal error and returns empty list when query throws`() =
        runBlocking {
            val reporter = AppErrorReporter()
            val dataSource = ContactsDataSource(
                contentResolver = object : ContentResolver(null) {},
                appErrorReporter = reporter,
                debounceMillis = 0L,
                contactsLoader = { throw IllegalStateException("contacts provider down") },
                contentObserverFactory = { onChange ->
                    object : ContentObserver(null) {
                        override fun onChange(selfChange: Boolean) {
                            super.onChange(selfChange)
                            onChange()
                        }
                    }
                },
                registerObservers = {},
                unregisterObservers = {}
            )

            val contacts = dataSource.observeContacts().first()
            val error = reporter.currentError.value

            assertEquals(emptyList<Contact>(), contacts)
            assertEquals(AppErrorKind.StartupFatal, error?.kind)
            assertEquals(AppErrorOrigin.ContactsImport, error?.origin)
            assertTrue(error?.technicalDetails?.contains("contacts provider down") == true)
        }

    @Test
    fun `resolveContactDisplayName falls back to display name when primary display name is null`() {
        assertEquals("Fallback Name", resolveContactDisplayName(null, "Fallback Name"))
    }

    @Test
    fun `resolveContactDisplayName uses placeholder when all display names are null or blank`() {
        assertEquals("Null Named", resolveContactDisplayName("   ", null))
    }
}
