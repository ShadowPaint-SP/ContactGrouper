package de.drvlabs.contactgrouper.groups

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.drvlabs.contactgrouper.Screen
import de.drvlabs.contactgrouper.contacts.ContactList
import de.drvlabs.contactgrouper.contacts.ContactsListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsMainScreen(
    navController: NavController,
    contactState: ContactsListState,
    groupState: GroupsListState,
    onRefresh: suspend () -> Unit
) {
    val allContacts = contactState.contacts
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val minimumRefreshIndicatorMillis = 350L

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = isRefreshing,
            onRefresh = {
                if (isRefreshing) {
                    return@PullToRefreshBox
                }
                coroutineScope.launch {
                    isRefreshing = true
                    val refreshStartedAt = System.currentTimeMillis()
                    try {
                        onRefresh()
                    } finally {
                        val elapsed = System.currentTimeMillis() - refreshStartedAt
                        val remaining = minimumRefreshIndicatorMillis - elapsed
                        if (remaining > 0) {
                            delay(remaining)
                        }
                        isRefreshing = false
                    }
                }
            }
        ) {
            if (groupState.groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No groups yet.\nPull down to sync device groups or tap '+' to add one.",
                        fontSize = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groupState.groups) { group ->
                        val memberCount = allContacts.count { group.id in it.groupIds }
                        GroupCard(group = group, memberCount = memberCount) {
                            navController.navigate(Screen.GroupDetails.createRoute(group.id))
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 24.dp),
            onClick = { navController.navigate(Screen.AddGroup.route) }
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Group")
        }
    }
}

@Composable
fun GroupCard(group: Group, memberCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = group.color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = group.name, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$memberCount members", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupScreen(
    navController: NavController,
    state: AddGroupState,
    onNameChange: (String) -> Unit,
    onRingtoneSelected: (Uri?) -> Unit,
    onCancel: () -> Unit,
    onSave: suspend () -> GroupMutationResult
) {
    var showNameError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                val uri =
                    result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                onRingtoneSelected(uri)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Group") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (state.name.isBlank()) {
                                showNameError = true
                                return@Button
                            }

                            coroutineScope.launch {
                                val result = onSave()
                                if (result.isSuccess) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 24.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save Group",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = {
                    onNameChange(it)
                    if (it.isNotBlank()) {
                        showNameError = false
                    }
                },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = showNameError,
                singleLine = true
            )
            if (showNameError) {
                Text("Group name cannot be empty.", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    ringtonePickerLauncher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER))
                }
            ) {
                Text(if (state.ringtoneUri == null) "Assign Ringtone" else "Change Ringtone")
            }

            state.ringtoneUri?.let {
                val ringtone = RingtoneManager.getRingtone(context, it)
                val title = ringtone.getTitle(context)
                Text("Ringtone selected: $title")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    groupId: Int,
    contactState: ContactsListState,
    groupState: GroupsListState,
    onChangeRingtone: suspend (Uri?) -> GroupMutationResult,
    onDeleteGroup: suspend () -> GroupMutationResult
) {
    val group = groupState.groups.find { it.id == groupId }
    val allContacts = contactState.contacts
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                val uri =
                    result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                coroutineScope.launch {
                    onChangeRingtone(uri)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    if (group != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Change Ringtone") },
                                    onClick = {
                                        showMenu = false
                                        ringtonePickerLauncher.launch(
                                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (group.deletesFromDevice) {
                                                "Delete Group From Device"
                                            } else {
                                                "Delete Group"
                                            }
                                        )
                                    },
                                    enabled = group.canDelete,
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirmation = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (group == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Group not found.")
            }
            return@Scaffold
        }

        val groupContacts = allContacts.filter { group.id in it.groupIds }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = group.color)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${groupContacts.size} member${if (groupContacts.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (group.isDeviceBacked) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Imported from device contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (group.deletesFromDevice) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Syncs to device contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    group.ringtoneUri?.let {
                        val ringtone = RingtoneManager.getRingtone(context, it)
                        val title = ringtone.getTitle(context)
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = "Ringtone",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Text(
                text = "Members",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Card(modifier = Modifier.fillMaxSize()) {
                if (groupContacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No contacts have been added to this group yet.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    ContactList(
                        contacts = groupContacts,
                        groups = groupState.groups,
                        onContactClick = { contact ->
                            navController.navigate(Screen.ContactDetails.createRoute(contact.id))
                        },
                        onContactLongClick = {}
                    )
                }
            }
        }
    }

    if (group != null && showDeleteConfirmation) {
        val confirmation = buildGroupDeletionConfirmation(group)
        DeleteGroupConfirmationDialog(
            title = confirmation.title,
            message = confirmation.message,
            confirmLabel = confirmation.confirmLabel,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                coroutineScope.launch {
                    val result = onDeleteGroup()
                    if (result.isSuccess) {
                        showDeleteConfirmation = false
                        navController.popBackStack()
                    }
                }
            }
        )
    }
}

@Composable
fun DeleteGroupConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
