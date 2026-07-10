package de.drvlabs.contactgrouper.search

import androidx.compose.ui.graphics.Color
import de.drvlabs.contactgrouper.contacts.Contact
import de.drvlabs.contactgrouper.contacts.ContactDataItem
import de.drvlabs.contactgrouper.groups.Group
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchFilteringTest {

    @Test
    fun `blank contact query keeps original contacts`() {
        val contacts = listOf(
            contact(id = 1L, displayName = "Alex Kim"),
            contact(id = 2L, displayName = "Jordan Lee")
        )

        val filtered = filterContactsBySearchQuery(
            contacts = contacts,
            groups = emptyList(),
            query = "   "
        )

        assertEquals(contacts, filtered)
    }

    @Test
    fun `contacts match display name nickname phone email and group name`() {
        val family = group(id = 10, name = "Family")
        val work = group(id = 20, name = "Work")
        val contacts = listOf(
            contact(
                id = 1L,
                displayName = "Alex Kim",
                nickname = "AK",
                phoneNumbers = listOf(ContactDataItem("+1 (555) 010-2222", typeConstant = 0)),
                emails = listOf(ContactDataItem("alex.kim@example.com", typeConstant = 0)),
                groupIds = listOf(family.id)
            ),
            contact(
                id = 2L,
                displayName = "Jordan Lee",
                nickname = "Jay",
                phoneNumbers = listOf(ContactDataItem("030 987654", typeConstant = 0)),
                emails = listOf(ContactDataItem("jordan@work.test", typeConstant = 0)),
                groupIds = listOf(work.id)
            )
        )
        val groups = listOf(family, work)

        assertEquals(listOf(1L), filterContactsBySearchQuery(contacts, groups, "alex").contactIds())
        assertEquals(listOf(1L), filterContactsBySearchQuery(contacts, groups, "ak").contactIds())
        assertEquals(
            listOf(1L),
            filterContactsBySearchQuery(contacts, groups, "5550102222").contactIds()
        )
        assertEquals(
            listOf(1L),
            filterContactsBySearchQuery(contacts, groups, "alex@example").contactIds()
        )
        assertEquals(listOf(1L), filterContactsBySearchQuery(contacts, groups, "family").contactIds())
    }

    @Test
    fun `contact query terms can match across indexed fields`() {
        val family = group(id = 10, name = "Family")
        val contacts = listOf(
            contact(id = 1L, displayName = "Alex Kim", groupIds = listOf(family.id)),
            contact(id = 2L, displayName = "Alex Lee")
        )

        val filtered = filterContactsBySearchQuery(
            contacts = contacts,
            groups = listOf(family),
            query = "alex family"
        )

        assertEquals(listOf(1L), filtered.contactIds())
    }

    @Test
    fun `blank group query keeps original groups`() {
        val groups = listOf(
            group(id = 1, name = "Family"),
            group(id = 2, name = "Work")
        )

        val filtered = filterGroupsBySearchQuery(
            groups = groups,
            contacts = emptyList(),
            query = ""
        )

        assertEquals(groups, filtered)
    }

    @Test
    fun `groups match group name and loaded member names`() {
        val family = group(id = 1, name = "Family")
        val work = group(id = 2, name = "Work")
        val cycling = group(id = 3, name = "Cycling")
        val contacts = listOf(
            contact(id = 10L, displayName = "Mia Wong", nickname = "Cycler", groupIds = listOf(cycling.id)),
            contact(id = 20L, displayName = "Jordan Lee", groupIds = listOf(work.id))
        )
        val groups = listOf(family, work, cycling)

        assertEquals(listOf(work.id), filterGroupsBySearchQuery(groups, contacts, "work").groupIds())
        assertEquals(
            listOf(work.id),
            filterGroupsBySearchQuery(groups, contacts, "jordan").groupIds()
        )
        assertEquals(
            listOf(cycling.id),
            filterGroupsBySearchQuery(groups, contacts, "cycler").groupIds()
        )
        assertEquals(emptyList<Int>(), filterGroupsBySearchQuery(groups, contacts, "sam").groupIds())
    }

    private fun contact(
        id: Long,
        displayName: String,
        nickname: String? = null,
        phoneNumbers: List<ContactDataItem> = emptyList(),
        emails: List<ContactDataItem> = emptyList(),
        groupIds: List<Int> = emptyList()
    ): Contact {
        return Contact(
            id = id,
            displayName = displayName,
            photoUri = null,
            thumbnailUri = null,
            customRingtone = null,
            nickname = nickname,
            phoneNumbers = phoneNumbers,
            emails = emails,
            groupIds = groupIds
        )
    }

    private fun group(id: Int, name: String): Group {
        return Group(
            id = id,
            name = name,
            color = Color.Red
        )
    }

    private fun List<Contact>.contactIds(): List<Long> = map(Contact::id)

    private fun List<Group>.groupIds(): List<Int> = map(Group::id)
}
