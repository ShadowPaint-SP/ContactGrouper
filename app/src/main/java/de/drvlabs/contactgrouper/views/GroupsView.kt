package de.drvlabs.contactgrouper.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.drvlabs.contactgrouper.Contact

@Composable
fun ContactList(contacts: List<Contact>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        items(contacts) { contact ->
            Text(
                text = contact.name,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp).fillMaxSize()
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.inverseOnSurface)
        }
    }
}