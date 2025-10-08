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
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

    private val contactViewModel: ContactsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ContactsViewModel(contentResolver, db.dao) as T
            }
        }
    }

    private val groupViewModel by viewModels<GroupViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GroupViewModel(db.dao) as T
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
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var permanentlyDenied by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasPermission = isGranted
                        if (!isGranted && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                            permanentlyDenied = true
                        }
                    }
                )

                val navController = rememberNavController()
                val groupState by groupViewModel.state.collectAsState()
                val contactState by contactViewModel.state.collectAsState()


                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigation(navController = navController)
                    }
                ) { innerPadding ->
                    if (hasPermission) {
                        val contacts by contactViewModel.contacts.collectAsState()
                        NavHost(
                            navController = navController,
                            startDestination = "contacts_graph",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            navigation(
                                startDestination = Screen.Contacts.route,
                                route = "contacts_graph"
                            ){
                                composable(Screen.Contacts.route) {
                                    ContactsMainScreen(
                                        navController = navController,
                                        contactState = contactState,
                                        onContactEvent = contactViewModel::onEvent,
                                        groupState = groupState,
                                        onGroupEvent = groupViewModel::onEvent)
                                }
                                composable(Screen.ContactDetails.route) {
                                    ContactDetailScreen(
                                        navController = navController,
                                        contactState = contactState,
                                        groupState = groupState
                                    )
                                }
                            }
                            navigation(
                                startDestination = Screen.Groups.route,
                                route = "groups_graph"
                            ){
                                composable(Screen.Groups.route) {
                                    GroupsMainScreen(
                                        navController,
                                        contacts,
                                        groupState,
                                        groupViewModel::onEvent)
                                }
                                composable(Screen.AddGroup.route) {
                                    AddGroupScreen(
                                        navController = navController,
                                        state = groupState,
                                        onEvent = groupViewModel::onEvent)
                                }
                                composable(Screen.GroupDetails.route) { 
                                    GroupDetailScreen(
                                        state = groupState,
                                        onEvent = groupViewModel::onEvent,
                                        allContacts = contacts,
                                        navController = navController
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
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun preview(){

}

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
        dialogText = "Um die App zu nutzen, ist der Zugriff auf Kontakte notwendig. Bitte aktivieren Sie die Berechtigung manuell in den App-Einstellungen."
        buttonText = "Zu den Einstellungen"
        onButtonClick = { context.openAppSettings() }
    } else {
        dialogText = "Diese App benötigt Lesezugriff auf Ihre Kontakte, um sie in Gruppen zu organisieren. Bitte erteilen Sie die Berechtigung."
        buttonText = "Berechtigung erteilen"
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
