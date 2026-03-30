package de.drvlabs.contactgrouper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import de.drvlabs.contactgrouper.contacts.ContactDetailScreen
import de.drvlabs.contactgrouper.contacts.ContactsMainScreen
import de.drvlabs.contactgrouper.contacts.ContactsViewModel
import de.drvlabs.contactgrouper.groups.AddGroupScreen
import de.drvlabs.contactgrouper.groups.AddGroupViewModel
import de.drvlabs.contactgrouper.groups.GroupDetailScreen
import de.drvlabs.contactgrouper.groups.GroupViewModel
import de.drvlabs.contactgrouper.groups.GroupViewModel.Companion.factory as groupFactory
import de.drvlabs.contactgrouper.groups.GroupsMainScreen
import de.drvlabs.contactgrouper.groups.userMessage
import de.drvlabs.contactgrouper.permission.ContactsPermissionEvaluator
import de.drvlabs.contactgrouper.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    companion object {
        internal var bootstrapOverride: ((MainActivity, AppErrorReporter) -> MainActivityBootstrap)? =
            null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appErrorReporter = AppErrorReporter()
        val bootstrap = runCatching {
            val bootstrapper = bootstrapOverride ?: { activity: MainActivity, reporter: AppErrorReporter ->
                activity.bootstrapApp(reporter)
            }
            bootstrapper(this, appErrorReporter)
        }.getOrElse { throwable ->
            appErrorReporter.report(
                AppError.startupFatal(
                    origin = AppErrorOrigin.Bootstrap,
                    title = "App Failed to Start",
                    userMessage = "The app hit an unexpected error during startup.",
                    throwable = throwable,
                    heading = "Bootstrapping the app failed."
                )
            )
            null
        }

        setContent {
            AppTheme {
                val reporter = bootstrap?.appContainer?.appErrorReporter ?: appErrorReporter
                val currentError by reporter.currentError.collectAsState()

                if (bootstrap == null) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    MainActivityContent(
                        activity = this,
                        bootstrap = bootstrap,
                        currentError = currentError
                    )
                }

                currentError?.let { error ->
                    AppErrorDialog(
                        error = error,
                        onDismiss = reporter::clearCurrent,
                        onCloseApp = ::finishAffinity
                    )
                }
            }
        }
    }

    private fun bootstrapApp(appErrorReporter: AppErrorReporter): MainActivityBootstrap {
        val appContainer = AppContainer(
            context = applicationContext,
            appErrorReporter = appErrorReporter
        )
        val contactsViewModel = ViewModelProvider(
            this,
            ContactsViewModel.factory(
                contactsDataSource = appContainer.contactsDataSource,
                repository = appContainer.groupsRepository
            )
        )[ContactsViewModel::class.java]
        val groupViewModel = ViewModelProvider(
            this,
            groupFactory(appContainer.groupsRepository)
        )[GroupViewModel::class.java]

        return MainActivityBootstrap(
            appContainer = appContainer,
            contactsViewModel = contactsViewModel,
            groupViewModel = groupViewModel
        )
    }
}

internal data class MainActivityBootstrap(
    val appContainer: AppContainer,
    val contactsViewModel: ContactsViewModel,
    val groupViewModel: GroupViewModel
)

