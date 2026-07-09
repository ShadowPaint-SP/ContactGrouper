package de.drvlabs.contactgrouper.contacts

import androidx.compose.ui.graphics.Color
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupMembership
import de.drvlabs.contactgrouper.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsStateMapperTest {

    @Test
    fun `initial install without groups keeps imported contacts unassigned`() {
        val contacts = listOf(
            contact(id = 10L, displayName = "Alex Kim"),
            contact(id = 20L, displayName = "Jordan Lee")
        )

        val state = ContactsStateMapper.build(
            contacts = contacts,
            groups = emptyList(),
            memberships = emptyList()
        )

        assertEquals(listOf(10L, 20L), state.contacts.map(Contact::id))
        assertTrue(state.contacts.all { it.groupIds.isEmpty() })
        assertTrue(state.contacts.all { it.effectiveRingtoneGroupId == null })
    }

    @Test
    fun `contacts with the same display name keep separate memberships`() {
        val family = group(id = 1, name = "Family")
        val duplicateName = "Sam Taylor"

        val state = ContactsStateMapper.build(
            contacts = listOf(
                contact(id = 1L, displayName = duplicateName),
                contact(id = 2L, displayName = duplicateName)
            ),
            groups = listOf(family),
            memberships = listOf(
                GroupMembership(groupId = family.id, contactId = 2L, assignedAt = 100L)
            )
        )

        val firstSam = state.contacts.first { it.id == 1L }
        val secondSam = state.contacts.first { it.id == 2L }

        assertEquals(2, state.contacts.count { it.displayName == duplicateName })
        assertTrue(firstSam.groupIds.isEmpty())
        assertNull(firstSam.effectiveRingtoneGroupId)
        assertEquals(listOf(family.id), secondSam.groupIds)
        assertNull(secondSam.effectiveRingtoneGroupId)
    }

    @Test
    fun `already assigned contacts keep newest group order`() {
        val silentNewest = group(id = 2, name = "Recent")
        val ringtoneFallback = group(id = 3, name = "Family")
        val olderRingtone = group(id = 1, name = "Work")

        val state = ContactsStateMapper.build(
            contacts = listOf(contact(id = 99L, displayName = "Casey Morgan")),
            groups = listOf(olderRingtone, silentNewest, ringtoneFallback),
            memberships = listOf(
                GroupMembership(groupId = silentNewest.id, contactId = 99L, assignedAt = 300L),
                GroupMembership(groupId = ringtoneFallback.id, contactId = 99L, assignedAt = 250L),
                GroupMembership(groupId = olderRingtone.id, contactId = 99L, assignedAt = 200L)
            )
        )

        val contact = state.contacts.single()
        assertEquals(listOf(2, 3, 1), contact.groupIds)
        assertNull(contact.effectiveRingtoneGroupId)
    }

    @Test
    fun `prefer nickname setting uses nonblank nickname as display name`() {
        val state = ContactsStateMapper.build(
            contacts = listOf(
                contact(
                    id = 42L,
                    displayName = "Robert Smith",
                    nickname = "Bobby"
                )
            ),
            groups = emptyList(),
            memberships = emptyList(),
            settings = AppSettings(preferNicknameDisplayName = true)
        )

        val contact = state.contacts.single()
        assertEquals("Bobby", contact.displayName)
        assertEquals("Robert Smith", contact.providerDisplayName)
        assertEquals("Bobby", contact.nickname)
    }

    @Test
    fun `prefer nickname setting falls back to provider display name for blank nickname`() {
        val state = ContactsStateMapper.build(
            contacts = listOf(
                contact(
                    id = 42L,
                    displayName = "Robert Smith",
                    nickname = "   "
                )
            ),
            groups = emptyList(),
            memberships = emptyList(),
            settings = AppSettings(preferNicknameDisplayName = true)
        )

        val contact = state.contacts.single()
        assertEquals("Robert Smith", contact.displayName)
        assertEquals("Robert Smith", contact.providerDisplayName)
    }

    @Test
    fun `disabled prefer nickname setting keeps provider display name`() {
        val state = ContactsStateMapper.build(
            contacts = listOf(
                contact(
                    id = 42L,
                    displayName = "Robert Smith",
                    nickname = "Bobby"
                )
            ),
            groups = emptyList(),
            memberships = emptyList(),
            settings = AppSettings(preferNicknameDisplayName = false)
        )

        val contact = state.contacts.single()
        assertEquals("Robert Smith", contact.displayName)
        assertEquals("Robert Smith", contact.providerDisplayName)
        assertEquals("Bobby", contact.nickname)
    }

    @Test
    fun `large imports keep every contact and assigned state stable`() {
        val localRingtone = group(id = 1, name = "Family")
        val silentGroup = group(id = 2, name = "Work")
        val newestRingtone = group(id = 3, name = "VIP")
        val contacts = (1L..750L).map { id ->
            contact(
                id = id,
                displayName = "Shared Name ${id % 7}"
            )
        }
        val memberships = buildList {
            contacts.forEach { contact ->
                if (contact.id % 3L == 0L) {
                    add(GroupMembership(groupId = 1, contactId = contact.id, assignedAt = contact.id * 10 + 1))
                }
                if (contact.id % 2L == 0L) {
                    add(GroupMembership(groupId = 2, contactId = contact.id, assignedAt = contact.id * 10 + 2))
                }
                if (contact.id % 5L == 0L) {
                    add(GroupMembership(groupId = 3, contactId = contact.id, assignedAt = contact.id * 10 + 3))
                }
            }
        }

        val state = ContactsStateMapper.build(
            contacts = contacts,
            groups = listOf(localRingtone, silentGroup, newestRingtone),
            memberships = memberships
        )

        assertEquals(750, state.contacts.size)
        assertEquals((1L..750L).toList(), state.contacts.map(Contact::id))
        assertEquals(108, state.contacts.count { it.displayName == "Shared Name 1" })
        assertEquals(
            contacts.count { it.id % 2L == 0L || it.id % 3L == 0L || it.id % 5L == 0L },
            state.contacts.count { it.groupIds.isNotEmpty() }
        )

        val contact6 = state.contacts.first { it.id == 6L }
        assertEquals(listOf(2, 1), contact6.groupIds)
        assertNull(contact6.effectiveRingtoneGroupId)

        val contact14 = state.contacts.first { it.id == 14L }
        assertEquals(listOf(2), contact14.groupIds)
        assertNull(contact14.effectiveRingtoneGroupId)

        val contact30 = state.contacts.first { it.id == 30L }
        assertEquals(listOf(3, 2, 1), contact30.groupIds)
        assertNull(contact30.effectiveRingtoneGroupId)
    }

    private fun contact(
        id: Long,
        displayName: String,
        nickname: String? = null
    ): Contact {
        return Contact(
            id = id,
            displayName = displayName,
            photoUri = null,
            thumbnailUri = null,
            customRingtone = null,
            nickname = nickname
        )
    }

    private fun group(
        id: Int,
        name: String
    ): Group {
        return Group(
            id = id,
            name = name,
            color = Color.Red
        )
    }
}
