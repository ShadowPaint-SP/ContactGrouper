package de.drvlabs.contactgrouper

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test

class MainActivityStartupErrorTest {

    companion object {
        init {
            MainActivity.bootstrapOverride = { _, _ ->
                throw IllegalStateException("forced bootstrap failure")
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            MainActivity.bootstrapOverride = null
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startupFailureShowsBlockingAppErrorDialog() {
        composeRule.onNodeWithText("App Failed to Start").assertIsDisplayed()
        composeRule.onNodeWithText("The app hit an unexpected error during startup.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Technical details").assertIsDisplayed()
        composeRule.onNodeWithText("Close app").assertIsDisplayed()
    }
}
