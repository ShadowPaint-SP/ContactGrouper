package de.drvlabs.contactgrouper

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val selected: ImageVector, val unselected: ImageVector) {
    sealed class NavBarScreen(
        route: String,
        selected: ImageVector,
        unselected: ImageVector,
        val graphRoute: String,
        @StringRes val labelResId: Int
    ) : Screen(route, selected, unselected) {
        data object Contacts : NavBarScreen(
            "Contacts",
            Icons.Filled.Person,
            Icons.Outlined.Person,
            "contacts_graph",
            R.string.nav_contacts
        )
        data object Groups : NavBarScreen(
            "Groups",
            Icons.Filled.Groups,
            Icons.Outlined.Groups,
            "groups_graph",
            R.string.nav_groups
        )
    }

    data object AddGroup : Screen("AddGroup", Icons.Filled.Group, Icons.Outlined.Group)
    data object ContactDetails : Screen(
        "ContactDetails/{contactId}",
        Icons.Filled.Person,
        Icons.Outlined.Person
    ) {
        const val ARG_CONTACT_ID = "contactId"

        fun createRoute(contactId: Long): String = "ContactDetails/$contactId"
    }

    data object GroupDetails : Screen(
        "GroupDetails/{groupId}",
        Icons.Filled.Group,
        Icons.Outlined.Group
    ) {
        const val ARG_GROUP_ID = "groupId"

        fun createRoute(groupId: Int): String = "GroupDetails/$groupId"
    }
}

val navbarItems = listOf(Screen.NavBarScreen.Contacts, Screen.NavBarScreen.Groups)

@Composable
fun BottomNavigation(navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        navbarItems.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val label = stringResource(screen.labelResId)
            NavigationBarItem(
                icon = {
                    Icon(
                        if (isSelected) screen.selected else screen.unselected,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.graphRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
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