@Composable
private fun MainActivityContent(
    activity: MainActivity,
    bootstrap: MainActivityBootstrap,
    currentError: AppError?
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    var permissionState by remember {
        mutableStateOf(
            ContactsPermissionEvaluator.evaluate(
                activity = activity,
                hasRequestedPermission = false
            )
        )
    }

    fun refreshPermissionState() {
        permissionState = ContactsPermissionEvaluator.evaluate(
            activity = activity,
            hasRequestedPermission = hasRequestedPermission
        )
    }

    DisposableEffect(hasRequestedPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionState()
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose {
            activity.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            hasRequestedPermission = true
            refreshPermissionState()
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarRoutes = navbarItems.map { it.route }

    if (permissionState.hasPermission) {
        val contactsViewModel = bootstrap.contactsViewModel
        val groupViewModel = bootstrap.groupViewModel
        val appContainer = bootstrap.appContainer
        val contactState by contactsViewModel.state.collectAsState()
        val groupState by groupViewModel.state.collectAsState()

        LaunchedEffect(groupViewModel) {
            groupViewModel.messages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }

        DisposableEffect(permissionState.hasPermission) {
            appContainer.deviceGroupSyncManager.start()
            onDispose {
                appContainer.deviceGroupSyncManager.stop()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentRoute in bottomBarRoutes) {
                    BottomNavigation(navController = navController)
                }
            }
        ) { innerPadding ->
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
                ) {
                    composable(Screen.NavBarScreen.Contacts.route) {
                        ContactsMainScreen(
                            navController = navController,
                            state = contactState,
                            groups = groupState.groups,
                            onAssignContactsToGroups = { groupIds, contactIds ->
                                groupViewModel.assignContactsToGroups(groupIds, contactIds)
                            }
                        )
                    }
                    composable(
                        route = Screen.ContactDetails.route,
                        arguments = listOf(
                            navArgument(Screen.ContactDetails.ARG_CONTACT_ID) {
                                type = NavType.LongType
                            }
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { 1000 },
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { 1000 },
                                animationSpec = tween(300)
                            )
                        }
                    ) { backStackEntry ->
                        val contactId = backStackEntry.arguments?.getLong(
                            Screen.ContactDetails.ARG_CONTACT_ID
                        ) ?: return@composable

                        ContactDetailScreen(
                            navController = navController,
                            contactId = contactId,
                            contactState = contactState,
                            groupState = groupState,
                            onSaveGroups = { groupIds ->
                                groupViewModel.setContactGroups(contactId, groupIds)
                            },
                            onRemoveGroup = { groupId ->
                                groupViewModel.removeContactFromGroup(groupId, contactId)
                            }
                        )
                    }
                }

                navigation(
                    startDestination = Screen.NavBarScreen.Groups.route,
                    route = "groups_graph"
                ) {
                    composable(Screen.NavBarScreen.Groups.route) {
                        GroupsMainScreen(
                            navController = navController,
                            contactState = contactState,
                            groupState = groupState,
                            onRefresh = {
                                appContainer.deviceGroupSyncManager.syncNow()
                                    .userMessage()
                                    ?.let { snackbarHostState.showSnackbar(it) }
                            }
                        )
                    }
                    composable(
                        route = Screen.AddGroup.route,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { 1000 },
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { 1000 },
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        val addGroupViewModel: AddGroupViewModel = viewModel(
                            factory = AddGroupViewModel.factory(appContainer.groupsRepository)
                        )
                        val addGroupState by addGroupViewModel.state.collectAsState()

                        LaunchedEffect(addGroupViewModel) {
                            addGroupViewModel.messages.collect { message ->
                                snackbarHostState.showSnackbar(message)
                            }
                        }

                        AddGroupScreen(
                            navController = navController,
                            state = addGroupState,
                            onNameChange = addGroupViewModel::setGroupName,
                            onRingtoneSelected = addGroupViewModel::setRingtoneUri,
                            onCancel = {
                                addGroupViewModel.resetDraft()
                                navController.popBackStack()
                            },
                            onSave = { addGroupViewModel.saveGroup() }
                        )
                    }
                    composable(
                        route = Screen.GroupDetails.route,
                        arguments = listOf(
                            navArgument(Screen.GroupDetails.ARG_GROUP_ID) {
                                type = NavType.IntType
                            }
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { 1000 },
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { 1000 },
                                animationSpec = tween(300)
                            )
                        }
                    ) { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getInt(
                            Screen.GroupDetails.ARG_GROUP_ID
                        ) ?: return@composable

                        GroupDetailScreen(
                            navController = navController,
                            groupId = groupId,
                            contactState = contactState,
                            groupState = groupState,
                            onChangeRingtone = { ringtoneUri ->
                                groupViewModel.changeGroupRingtone(groupId, ringtoneUri)
                            },
                            onDeleteGroup = {
                                groupViewModel.deleteGroup(groupId)
                            }
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    if (!permissionState.hasPermission && currentError?.kind != AppErrorKind.StartupFatal) {
        PermissionDialog(
            permanentlyDenied = permissionState.permanentlyDenied,
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

@Composable
fun PermissionDialog(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val dialogText: String
    val buttonText: String
    val onButtonClick: () -> Unit

    if (permanentlyDenied) {
        dialogText = "This app needs contact access to organize groups and apply ringtones. Please enable the permissions in app settings."
        buttonText = "Open Settings"
        onButtonClick = { context.openAppSettings() }
    } else {
        dialogText = "This app needs contact read and write access to organize groups and apply ringtones."
        buttonText = "Grant Permissions"
        onButtonClick = onRequestPermission
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Permission Required") },
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
