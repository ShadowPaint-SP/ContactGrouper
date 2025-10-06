package de.drvlabs.contactgrouper.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import androidx.room.TypeConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@Entity
data class ContactGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val ringtoneUri: Uri? = null,
    val contactIds: List<String>
)

class Converters {

    @TypeConverter
    fun fromString(value: String?): Uri? {
        return value?.toUri()
    }

    @TypeConverter
    fun uriToString(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun fromString(value: String): List<String> {
        return value.split(",").map { it.trim() }
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}
class GroupViewModel(private val dao: GroupDao) : ViewModel() {

    // Lese alle Gruppen als StateFlow aus der Datenbank.
    // Deine UI kann diesen Flow beobachten und aktualisiert sich automatisch.
    val allContactGroups: StateFlow<List<ContactGroup>> = dao.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun getGroupById(groupId: Int){
        viewModelScope.launch {
            dao.getGroupById(groupId)
        }
    }


    // Funktion zum Hinzufügen einer neuen Gruppe
    fun addContactGroup(name: String, contacts: List<String>) {
        val newGroup = ContactGroup(name = name, contactIds = contacts)
        // Starte eine Coroutine, um die Einfüge-Operation im Hintergrund auszuführen
        viewModelScope.launch {
            dao.upsertGroup(newGroup)
        }
    }

    // Ähnliche Funktionen für update und delete...
    fun deleteContactGroup(group: ContactGroup) {
        viewModelScope.launch {
            dao.deleteGroup(group)
        }
    }
}