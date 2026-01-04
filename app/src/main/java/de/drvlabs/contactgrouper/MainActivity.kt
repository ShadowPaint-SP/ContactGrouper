package de.drvlabs.contactgrouper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.room.Room
import de.drvlabs.contactgrouper.contacts.ContactDetailScreen
import de.drvlabs.contactgrouper.groups.AddGroupScreen
import de.drvlabs.contactgrouper.groups.GroupDatabase
import de.drvlabs.contactgrouper.ui.theme.AppTheme
import de.drvlabs.contactgrouper.contacts.ContactsMainScreen
import de.drvlabs.contactgrouper.groups.GroupDetailScreen
import de.drvlabs.contactgrouper.groups.GroupsMainScreen
import de.drvlabs.contactgrouper.contacts.ContactsViewModel
import de.drvlabs.contactgrouper.groups.GroupViewModel

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            GroupDatabase::class.java,
            "groups.db"
        ).build()
    }
    private val contactViewModel by viewModels<ContactsViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ContactsViewModel(contentResolver, db.dao) as T
                }
            }
        }
    )

    private val groupViewModel by viewModels<GroupViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GroupViewModel(db.dao, applicationContext) as T
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var permanentlyDenied by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        val readGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
                        val writeGranted = permissions[Manifest.permission.WRITE_CONTACTS] ?: false
                        hasPermission = readGranted && writeGranted
                        if (!hasPermission && 
                            (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) ||
                             !shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS))) {
                            permanentlyDenied = true
                        }
                    }
                )

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route


                val bottomBarRoutes = navbarItems.map { it.route }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute in bottomBarRoutes) {
                            BottomNavigation(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    if (hasPermission) {
                        val groupState by groupViewModel.state.collectAsState()
                        val contactState by contactViewModel.state.collectAsState()
                        NavHost(
                            navController = navController,
                            startDestination = "contacts_graph",
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = { fadeIn(animationSpec = tween(400)) },
                            exitTransition = { fadeOut(animationSpec = tween(400)) }
                        ) {
                            navigation(
                                startDestination = Screen.NavBarScreen.Contacts.route,
                                route = "contacts_graph"
                            ){
                                composable(Screen.NavBarScreen.Contacts.route) {
                                    ContactsMainScreen(
                                        navController = navController,
                                        contactState = contactState,
                                        onContactEvent = contactViewModel::onEvent,
                                        groupState = groupState,
                                        onGroupEvent = groupViewModel::onEvent)
                                }
                                composable(Screen.ContactDetails.route,
                                    enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
                                ) {
                                    ContactDetailScreen(
                                        navController = navController,
                                        contactState = contactState,
                                        groupState = groupState,
                                        onGroupEvent = groupViewModel::onEvent,
                                        onContactEvent = contactViewModel::onEvent
                                    )
                                }
                            }
                            navigation(
                                startDestination = Screen.NavBarScreen.Groups.route,
                                route = "groups_graph"
                            ){
                                composable(Screen.NavBarScreen.Groups.route) {
                                    GroupsMainScreen(
                                        navController = navController,
                                        contactState = contactState,
                                        groupState = groupState,
                                        onEvent = groupViewModel::onEvent)
                                }
                                composable(Screen.AddGroup.route,
                                    enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }) {
                                    AddGroupScreen(
                                        navController = navController,
                                        state = groupState,
                                        onEvent = groupViewModel::onEvent)
                                }
                                composable(Screen.GroupDetails.route,
                                    enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) }
                                ) {
                                    GroupDetailScreen(
                                        navController = navController,
                                        contactState = contactState,
                                        groupState = groupState,
                                        onEvent = groupViewModel::onEvent
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding))
                    }
                }

                if (!hasPermission) {
                    PermissionDialog(
                        permanentlyDenied = permanentlyDenied,
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

//@Preview
//@Composable
//fun preview(){
//
//}

@Composable
fun PermissionDialog(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current

    val dialogText: String
    val buttonText: String
    val onButtonClick: () -> Unit

    if (permanentlyDenied) {
        dialogText = "Um die App zu nutzen, sind Zugriffsberechtigungen auf Ihre Kontakte notwendig. Bitte aktivieren Sie die Berechtigungen manuell in den App-Einstellungen. Starten Sie die App anschließend neu."
        buttonText = "Zu den Einstellungen"
        onButtonClick = { context.openAppSettings() }
    } else {
        dialogText = "Diese App benötigt Lese- und Schreibzugriff auf Ihre Kontakte, um sie in Gruppen zu organisieren und Klingeltöne zuzuweisen. Bitte erteilen Sie die Berechtigungen."
        buttonText = "Berechtigungen erteilen"
        onButtonClick = onRequestPermission
    }

    AlertDialog(
        onDismissRequest = { /* Nichts tun, Nutzer MUSS eine Wahl treffen */ },
        title = { Text("Berechtigung erforderlich") },
        text = { Text(dialogText) },
        confirmButton = {
            TextButton(onClick = onButtonClick) {
                Text(buttonText)
            }
        }
    )
}


fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    startActivity(intent)
}
