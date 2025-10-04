package de.drvlabs.contactgrouper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.drvlabs.contactgrouper.ui.theme.AppTheme
import de.drvlabs.contactgrouper.views.ContactList
import de.drvlabs.contactgrouper.views.GroupEditor
sealed class Screen(val route: String, val selected: ImageVector, val unselected: ImageVector) {
    object Contacts : Screen("Contacts", Icons.Filled.Person, Icons.Outlined.Person)
    object Groups : Screen("Groups", Icons.Filled.Groups, Icons.Outlined.Groups)
}

class MainActivity : ComponentActivity() {
    private val viewModel: ContactsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactsViewModel(contentResolver) as T
            }
        }
    }

    private val permissions = arrayOf(
        Manifest.permission.READ_CONTACTS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var selectedScreen by remember { mutableStateOf<Screen>(Screen.Contacts) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigation(selectedScreen = selectedScreen) { screen ->
                            selectedScreen = screen
                        }
                    }
                ) { innerPadding ->

                    var hasPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }
                    var requestInProgress by remember { mutableStateOf(false) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { isGranted ->
                            hasPermission = isGranted
                            requestInProgress = false
                        }
                    )

                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_START) { hasPermission = ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    if (hasPermission) {
                        when (selectedScreen) {
                            Screen.Contacts -> {
                                val contacts = viewModel.contacts.collectAsState()
                                ContactList(
                                    contacts = contacts.value,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                            Screen.Groups -> {
                                GroupEditor(modifier = Modifier.padding(innerPadding))
                            }
                        }
                    } else {
                        PermissionRequest(
                            modifier = Modifier.padding(innerPadding),
                            requestInProgress = requestInProgress,
                            onPermissionRequested = {
                                requestInProgress = true
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigation(selectedScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val items = listOf(Screen.Contacts, Screen.Groups)
    NavigationBar {
        items.forEach { screen ->
            val isSelected = selectedScreen == screen
            NavigationBarItem(
                icon = {
                    Icon(if (isSelected) screen.selected else screen.unselected, contentDescription = screen.route)
                },
                label = { Text(screen.route) },
                selected = isSelected,
                onClick = { onScreenSelected(screen) }
            )
        }
    }
}



@Composable
fun PermissionRequest(
    modifier: Modifier = Modifier,
    requestInProgress: Boolean,
    onPermissionRequested: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "We need to read your contacts to group them.")
        if (requestInProgress) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else {
            Button(

                onClick = onPermissionRequested,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Grant Permission")
            }
        }
    }
}
