package de.drvlabs.contactgrouper.contacts

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupMutationResult
import de.drvlabs.contactgrouper.groups.GroupSyncSource
import de.drvlabs.contactgrouper.groups.GroupsListState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContactDetailScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun manageGroupsDialogReflectsEditableStateAndAllowsClearingAll() {
        var savedGroupIds: List<Int>? = null

        composeRule.setContent {
            ContactDetailScreen(
                navController = rememberNavController(),
                contactId = 42L,
                contactState = ContactsListState(
                    contacts = listOf(
                        Contact(
                            id = 42L,
                            displayName = "Jane Doe",
                            photoUri = null,
                            thumbnailUri = null,
                            customRingtone = null,
                            groupIds = listOf(1, 3)
                        )
                    )
                ),
                groupState = GroupsListState(
                    groups = listOf(
                        Group(id = 1, name = "Family", color = Color.Red),
                        Group(id = 2, name = "Work", color = Color.Blue),
                        Group(
                            id = 3,
                            name = "Read Only",
                            color = Color.Green,
                            syncSource = GroupSyncSource.DEVICE,
                            isReadOnly = true
                        )
                    )
                ),
                onSaveGroups = { groupIds ->
                    savedGroupIds = groupIds
                    GroupMutationResult.Success
                },
                onRemoveGroup = { GroupMutationResult.Success },
                onEditContact = {},
                onDeleteContact = { true }
            )
        }

        composeRule.onAllNodesWithText("Controls ringtone").assertCountEquals(0)
        composeRule.onAllNodesWithText("Imported from device contacts").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Remove from group").assertCountEquals(1)

        composeRule.onAllNodesWithContentDescription("Manage groups").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Manage groups").performClick()

        composeRule.onNodeWithTag("manage-group-checkbox-1").assertIsOn()
        composeRule.onNodeWithTag("manage-group-checkbox-2").assertIsOff()
        composeRule.onAllNodesWithText("Read Only").assertCountEquals(1)
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNodeWithTag("manage-group-checkbox-1").performClick()
        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { savedGroupIds != null }
        assertEquals(emptyList<Int>(), savedGroupIds)
        composeRule.onAllNodesWithText("Manage Groups").assertCountEquals(0)
    }

    @Test
    fun manageGroupsDialogSavesFullEditableSelection() {
        var savedGroupIds: List<Int>? = null

        composeRule.setContent {
            ContactDetailScreen(
                navController = rememberNavController(),
                contactId = 42L,
                contactState = ContactsListState(
                    contacts = listOf(
                        Contact(
                            id = 42L,
                            displayName = "Jane Doe",
                            photoUri = null,
                            thumbnailUri = null,
                            customRingtone = null,
                            groupIds = listOf(2)
                        )
                    )
                ),
                groupState = GroupsListState(
                    groups = listOf(
                        Group(id = 2, name = "Work", color = Color.Blue),
                        Group(id = 1, name = "Family", color = Color.Red)
                    )
                ),
                onSaveGroups = { groupIds ->
                    savedGroupIds = groupIds
                    GroupMutationResult.Success
                },
                onRemoveGroup = { GroupMutationResult.Success },
                onEditContact = {},
                onDeleteContact = { true }
            )
        }

        composeRule.onNodeWithContentDescription("Manage groups").performClick()
        composeRule.onNodeWithTag("manage-group-checkbox-2").assertIsOn()
        composeRule.onNodeWithTag("manage-group-checkbox-1").assertIsOff()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNodeWithTag("manage-group-checkbox-1").performClick()
        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { savedGroupIds != null }
        assertEquals(listOf(1, 2), savedGroupIds)
    }

    @Test
    fun effectiveDisplayNameDrivesHeaderAndConfirmationWhilePersonalShowsProviderName() {
        composeRule.setContent {
            ContactDetailScreen(
                navController = rememberNavController(),
                contactId = 42L,
                contactState = ContactsListState(
                    contacts = listOf(
                        Contact(
                            id = 42L,
                            displayName = "Bobby",
                            providerDisplayName = "Robert Smith",
                            photoUri = null,
                            thumbnailUri = null,
                            customRingtone = null,
                            nickname = "Bobby"
                        )
                    )
                ),
                groupState = GroupsListState(),
                onSaveGroups = { GroupMutationResult.Success },
                onRemoveGroup = { GroupMutationResult.Success },
                onEditContact = {},
                onDeleteContact = { true }
            )
        }

        composeRule.onAllNodesWithText("Bobby").assertCountEquals(2)
        composeRule.onAllNodesWithText("Robert Smith").assertCountEquals(1)
        composeRule.onAllNodesWithText("Display name").assertCountEquals(1)

        composeRule.onNodeWithContentDescription("Delete contact").performClick()
        composeRule.onAllNodesWithText("This removes Bobby from device contacts.").assertCountEquals(1)
    }
}
