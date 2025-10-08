package de.drvlabs.contactgrouper.contacts

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.drvlabs.contactgrouper.groups.GroupState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    navController: NavController,
    contactState: ContactState,
    groupState: GroupState
) {
    val contact = contactState.selectedContact!!//TODO make better
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.displayName) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (contact.groupId != null){
                item {
                    DetailSection(title = "Group") {
                        DetailItem(
                            icon = Icons.Default.Groups,
                            value = groupState.groups.find { it.id == contact.groupId }!!.name,//TODO This is unsafe
                            type =""
                        )
                    }
                }
            }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.headlineMedium
            )
        }
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
private fun DetailItem(icon: ImageVector, value: String, type: String?) {
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

private fun getPhoneTypeLabel(type: Int): String {
    return when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Work Fax"
        ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "Home Fax"
        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
        else -> "Other"
    }
}

private fun getEmailTypeLabel(type: Int): String {
    return when (type) {
        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "Work"
        ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> "Mobile"
        ContactsContract.CommonDataKinds.Email.TYPE_OTHER -> "Other"
        else -> "Other"
    }
}

private fun getAddressTypeLabel(type: Int): String {
    return when (type) {
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME -> "Home"
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> "Work"
        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER -> "Other"
        else -> "Other"
    }
}
