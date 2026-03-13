package de.drvlabs.contactgrouper.contacts

import android.media.RingtoneManager
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.core.net.toUri
import coil.compose.AsyncImage
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupEvent
import de.drvlabs.contactgrouper.groups.GroupState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    navController: NavController,
    contactState: ContactState,
    groupState: GroupState,
    onGroupEvent: (GroupEvent) -> Unit,
    onContactEvent: (ContactEvent) -> Unit
) {
    val contact = contactState.selectedContact ?: return
    val groupsById = groupState.groups.associateBy { it.id }
    val contactGroups = contact.groupIds.mapNotNull(groupsById::get)
    val availableGroups = groupState.groups.filter { it.isMembershipEditable && it.id !in contact.groupIds }
    var showAddGroupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    if (availableGroups.isNotEmpty()) {
                        IconButton(onClick = { showAddGroupDialog = true }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Add to group")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                ContactHeader(contact)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (contact.phoneNumbers.isNotEmpty()) {
                item {
                    DetailSection(title = "Phone") {
                        contact.phoneNumbers.forEach { item ->
                            DetailItem(
                                icon = Icons.Default.Phone,
                                value = item.value,
                                type = getPhoneTypeLabel(item.typeConstant)
                            )
                        }
                    }
                }
            }
            if (contact.emails.isNotEmpty()) {
                item {
                    DetailSection(title = "Email") {
                        contact.emails.forEach { item ->
                            DetailItem(
                                icon = Icons.Default.Email,
                                value = item.value,
                                type = getEmailTypeLabel(item.typeConstant)
                            )
                        }
                    }
                }
            }
            if (contact.addresses.isNotEmpty()) {
                item {
                    DetailSection(title = "Address") {
                        contact.addresses.forEach { address ->
                            DetailItem(
                                icon = Icons.Default.LocationOn,
                                value = address.formattedAddress,
                                type = getAddressTypeLabel(address.typeConstant)
                            )
                        }
                    }
                }
            }

            item {
                DetailSection(title = "Ringtone") {
                    val context = LocalContext.current
                    val title = contact.customRingtone?.let { ringtoneValue ->
                        val ringtone = RingtoneManager.getRingtone(context, ringtoneValue.toUri())
                        ringtone?.getTitle(context)
                    } ?: "Default"

                    DetailItem(
                        icon = Icons.Default.PhoneInTalk,
                        value = title ?: "Default",
                        type = null
                    )
                }
            }

            if (contactGroups.isNotEmpty()) {
                item {
                    DetailSection(title = "Groups") {
                        contactGroups.forEach { group ->
                            DetailGroupItem(
                                contact = contact,
                                group = group,
                                isEffective = contact.effectiveRingtoneGroupId == group.id,
                                onGroupEvent = onGroupEvent,
                                onContactEvent = onContactEvent
                            )
                        }
                    }
                }
            } else if (availableGroups.isNotEmpty()) {
                item {
                    DetailSection(title = "Groups") {
                        Text(
                            text = "This contact is not in any group yet.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showAddGroupDialog) {
        AddContactToGroupsDialog(
            groups = availableGroups,
            onDismiss = { showAddGroupDialog = false },
            onAssign = { groupIds ->
                onGroupEvent(GroupEvent.AssignContactsToGroups(groupIds, listOf(contact.id)))
                showAddGroupDialog = false
            }
        )
    }
}

@Composable
private fun ContactHeader(contact: Contact) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (contact.photoUri != null) {
                AsyncImage(
                    model = contact.photoUri,
                    contentDescription = contact.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = contact.displayName.firstOrNull()?.uppercase() ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(
            text = contact.displayName,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, type: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
            type?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailGroupItem(
    contact: Contact,
    group: Group,
    isEffective: Boolean,
    onGroupEvent: (GroupEvent) -> Unit,
    onContactEvent: (ContactEvent) -> Unit
) {
    val subtitle = buildString {
        if (isEffective) {
            append("Controls ringtone")
        }
        if (group.isDeviceBacked) {
            if (isNotEmpty()) append(" • ")
            append("Synced from device")
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = group.name, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (group.isMembershipEditable) {
            IconButton(
                onClick = {
                    onGroupEvent(GroupEvent.RemoveGroupMember(contact, group))
                    onContactEvent(ContactEvent.ClearContactGroup(contact.id, group.id))
                }
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Remove from group")
            }
        }
    }
}

@Composable
private fun AddContactToGroupsDialog(
    groups: List<Group>,
    onDismiss: () -> Unit,
    onAssign: (List<Int>) -> Unit
) {
    val selectedGroupIds = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact To Groups") },
        text = {
            LazyColumn {
                items(groups) { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = group.name)
                            if (group.isDeviceBacked) {
                                Text(
                                    text = "Syncs back to contacts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                if (group.id in selectedGroupIds) {
                                    selectedGroupIds.remove(group.id)
                                } else {
                                    selectedGroupIds.add(group.id)
                                }
                            }
                        ) {
                            Text(if (group.id in selectedGroupIds) "Selected" else "Add")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAssign(selectedGroupIds.toList()) },
                enabled = selectedGroupIds.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getPhoneTypeLabel(type: Int): String {
    return when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
        else -> "Other"
    }
}

private fun getEmailTypeLabel(type: Int): String {
    return when (type) {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "Work"
        ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> "Mobile"
        else -> "Other"
    }
}

private fun getAddressTypeLabel(type: Int): String {
    return when (type) {
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> "Work"
        else -> "Other"
    }
}
