package de.drvlabs.contactgrouper.contacts

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test

class ContactsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun contactListUsesEffectiveDisplayNameForRowsAndGrouping() {
        composeRule.setContent {
            ContactList(
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
                ),
                onContactClick = {},
                onContactLongClick = {}
            )
        }

        composeRule.onAllNodesWithText("Bobby").assertCountEquals(1)
        composeRule.onAllNodesWithText("Robert Smith").assertCountEquals(0)
        composeRule.onAllNodesWithText("B").assertCountEquals(2)
    }
}
