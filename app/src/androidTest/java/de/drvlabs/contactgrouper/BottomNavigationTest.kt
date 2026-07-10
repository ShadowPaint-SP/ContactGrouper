package de.drvlabs.contactgrouper

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import org.junit.Rule
import org.junit.Test

class BottomNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsItemNavigatesToSettingsGraph() {
        composeRule.setContent {
            val navController = rememberNavController()

            Column(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = "contacts_graph",
                    modifier = Modifier.weight(1f)
                ) {
                    navigation(
                        startDestination = Screen.NavBarScreen.Contacts.route,
                        route = "contacts_graph"
                    ) {
                        composable(Screen.NavBarScreen.Contacts.route) {
                            Text("Contacts destination")
                        }
                    }
                    navigation(
                        startDestination = Screen.NavBarScreen.Groups.route,
                        route = "groups_graph"
                    ) {
                        composable(Screen.NavBarScreen.Groups.route) {
                            Text("Groups destination")
                        }
                    }
                    navigation(
                        startDestination = Screen.NavBarScreen.Settings.route,
                        route = "settings_graph"
                    ) {
                        composable(Screen.NavBarScreen.Settings.route) {
                            Text("Settings destination")
                        }
                    }
                }
                BottomNavigation(navController = navController)
            }
        }

        composeRule.onNodeWithText("Contacts destination").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Settings destination").assertIsDisplayed()
    }
}
