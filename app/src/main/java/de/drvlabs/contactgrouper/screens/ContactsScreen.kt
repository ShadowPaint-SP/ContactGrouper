package de.drvlabs.contactgrouper.screens

import androidx.compose.foundation.clickable
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
import de.drvlabs.contactgrouper.viewmodels.Contact

@Composable
fun ContactsMainScreen(contacts: List<Contact>, modifier: Modifier = Modifier) {
    LazyColumn() {
        items(contacts) { contact ->
            Text(
                text = contact.name,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = modifier.clickable{}.padding( 16.dp).fillMaxSize()
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.inverseOnSurface)
        }
    }
}


//
//        package de.drvlabs.contactgrouper.screens
//
//            import androidx.compose.foundation.clickable
//            import androidx.compose.foundation.layout.Arrangement
//            import androidx.compose.foundation.layout.Row
//            import androidx.compose.foundation.layout.fillMaxSize
//            import androidx.compose.foundation.layout.padding
//            import androidx.compose.foundation.lazy.LazyColumn
//            import androidx.compose.foundation.lazy.items
//            import androidx.compose.material3.Checkbox
//            import androidx.compose.material3.HorizontalDivider
//            import androidx.compose.material3.MaterialTheme
//            import androidx.compose.material3.Text
//            import androidx.compose.runtime.Composable
//            import androidx.compose.runtime.getValue
//            import androidx.compose.runtime.mutableStateOf
//            import androidx.compose.runtime.remember
//            import androidx.compose.runtime.setValue
//            import androidx.compose.ui.Alignment
//            import androidx.compose.ui.Modifier
//            import androidx.compose.ui.text.font.FontWeight
//            import androidx.compose.ui.unit.dp
//            import de.drvlabs.contactgrouper.viewmodels.Contact
//
//            data class SelectionList(
//        val contact: Contact,
//        var isSelected: Boolean
//    )
//        @Composable
//        fun ContactsMainScreen(contacts: List<Contact>, modifier: Modifier = Modifier) {
//            contacts.map { SelectionList(
//            contact = it,
//            isSelected = false
//        ) }
//    ) }
//
//    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
//        items(selectedContacts) { contact ->
//            Row(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(vertical = 16.dp)
//                    .clickable{
//                        selectedContacts = selectedContacts.mapIndexed { index, list ->
//                            if (index == selectedContacts.indexOf(contact)) {
//                                list.copy(isSelected = !list.isSelected)
//                            } else list
//                        }
//                    },
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = contact.contact.name,
//                    color = MaterialTheme.colorScheme.secondary,
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.fillMaxSize()
//                )
//                Checkbox(
//                    checked = contact.isSelected,
//                    enabled = false,
//                    onCheckedChange = {contact.isSelected = !contact.isSelected})
//            }
//            HorizontalDivider(color = MaterialTheme.colorScheme.inverseOnSurface)
//        }
//    }
//}