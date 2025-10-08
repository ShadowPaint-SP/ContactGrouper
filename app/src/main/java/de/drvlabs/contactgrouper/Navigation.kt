package de.drvlabs.contactgrouper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val selected: ImageVector, val unselected: ImageVector) {
    object Contacts : Screen("Contacts", Icons.Filled.Person, Icons.Outlined.Person)
    object ContactDetails : Screen("ContactDetails", Icons.Filled.Person, Icons.Outlined.Person)
    object Groups : Screen("Groups", Icons.Filled.Groups, Icons.Outlined.Groups)
    object AddGroup : Screen("AddGroup", Icons.Filled.Group, Icons.Outlined.Group)
    object GroupDetails : Screen("GroupDetails", Icons.Filled.Group, Icons.Outlined.Group)
}

val navbarItems = listOf(Screen.Contacts, Screen.Groups)

@Composable
fun BottomNavigation(navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        navbarItems.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        if (isSelected) screen.selected else screen.unselected,
                        contentDescription = screen.route
                    )
                },
                label = { Text(screen.route) },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}