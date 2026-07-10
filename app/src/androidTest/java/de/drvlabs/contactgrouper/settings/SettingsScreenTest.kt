package de.drvlabs.contactgrouper.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsScreenShowsAndTogglesVisibleSettings() {
        composeRule.setContent {
            var settings by remember { mutableStateOf(AppSettings()) }

            SettingsScreen(
                settings = settings,
                onPreferNicknameDisplayNameChange = {
                    settings = settings.copy(preferNicknameDisplayName = it)
                },
                onAutoSyncDeviceGroupChangesChange = {
                    settings = settings.copy(autoSyncDeviceGroupChanges = it)
                }
            )
        }

        composeRule.onNodeWithText("Settings").assertExists()
        composeRule.onNodeWithText("Use nickname as contact display name").assertExists()
        composeRule.onNodeWithText("Auto sync device group changes").assertExists()

        composeRule.onNodeWithTag(PREFER_NICKNAME_SETTING_TAG)
            .assertIsOff()
            .performClick()
            .assertIsOn()

        composeRule.onNodeWithTag(AUTO_SYNC_DEVICE_GROUPS_SETTING_TAG)
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }
}
