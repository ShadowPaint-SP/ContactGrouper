package de.drvlabs.contactgrouper.groups

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import de.drvlabs.contactgrouper.contacts.ContactsListState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GroupDetailScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun deviceBackedGroupDeleteShowsDeviceWideWarningAndCancelDoesNotDelete() {
        var deleteCalls = 0

        composeRule.setContent {
            GroupDetailScreen(
                navController = rememberNavController(),
                groupId = 7,
                contactState = ContactsListState(),
                groupState = GroupsListState(
                    groups = listOf(
                        Group(
                            id = 7,
                            name = "Work",
                            color = Color.Red,
                            deviceGroupId = 88L
                        )
                    )
                ),
                onChangeRingtone = { GroupMutationResult.Success },
                onDeleteGroup = {
                    deleteCalls += 1
                    GroupMutationResult.Success
                }
            )
        }

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Delete Group From Device").performClick()

        composeRule.onNodeWithText("Delete Group From Device?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Deleting this group will remove it from this app and from the device's contact groups."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onAllNodesWithText("Delete Group From Device?").assertCountEquals(0)
        assertEquals(0, deleteCalls)
    }

    @Test
    fun localOnlyGroupDeleteUsesLocalWarningAndConfirmsDeletion() {
        var deleteCalls = 0

        composeRule.setContent {
            GroupDetailScreen(
                navController = rememberNavController(),
                groupId = 3,
                contactState = ContactsListState(),
                groupState = GroupsListState(
                    groups = listOf(
                        Group(
                            id = 3,
                            name = "Family",
                            color = Color.Blue
                        )
                    )
                ),
                onChangeRingtone = { GroupMutationResult.Success },
                onDeleteGroup = {
                    deleteCalls += 1
                    GroupMutationResult.Success
                }
            )
        }

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Delete Group").performClick()

        composeRule.onNodeWithText("Delete Group?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Deleting this group will remove it from this app. Contacts will stay on your device."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Delete Group").performClick()
        assertEquals(1, deleteCalls)
    }
}
