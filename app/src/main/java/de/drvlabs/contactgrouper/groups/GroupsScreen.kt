package de.drvlabs.contactgrouper.groups

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.drvlabs.contactgrouper.contacts.ContactList
import de.drvlabs.contactgrouper.contacts.ContactState


@Composable
fun GroupsMainScreen(navController: NavController,
                     contactState: ContactState,
                     groupState: GroupState,
                     onEvent: (GroupEvent) -> Unit
) {
    val allContacts = contactState.contacts
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        if (groupState.groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No groups yet. Tap '+' to add one!", fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupState.groups) { group ->
                    val memberCount = allContacts.count { it.groupId == group.id }
                    GroupCard(group = group, memberCount = memberCount) {
                        onEvent(GroupEvent.SetSelectedGroup(group))
                        navController.navigate("GroupDetails")
                    }
                }
            }
        }
        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 24.dp),
            onClick = { navController.navigate("AddGroup") }
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
        colors = CardDefaults.cardColors(containerColor = group.color),
        //elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = group.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$memberCount members", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * A screen for adding a new contact group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupScreen(
    state: GroupState,
    onEvent: (GroupEvent) -> Unit,
    navController: NavController
) {
    var showNameError by remember { mutableStateOf(false) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> state.ringtoneUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Group") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                        onEvent(GroupEvent.SetGroupName(""))
                    }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (state.name.isNotBlank()) {
                                onEvent(GroupEvent.SaveGroup)
                                navController.popBackStack()
                                onEvent(GroupEvent.SetGroupName(""))
                            } else {
                                showNameError = true
                            }
                        },
                        modifier = Modifier.padding( end = 24.dp)) {
                        Icon(Icons.Default.Save, contentDescription = "Save Group", modifier = Modifier.padding(end = 8.dp))
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
                    onEvent(GroupEvent.SetGroupName(it))
                    if (it.isNotBlank()) showNameError = false
                },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = showNameError,
                singleLine = true
            )
            if (showNameError) {
                Text("Group name cannot be empty.", color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = { ringtonePickerLauncher.launch("audio/ringtone") }) {
                Text(if (state.ringtoneUri == null) "Assign Ringtone" else "Change Ringtone")
            }

            state.ringtoneUri?.let {
                Text("Ringtone selected: ${it.path?.substringAfterLast('/') ?: "Custom"}")
            }
        }

    }
}

/**
 * A screen showing the details of a specific group and its members.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    contactState: ContactState,
    groupState: GroupState,
    onEvent: (GroupEvent) -> Unit
) {
    val group = groupState.selectedGroup
    val allContacts = contactState.contacts
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${group?.name} Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            group?.let { onEvent(GroupEvent.DeleteGroup(it)) }
                            navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                Text("Group not found!")
            }
            return@Scaffold
        }

        val groupContacts = allContacts.filter { it.groupId == group.id }

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            // Group Info Section
            Text("Members: ${groupContacts.size}", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            // Member List
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp)
            ){
                if (groupContacts.isEmpty()) {
                    Text("No contacts have been added to this group yet.", modifier = Modifier.padding(16.dp))
                } else {
                    ContactList(contacts = groupContacts, onContactClick = {return@ContactList}, onContactLongClick = {return@ContactList})
                }
            }
            // TODO: Add a button here to navigate to a contact picker screen
            // to add/remove members from the group.
        }
    }
}