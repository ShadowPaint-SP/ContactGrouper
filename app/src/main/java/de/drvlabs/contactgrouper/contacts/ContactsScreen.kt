package de.drvlabs.contactgrouper.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.drvlabs.contactgrouper.Screen
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupMutationResult
import de.drvlabs.contactgrouper.groups.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsMainScreen(
    navController: NavController,
    state: ContactsListState,
    groups: List<Group>,
    onSetContactsGroups: suspend (Map<Long, List<Int>>) -> GroupMutationResult
) {
    var selectedContacts by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val isInSelectionMode = selectedContacts.isNotEmpty()
    var showAssignGroupDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = isInSelectionMode) {
        selectedContacts = emptySet()
    }

    Box {
        ContactList(
            contacts = state.contacts,
            groups = groups,
            selectedContacts = selectedContacts,
            onContactClick = { contact ->
                if (isInSelectionMode) {
                    selectedContacts = if (contact.id in selectedContacts) {
                        selectedContacts - contact.id
                    } else {
                        selectedContacts + contact.id
                    }
                } else {
                    navController.navigate(Screen.ContactDetails.createRoute(contact.id))
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
                onClick = { showAssignGroupDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = "Assign groups"
                )
            }
        }

        if (showAssignGroupDialog) {
            val selectedContactModels = state.contacts.filter { it.id in selectedContacts }
            AssignToGroupDialog(
                groups = groups,
                selectedContacts = selectedContactModels,
                onDismiss = { showAssignGroupDialog = false },
                onAssign = { contactGroupIds ->
                    setContactsGroups(
                        scope = coroutineScope,
                        contactGroupIds = contactGroupIds,
                        onSetContactsGroups = onSetContactsGroups,
                        onSuccess = {
                            selectedContacts = emptySet()
                            showAssignGroupDialog = false
                        }
                    )
                }
            )
        }
    }
}

private fun setContactsGroups(
    scope: CoroutineScope,
    contactGroupIds: Map<Long, List<Int>>,
    onSetContactsGroups: suspend (Map<Long, List<Int>>) -> GroupMutationResult,
    onSuccess: () -> Unit
) {
    scope.launch {
        val result = onSetContactsGroups(contactGroupIds)
        if (result.isSuccess) {
            onSuccess()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssignToGroupDialog(
    groups: List<Group>,
    selectedContacts: List<Contact>,
    onDismiss: () -> Unit,
    onAssign: (Map<Long, List<Int>>) -> Unit
) {
    val editableGroups = groups.filter { it.isMembershipEditable }
    val initialGroupStates = remember(selectedContacts, editableGroups) {
        initialBulkGroupMembershipStates(selectedContacts, editableGroups)
    }
    val groupStates = remember(initialGroupStates) {
        mutableStateListOf<Pair<Int, BulkGroupMembershipState>>().apply {
            addAll(initialGroupStates.toList())
        }
    }

    fun groupState(groupId: Int): BulkGroupMembershipState {
        return groupStates.firstOrNull { it.first == groupId }?.second
            ?: BulkGroupMembershipState.Unselected
    }

    fun setGroupState(groupId: Int, state: BulkGroupMembershipState) {
        val index = groupStates.indexOfFirst { it.first == groupId }
        if (index >= 0) {
            groupStates[index] = groupId to state
        }
    }

    fun cycleGroupState(groupId: Int) {
        setGroupState(groupId, nextBulkGroupMembershipState(groupState(groupId)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Groups") },
        text = {
            if (editableGroups.isEmpty()) {
                Text("Create a local group first. Imported read-only groups cannot be changed here.")
            } else {
                LazyColumn {
                    items(editableGroups) { group ->
                        val state = groupState(group.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable { cycleGroupState(group.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TriStateCheckbox(
                                state = state.toToggleableState(),
                                onClick = { cycleGroupState(group.id) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = group.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAssign(
                        buildBulkContactGroupSelections(
                            selectedContacts = selectedContacts,
                            editableGroups = editableGroups,
                            groupStates = groupStates.toMap()
                        )
                    )
                },
                enabled = editableGroups.isNotEmpty() && selectedContacts.isNotEmpty()
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

private fun BulkGroupMembershipState.toToggleableState(): ToggleableState {
    return when (this) {
        BulkGroupMembershipState.Selected -> ToggleableState.On
        BulkGroupMembershipState.Partial -> ToggleableState.Indeterminate
        BulkGroupMembershipState.Unselected -> ToggleableState.Off
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ContactList(
    modifier: Modifier = Modifier,
    contacts: List<Contact>,
    groups: List<Group> = emptyList(),
    selectedContacts: Set<Long> = emptySet(),
    onContactClick: (Contact) -> Unit,
    onContactLongClick: (Contact) -> Unit
) {
    val groupedContacts = contacts.groupBy { it.displayName.firstOrNull()?.uppercase() ?: "#" }
    val groupsById = groups.associateBy { it.id }
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

            item { Spacer(Modifier.size(8.dp)) }

            itemsIndexed(contactsInGroup) { index, contact ->
                val isSelected = selectedContacts.contains(contact.id)
                val largeRadius = 24.dp
                val smallRadius = 8.dp
                val cardShape = when {
                    contactsInGroup.size == 1 -> RoundedCornerShape(largeRadius)
                    index == 0 -> RoundedCornerShape(
                        topStart = largeRadius,
                        topEnd = largeRadius,
                        bottomStart = smallRadius,
                        bottomEnd = smallRadius
                    )

                    index == contactsInGroup.size - 1 -> RoundedCornerShape(
                        topStart = smallRadius,
                        topEnd = smallRadius,
                        bottomStart = largeRadius,
                        bottomEnd = largeRadius
                    )

                    else -> RoundedCornerShape(smallRadius)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = if (index < contactsInGroup.lastIndex) 2.dp else 0.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onTertiary
                                    )
                                } else if (contact.photoUri != null) {
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

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.displayName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                val contactGroups = contact.groupIds.mapNotNull(groupsById::get)
                                if (contactGroups.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow {
                                        contactGroups.take(3).forEach { group ->
                                            GroupBadge(
                                                group = group,
                                                isEffective = contact.effectiveRingtoneGroupId == group.id
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        if (contactGroups.size > 3) {
                                            Text(
                                                text = "+${contactGroups.size - 3}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(8.dp)) }
        }
    }
}

@Composable
fun GroupBadge(
    group: Group,
    isEffective: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = group.color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
            if (isEffective) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Controls ringtone",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
