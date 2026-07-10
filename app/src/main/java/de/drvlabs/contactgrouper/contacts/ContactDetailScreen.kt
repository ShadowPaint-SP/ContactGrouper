@file:Suppress("DEPRECATION")

package de.drvlabs.contactgrouper.contacts

import android.media.RingtoneManager
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.drvlabs.contactgrouper.R
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupMutationResult
import de.drvlabs.contactgrouper.groups.GroupsListState
import de.drvlabs.contactgrouper.groups.isSuccess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    navController: NavController,
    contactId: Long,
    contactState: ContactsListState,
    groupState: GroupsListState,
    onSaveGroups: suspend (List<Int>) -> GroupMutationResult,
    onRemoveGroup: suspend (Int) -> GroupMutationResult,
    onEditContact: (Long) -> Unit,
    onDeleteContact: suspend (Long) -> Boolean,
    hasSeenMultipleGroupsRingtoneInfo: Boolean = true,
    onMultipleGroupsRingtoneInfoAcknowledged: () -> Unit = {}
) {
    val contact = contactState.contacts.find { it.id == contactId }
    val groupsById = groupState.groups.associateBy { it.id }
    var showManageGroupsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var multipleGroupsRingtoneInfo by remember { mutableStateOf<MultipleGroupsRingtoneInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (contact != null) {
                        IconButton(onClick = { onEditContact(contact.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit contact")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete contact")
                        }
                    }
                    val editableGroups = groupState.groups.filter { it.isMembershipEditable }
                    if (contact != null && editableGroups.isNotEmpty()) {
                        IconButton(onClick = { showManageGroupsDialog = true }) {
                            Icon(
                                Icons.Default.GroupAdd,
                                contentDescription = stringResource(R.string.contacts_manage_groups)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (contact == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.contact_not_found))
            }
            return@Scaffold
        }

        val contactGroups = contact.groupIds.mapNotNull(groupsById::get)
        val editableGroups = groupState.groups
            .filter { it.isMembershipEditable }
            .sortedBy { it.name.lowercase() }
        val selectedEditableGroupIds = editableGroups
            .filter { it.id in contact.groupIds }
            .map(Group::id)

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
                    DetailSection(title = stringResource(R.string.contact_section_phone)) {
                        contact.phoneNumbers.forEach { item ->
                            DetailItem(
                                icon = Icons.Default.Phone,
                                value = item.value,
                                type = getPhoneTypeLabel(item)
                            )
                        }
                    }
                }
            }

            item {
                DetailSection(title = stringResource(R.string.contact_section_ringtone)) {
                    val context = LocalContext.current
                    val defaultRingtoneTitle = stringResource(R.string.contact_default_ringtone)
                    val title = contact.customRingtone?.let { ringtoneValue ->
                        val ringtone = RingtoneManager.getRingtone(context, ringtoneValue.toUri())
                        ringtone?.getTitle(context)
                    } ?: defaultRingtoneTitle

                    DetailItem(
                        icon = Icons.Default.PhoneInTalk,
                        value = title ?: defaultRingtoneTitle,
                        type = null
                    )
                }
            }

            if (contactGroups.isNotEmpty()) {
                item {
                    DetailSection(title = stringResource(R.string.contact_section_groups)) {
                        contactGroups.forEach { group ->
                            DetailGroupItem(
                                group = group,
                                onRemove = {
                                    coroutineScope.launch {
                                        onRemoveGroup(group.id)
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (editableGroups.isNotEmpty()) {
                item {
                    DetailSection(title = stringResource(R.string.contact_section_groups)) {
                        Text(
                            text = stringResource(R.string.contact_no_groups),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (contact.hasPersonalDetails()) {
                item {
                    DetailSection(title = "Personal") {
                        contact.nickname?.let { nickname ->
                            DetailItem(
                                icon = Icons.Default.Person,
                                value = nickname,
                                type = "Nickname"
                            )
                        }
                        contact.structuredName?.details().orEmpty().forEach { (label, value) ->
                            DetailItem(
                                icon = Icons.Default.Person,
                                value = value,
                                type = label
                            )
                        }
                    }
                }
            }

            if (contact.emails.isNotEmpty()) {
                item {
                    DetailSection(title = stringResource(R.string.contact_section_email)) {
                        contact.emails.forEach { item ->
                            DetailItem(
                                icon = Icons.Default.Email,
                                value = item.value,
                                type = getEmailTypeLabel(item)
                            )
                        }
                    }
                }
            }
            if (contact.addresses.isNotEmpty()) {
                item {
                    DetailSection(title = stringResource(R.string.contact_section_address)) {
                        contact.addresses.forEach { address ->
                            DetailItem(
                                icon = Icons.Default.LocationOn,
                                value = address.formattedAddress,
                                type = getAddressTypeLabel(address)
                            )
                        }
                    }
                }
            }
            if (contact.organizations.isNotEmpty()) {
                item {
                    DetailSection(title = "Organization") {
                        contact.organizations.forEach { organization ->
                            DetailItem(
                                icon = Icons.Default.Business,
                                value = organization.company,
                                type = listOfNotNull(
                                    organization.title,
                                    organization.department,
                                    getOrganizationTypeLabel(organization)
                                ).joinToString(" / ").takeUnless { it.isBlank() }
                            )
                        }
                    }
                }
            }
            if (contact.websites.isNotEmpty()) {
                item {
                    DetailSection(title = "Websites") {
                        contact.websites.forEach { website ->
                            DetailItem(
                                icon = Icons.Default.Link,
                                value = website.url,
                                type = getWebsiteTypeLabel(website)
                            )
                        }
                    }
                }
            }
            if (contact.events.isNotEmpty()) {
                item {
                    DetailSection(title = "Dates") {
                        contact.events.forEach { event ->
                            DetailItem(
                                icon = Icons.Default.Cake,
                                value = event.date,
                                type = getEventTypeLabel(event)
                            )
                        }
                    }
                }
            }
            if (contact.relations.isNotEmpty()) {
                item {
                    DetailSection(title = "Relationships") {
                        contact.relations.forEach { relation ->
                            DetailItem(
                                icon = Icons.Default.Person,
                                value = relation.name,
                                type = getRelationTypeLabel(relation)
                            )
                        }
                    }
                }
            }
            if (contact.instantMessages.isNotEmpty() || contact.sipAddresses.isNotEmpty()) {
                item {
                    DetailSection(title = "Messaging") {
                        contact.instantMessages.forEach { message ->
                            DetailItem(
                                icon = Icons.Default.Chat,
                                value = message.handle,
                                type = listOfNotNull(
                                    getImProtocolLabel(message),
                                    getImTypeLabel(message)
                                ).joinToString(" / ")
                            )
                        }
                        contact.sipAddresses.forEach { sip ->
                            DetailItem(
                                icon = Icons.Default.PhoneInTalk,
                                value = sip.value,
                                type = "SIP ${getSipTypeLabel(sip)}"
                            )
                        }
                    }
                }
            }
            if (contact.notes.isNotEmpty()) {
                item {
                    DetailSection(title = "Notes") {
                        contact.notes.forEach { note ->
                            DetailItem(
                                icon = Icons.Default.Notes,
                                value = note,
                                type = null
                            )
                        }
                    }
                }
            }

            if (contact.starred || contact.sendToVoicemail) {
                item {
                    DetailSection(title = "Options") {
                        if (contact.starred) {
                            DetailItem(
                                icon = Icons.Default.Star,
                                value = "Starred",
                                type = null
                            )
                        }
                        if (contact.sendToVoicemail) {
                            DetailItem(
                                icon = Icons.Default.PhoneInTalk,
                                value = "Send calls to voicemail",
                                type = null
                            )
                        }
                    }
                }
            }
        }

        if (showManageGroupsDialog) {
            ManageContactGroupsDialog(
                groups = editableGroups,
                selectedGroupIds = selectedEditableGroupIds,
                onDismiss = { showManageGroupsDialog = false },
                onSave = { groupIds ->
                    coroutineScope.launch {
                        val result = onSaveGroups(groupIds)
                        if (result.isSuccess) {
                            multipleGroupsRingtoneInfo =
                                findMultipleGroupsRingtoneInfoAfterAssignments(
                                    contacts = contactState.contacts,
                                    groups = groupState.groups,
                                    assignedEditableGroupIdsByContact = mapOf(contact.id to groupIds),
                                    hasSeenMultipleGroupsRingtoneInfo = hasSeenMultipleGroupsRingtoneInfo
                                )
                            showManageGroupsDialog = false
                        }
                    }
                }
            )
        }

        multipleGroupsRingtoneInfo?.let { info ->
            MultipleGroupsRingtoneInfoDialog(
                info = info,
                onAcknowledge = {
                    multipleGroupsRingtoneInfo = null
                    onMultipleGroupsRingtoneInfoAcknowledged()
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete contact?") },
                text = { Text("This removes ${contact.displayName} from device contacts.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                if (onDeleteContact(contact.id)) {
                                    showDeleteDialog = false
                                    navController.popBackStack()
                                }
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
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
            val context = LocalContext.current
            val photoModel = contact.photoModel(context)
            if (photoModel != null) {
                AsyncImage(
                    model = photoModel,
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
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    type: String?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
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
    group: Group,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = group.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (group.isMembershipEditable) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = stringResource(R.string.contact_remove_from_group)
                )
            }
        }
    }
}

@Composable
private fun ManageContactGroupsDialog(
    groups: List<Group>,
    selectedGroupIds: List<Int>,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit
) {
    val initialSelectedGroupIds = remember(selectedGroupIds) { selectedGroupIds.toSet() }
    val currentSelectedGroupIds = remember(groups, initialSelectedGroupIds) {
        mutableStateListOf<Int>().apply {
            addAll(selectedGroupIds)
        }
    }
    val hasChanges = currentSelectedGroupIds.toSet() != initialSelectedGroupIds

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contacts_manage_groups)) },
        text = {
            LazyColumn {
                items(groups) { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (group.id in currentSelectedGroupIds) {
                                    currentSelectedGroupIds.remove(group.id)
                                } else {
                                    currentSelectedGroupIds.add(group.id)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = group.id in currentSelectedGroupIds,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (group.id !in currentSelectedGroupIds) {
                                        currentSelectedGroupIds.add(group.id)
                                    }
                                } else {
                                    currentSelectedGroupIds.remove(group.id)
                                }
                            },
                            modifier = Modifier.testTag("manage-group-checkbox-${group.id}")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = group.name,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        groups.filter { it.id in currentSelectedGroupIds }
                            .map(Group::id)
                    )
                },
                enabled = hasChanges
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun Contact.hasPersonalDetails(): Boolean {
    return nickname != null || structuredName?.details().orEmpty().isNotEmpty()
}

private fun StructuredName.details(): List<Pair<String, String>> {
    return listOfNotNull(
        givenName?.let { "Given name" to it },
        middleName?.let { "Middle name" to it },
        familyName?.let { "Family name" to it },
        prefix?.let { "Prefix" to it },
        suffix?.let { "Suffix" to it },
        phoneticGivenName?.let { "Phonetic given" to it },
        phoneticMiddleName?.let { "Phonetic middle" to it },
        phoneticFamilyName?.let { "Phonetic family" to it }
    )
}

@Composable
private fun customOrDefault(label: String?, default: String = "Custom"): String {
    return label?.takeIf { it.isNotBlank() } ?: default
}

@Composable
private fun getPhoneTypeLabel(item: ContactDataItem): String {
    return when (item.typeConstant) {
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> stringResource(R.string.contact_type_home)
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> stringResource(R.string.contact_type_work)
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> stringResource(R.string.contact_type_mobile)
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> stringResource(R.string.contact_type_main)
        ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customOrDefault(item.label)
        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getEmailTypeLabel(item: ContactDataItem): String {
    return when (item.typeConstant) {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> stringResource(R.string.contact_type_home)
        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> stringResource(R.string.contact_type_work)
        ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> stringResource(R.string.contact_type_mobile)
        ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM -> customOrDefault(item.label)
        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getAddressTypeLabel(address: Address): String {
    return when (address.typeConstant) {
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> stringResource(R.string.contact_type_home)
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> stringResource(R.string.contact_type_work)
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM -> {
            customOrDefault(address.label)
        }

        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getOrganizationTypeLabel(organization: Organization): String {
    return when (organization.typeConstant) {
        ContactsContract.CommonDataKinds.Organization.TYPE_WORK -> stringResource(R.string.contact_type_work)
        ContactsContract.CommonDataKinds.Organization.TYPE_OTHER -> stringResource(R.string.contact_type_other)
        ContactsContract.CommonDataKinds.Organization.TYPE_CUSTOM -> {
            customOrDefault(organization.label)
        }

        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getWebsiteTypeLabel(website: Website): String {
    return when (website.typeConstant) {
        ContactsContract.CommonDataKinds.Website.TYPE_HOMEPAGE -> "Homepage"
        ContactsContract.CommonDataKinds.Website.TYPE_BLOG -> "Blog"
        ContactsContract.CommonDataKinds.Website.TYPE_PROFILE -> "Profile"
        ContactsContract.CommonDataKinds.Website.TYPE_HOME -> stringResource(R.string.contact_type_home)
        ContactsContract.CommonDataKinds.Website.TYPE_WORK -> stringResource(R.string.contact_type_work)
        ContactsContract.CommonDataKinds.Website.TYPE_CUSTOM -> customOrDefault(website.label)
        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getEventTypeLabel(event: ContactEvent): String {
    return when (event.typeConstant) {
        ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY -> "Anniversary"
        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY -> "Birthday"
        ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM -> customOrDefault(event.label)
        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getRelationTypeLabel(relation: Relation): String {
    return when (relation.typeConstant) {
        ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT -> "Assistant"
        ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER -> "Brother"
        ContactsContract.CommonDataKinds.Relation.TYPE_CHILD -> "Child"
        ContactsContract.CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER -> "Partner"
        ContactsContract.CommonDataKinds.Relation.TYPE_FATHER -> "Father"
        ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND -> "Friend"
        ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER -> "Manager"
        ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER -> "Mother"
        ContactsContract.CommonDataKinds.Relation.TYPE_PARENT -> "Parent"
        ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER -> "Partner"
        ContactsContract.CommonDataKinds.Relation.TYPE_SISTER -> "Sister"
        ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE -> "Spouse"
        ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM -> customOrDefault(relation.label)
        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getImTypeLabel(message: InstantMessage): String {
    return when (message.typeConstant) {
        ContactsContract.CommonDataKinds.Im.TYPE_HOME -> stringResource(R.string.contact_type_home)
        ContactsContract.CommonDataKinds.Im.TYPE_WORK -> stringResource(R.string.contact_type_work)
        ContactsContract.CommonDataKinds.Im.TYPE_OTHER -> stringResource(R.string.contact_type_other)
        ContactsContract.CommonDataKinds.Im.TYPE_CUSTOM -> customOrDefault(message.label)
        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getImProtocolLabel(message: InstantMessage): String {
    return when (message.protocolConstant) {
        ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM -> "AIM"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK -> "Google Talk"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ -> "ICQ"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER -> "Jabber"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN -> "MSN"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING -> "NetMeeting"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ -> "QQ"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE -> "Skype"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO -> "Yahoo"
        ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM -> {
            customOrDefault(message.customProtocol)
        }

        else -> stringResource(R.string.contact_type_other)
    }
}

@Composable
private fun getSipTypeLabel(item: ContactDataItem): String {
    return when (item.typeConstant) {
        ContactsContract.CommonDataKinds.SipAddress.TYPE_HOME -> stringResource(R.string.contact_type_home)
        ContactsContract.CommonDataKinds.SipAddress.TYPE_WORK -> stringResource(R.string.contact_type_work)
        ContactsContract.CommonDataKinds.SipAddress.TYPE_CUSTOM -> customOrDefault(item.label)
        else -> stringResource(R.string.contact_type_other)
    }
}
