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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.navigation.compose.rememberNavController
import de.drvlabs.contactgrouper.ui.theme.AppTheme
import de.drvlabs.contactgrouper.views.ContactList
import de.drvlabs.contactgrouper.views.GroupEditor



class MainActivity : ComponentActivity() {
    private val viewModel: ContactsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactsViewModel(contentResolver) as T
            }
        }
    }

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

                var selectedScreen by remember { mutableStateOf<Screen>(Screen.Contacts) }
                val navController = rememberNavController()
//
//              APP BASE
//
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigation(navController = navController)
                    }
                ) { innerPadding ->
                    if (hasPermission) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Contacts.route, // Start-Screen
                            modifier = Modifier.padding(innerPadding),
                            enterTransition = {EnterTransition.None},
                            exitTransition = {ExitTransition.None}
                        ) {
                            composable(Screen.Contacts.route) {
                                val contacts = viewModel.contacts.collectAsState()
                                ContactList(contacts = contacts.value)
                            }
                            composable(Screen.Groups.route) {
                                GroupEditor()
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

