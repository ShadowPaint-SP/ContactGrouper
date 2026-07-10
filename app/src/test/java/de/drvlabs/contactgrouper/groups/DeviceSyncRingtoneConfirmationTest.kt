package de.drvlabs.contactgrouper.groups

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceSyncRingtoneConfirmationTest {

    @Test
    fun `preview counts ringtone contacts and involved groups`() {
        val preview = DeviceSyncRingtonePreview.calculate(
            contactIds = setOf(10L, 11L, 12L, 13L),
            membershipsByContact = mapOf(
                10L to listOf(
                    GroupMembership(
                        groupId = 1,
                        contactId = 10L,
                        assignedAt = 10L,
                        source = GroupSyncSource.DEVICE
                    )
                ),
                11L to listOf(
                    GroupMembership(
                        groupId = 1,
                        contactId = 11L,
                        assignedAt = 10L,
                        source = GroupSyncSource.DEVICE
                    ),
                    GroupMembership(
                        groupId = 2,
                        contactId = 11L,
                        assignedAt = 20L,
                        source = GroupSyncSource.DEVICE
                    )
                ),
                13L to listOf(
                    GroupMembership(
                        groupId = 2,
                        contactId = 13L,
                        assignedAt = 20L,
                        source = GroupSyncSource.DEVICE
                    )
                )
            ),
            groupsById = listOf(
                ringtoneGroup(id = 1),
                ringtoneGroup(id = 2)
            ).associateBy(Group::id),
            existingStatesByContact = mapOf(
                11L to ContactRingtoneState(
                    contactId = 11L,
                    baselineRingtoneUri = "baseline",
                    lastAppliedGroupId = 1,
                    lastAppliedRingtoneUri = "ringtone-1"
                ),
                12L to ContactRingtoneState(
                    contactId = 12L,
                    baselineRingtoneUri = "baseline",
                    lastAppliedGroupId = 1,
                    lastAppliedRingtoneUri = "ringtone-1"
                ),
                13L to ContactRingtoneState(
                    contactId = 13L,
                    baselineRingtoneUri = "baseline",
                    lastAppliedGroupId = 2,
                    lastAppliedRingtoneUri = "ringtone-2"
                )
            ),
            ringtoneUriForGroup = { group -> "ringtone-${group.id}" }
        )

        checkNotNull(preview)
        assertEquals(setOf(10L, 11L, 12L), preview.contactIds)
        assertEquals(setOf(1, 2), preview.groupIds)
        assertEquals(3, preview.ringtoneCount)
        assertEquals(3, preview.contactCount)
        assertEquals(2, preview.groupCount)
    }

    @Test
    fun `preview is empty when no contact ringtone would change`() {
        val preview = DeviceSyncRingtonePreview.calculate(
            contactIds = setOf(20L),
            membershipsByContact = mapOf(
                20L to listOf(
                    GroupMembership(
                        groupId = 3,
                        contactId = 20L,
                        assignedAt = 10L,
                        source = GroupSyncSource.DEVICE
                    )
                )
            ),
            groupsById = mapOf(3 to ringtoneGroup(id = 3)),
            existingStatesByContact = mapOf(
                20L to ContactRingtoneState(
                    contactId = 20L,
                    baselineRingtoneUri = "baseline",
                    lastAppliedGroupId = 3,
                    lastAppliedRingtoneUri = "ringtone-3"
                )
            ),
            ringtoneUriForGroup = { group -> "ringtone-${group.id}" }
        )

        assertNull(preview)
    }

    @Test
    fun `cancel leaves pending ringtone changes unapplied and allows later prompt`() = runBlocking {
        val confirmation = DeviceSyncRingtoneConfirmation(
            contactIds = setOf(30L),
            groupIds = setOf(4)
        )
        val appliedContacts = mutableListOf<Set<Long>>()
        val controller = DeviceSyncRingtoneConfirmationController(
            loadPendingPreview = { confirmation },
            applyRingtoneChanges = { contactIds ->
                appliedContacts += contactIds
                true
            }
        )

        assertEquals(
            GroupMutationResult.Success,
            controller.handleDeviceSync(DeviceSyncRingtoneMode.RequireConfirmation)
        )
        assertEquals(confirmation, controller.pendingConfirmation.value)

        controller.cancelPending()

        assertNull(controller.pendingConfirmation.value)
        assertEquals(emptyList<Set<Long>>(), appliedContacts)

        assertEquals(
            GroupMutationResult.Success,
            controller.handleDeviceSync(DeviceSyncRingtoneMode.RequireConfirmation)
        )
        assertEquals(confirmation, controller.pendingConfirmation.value)
    }

    @Test
    fun `accept applies pending ringtone changes and clears prompt`() = runBlocking {
        val confirmation = DeviceSyncRingtoneConfirmation(
            contactIds = setOf(40L, 41L),
            groupIds = setOf(5)
        )
        val appliedContacts = mutableListOf<Set<Long>>()
        val controller = DeviceSyncRingtoneConfirmationController(
            loadPendingPreview = { confirmation },
            applyRingtoneChanges = { contactIds ->
                appliedContacts += contactIds
                true
            }
        )

        controller.handleDeviceSync(DeviceSyncRingtoneMode.RequireConfirmation)
        val result = controller.acceptPending()

        assertEquals(GroupMutationResult.Success, result)
        assertNull(controller.pendingConfirmation.value)
        assertEquals(listOf(setOf(40L, 41L)), appliedContacts)
    }

    @Test
    fun `auto sync applies ringtone changes without prompting`() = runBlocking {
        val confirmation = DeviceSyncRingtoneConfirmation(
            contactIds = setOf(50L),
            groupIds = setOf(6)
        )
        val appliedContacts = mutableListOf<Set<Long>>()
        val controller = DeviceSyncRingtoneConfirmationController(
            loadPendingPreview = { confirmation },
            applyRingtoneChanges = { contactIds ->
                appliedContacts += contactIds
                true
            }
        )

        val result = controller.handleDeviceSync(DeviceSyncRingtoneMode.ApplyImmediately)

        assertEquals(GroupMutationResult.Success, result)
        assertNull(controller.pendingConfirmation.value)
        assertEquals(listOf(setOf(50L)), appliedContacts)
    }

    private fun ringtoneGroup(id: Int): Group {
        return Group(
            id = id,
            name = "Group $id",
            color = Color.Red
        )
    }
}
