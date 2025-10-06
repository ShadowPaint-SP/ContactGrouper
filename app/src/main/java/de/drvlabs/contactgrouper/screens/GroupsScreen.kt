package de.drvlabs.contactgrouper.screens

import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.drvlabs.contactgrouper.ui.theme.backgroundLight
import de.drvlabs.contactgrouper.ui.theme.success
import de.drvlabs.contactgrouper.viewmodels.Contact
import de.drvlabs.contactgrouper.viewmodels.ContactGroup
import de.drvlabs.contactgrouper.viewmodels.GroupViewModel



@Composable
fun GroupsMainScreen(navController: NavController,
                     groupViewModel: GroupViewModel,
                     allContacts: List<Contact>
) {
    val groups by groupViewModel.allContactGroups.collectAsState()
    Box {
        if (groups.isEmpty()) {
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
                items(groups) { group ->
                    val memberCount = allContacts.count { it.id == group.id }
                    GroupCard(group = group, memberCount = memberCount) {
                        navController.navigate("GroupDetails/${group.id}")
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { navController.navigate("AddGroup") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end= 24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Group")
        }

    }
}

@Composable
fun GroupCard(group: ContactGroup, memberCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
    navController: NavController,
    groupViewModel: GroupViewModel
) {
    var groupName by remember { mutableStateOf("") }
    var ringtoneUri by remember { mutableStateOf<Uri?>(null) }
    var showNameError by remember { mutableStateOf(false) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> ringtoneUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = {
                        groupName = it
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
                    Text(if (ringtoneUri == null) "Assign Ringtone" else "Change Ringtone")
                }

                ringtoneUri?.let {
                    Text("Ringtone selected: ${it.path?.substringAfterLast('/') ?: "Custom"}")
                }
            }
            FloatingActionButton(
                onClick = {
                    if (groupName.isNotBlank()) {
                        //val newGroup = ContactGroup(name = groupName, ringtoneUri = ringtoneUri)
                        groupViewModel.addContactGroup(groupName, emptyList())
                        navController.popBackStack()
                    } else {
                        showNameError = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 24.dp),
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save Group")
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
    groupId: Int,
    groupViewModel: GroupViewModel,
    allContacts: List<Contact>, // Pass the full contact list
    navController: NavController
) {
//    val group = groupViewModel.getGroupById(groupId)
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(group?. ?: "Group Details") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        if (group == null) {
//            Box(
//                modifier = Modifier.fillMaxSize().padding(padding),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("Group not found!")
//            }
//            return@Scaffold
//        }
//
//        val groupContacts = allContacts.filter { it.groupId == groupId }
//
//
//        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
//            // Group Info Section
//            Text("Members (${groupContacts.size})", style = MaterialTheme.typography.titleLarge)
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Member List
//            if (groupContacts.isEmpty()) {
//                Text("No contacts have been added to this group yet.")
//            } else {
//                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                    items(groupContacts) { contact ->
//                        Text(contact.name, fontSize = 18.sp)
//                    }
//                }
//            }
//            // TODO: Add a button here to navigate to a contact picker screen
//            // to add/remove members from the group.
//        }
//    }
}