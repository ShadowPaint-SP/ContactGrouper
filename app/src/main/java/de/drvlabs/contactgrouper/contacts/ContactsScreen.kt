package de.drvlabs.contactgrouper.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupEvent
import de.drvlabs.contactgrouper.groups.GroupState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsMainScreen(
    navController: NavController,
    contactState: ContactState,
    onContactEvent: (ContactEvent) -> Unit,
    onGroupEvent: (GroupEvent) -> Unit,
    groupState: GroupState
) {

    var selectedContacts by remember { mutableStateOf(setOf<Long>()) }
    val isInSelectionMode = selectedContacts.isNotEmpty()
    var showAssignGroupDialog by remember { mutableStateOf(false) }
    val contacts = contactState.contacts

    BackHandler(enabled = isInSelectionMode) {
        selectedContacts = emptySet()
    }

    Box{
        ContactList(
            contacts = contacts,
            groups = groupState.groups,
            selectedContacts = selectedContacts,
            onContactClick = { contact ->
                if (isInSelectionMode) {
                    selectedContacts = if (selectedContacts.contains(contact.id)) {
                        selectedContacts - contact.id
                    } else {
                        selectedContacts + contact.id
                    }
                } else {
                    onContactEvent(ContactEvent.SetSelectContact(contact))
                    navController.navigate("ContactDetails")
                }
            },
            onContactLongClick = { contact ->
                selectedContacts = selectedContacts + contact.id
            }
        )

        if (isInSelectionMode) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 24.dp),
                onClick = {
                    showAssignGroupDialog= true
                }) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = "Assign"
                )
            }
        }
        if (showAssignGroupDialog) {
            AssignToGroupDialog(
                groups = groupState.groups,
                onDismiss = { showAssignGroupDialog = false },
                onAssign = { groupId ->
                    onGroupEvent(GroupEvent.AssignContactsToGroup(groupId, selectedContacts.map { it }))
                    showAssignGroupDialog = false
                    selectedContacts = emptySet()
                }
            )
        }
    }
}

@Composable
private fun AssignToGroupDialog(
    groups: List<Group>,
    onDismiss: () -> Unit,
    onAssign: (Int) -> Unit
) {
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to Group") },
        text = {
            LazyColumn {
                items(groups) { group ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable { selectedGroupId = group.id }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedGroupId == group.id),
                            onClick = { selectedGroupId = group.id }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = group.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedGroupId?.let { onAssign(it) }
                },
                enabled = selectedGroupId != null
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ContactList(
    modifier: Modifier = Modifier,
    contacts: List<Contact>,
    groups: List<Group> = emptyList(),
    selectedContacts: Set<Long> = emptySet(),
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit
){
    val groupedContacts = contacts.groupBy { it.displayName.first().uppercase() }
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        groupedContacts.forEach { (initial, contactsInGroup) ->
            stickyHeader {
                Text(
                    text = initial,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(start = 32.dp, top = 6.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Spacer(Modifier.size(8.dp))
            }

            itemsIndexed(contactsInGroup) { index, contact ->
                val isSelected = selectedContacts.contains(contact.id)

                val largeRadius = 24.dp
                val smallRadius = 8.dp

                val cardShape = when {
                    contactsInGroup.size == 1 -> RoundedCornerShape(largeRadius)
                    index == 0 -> RoundedCornerShape(
                        topStart = largeRadius, topEnd = largeRadius,
                        bottomStart = smallRadius, bottomEnd = smallRadius
                    )
                    index == contactsInGroup.size - 1 -> RoundedCornerShape(
                        topStart = smallRadius, topEnd = smallRadius,
                        bottomStart = largeRadius, bottomEnd = largeRadius
                    )
                    else -> RoundedCornerShape(smallRadius)
                }

                val bottomPadding = if (index < contactsInGroup.size - 1) 2.dp else 0.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomPadding)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onContactClick(contact) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onContactLongClick(contact)
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onTertiary
                                    )
                                } else {
                                    // Show photo or initial when not selected
                                    if (contact.photoUri != null) {
                                        AsyncImage(
                                            model = contact.photoUri,
                                            contentDescription = contact.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = initial,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = contact.displayName,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (contact.groupId != null) {
                                Spacer(modifier = Modifier.weight(1f))
                                val group = groups.find { it.id == contact.groupId }
                                if (group != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    GroupBadge(group = group)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

/**
 * Displays a group label badge with the group name and color.
 */
@Composable
private fun GroupBadge(group: Group) {
    Surface(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = group.color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = group.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}
