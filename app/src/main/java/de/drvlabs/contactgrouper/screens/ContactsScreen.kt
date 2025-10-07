package de.drvlabs.contactgrouper.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.drvlabs.contactgrouper.viewmodels.Contact
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun ContactsMainScreen(contacts: List<Contact>, modifier: Modifier = Modifier) {
    ContactsList(contacts, modifier)
}

@Composable
fun ContactsList(contacts: List<Contact>, modifier: Modifier = Modifier){
    val groupedContacts = contacts.sortedBy { it.name }.groupBy { it.name.first().uppercase() }

    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer).padding(top = 12.dp)) {
        groupedContacts.forEach { (initial, contactsInGroup) ->
            stickyHeader {
                Text(
                    text = initial,
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(start = 32.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Spacer(modifier = modifier.size(8.dp))
            }

            itemsIndexed(contactsInGroup) { index, contact ->
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

                val bottomPadding = if (index < contactsInGroup.size - 1) 2.dp else 0.dp

                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = bottomPadding)
                ) {
                    Card(
                        shape = cardShape,
                        modifier = modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    ) {
                        Row(
                            modifier = modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (contact.photoUri != null) {
                                    AsyncImage(
                                        model = contact.photoUri,
                                        contentDescription = contact.name,
                                        modifier = modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = initial,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = modifier.width(16.dp))

                            Text(
                                text = contact.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = modifier.size(8.dp))
            }
        }
    }
}