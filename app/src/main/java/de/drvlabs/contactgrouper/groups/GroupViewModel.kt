package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

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

    private val _groups = dao.getAllGroups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(),emptyList())


    private val _state = MutableStateFlow(GroupState())
    val state = combine(_state,_groups) { state, groups ->
        state.copy(
            groups = groups
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        GroupState())

    fun onEvent(event: GroupEvent){
        when(event){
            is GroupEvent.DeleteGroup -> {
                viewModelScope.launch {
                    dao.deleteGroup(event.group)
                }
            }
            GroupEvent.SaveGroup -> {
                val name = state.value.name
                val ringtoneUri = state.value.ringtoneUri
                val contacts = state.value.contacts


                if (name.isBlank()){
                    return
                }

                val group = ContactGroup(
                    name = name,
                    ringtoneUri = ringtoneUri,
                    contactIds = contacts
                )
                viewModelScope.launch {
                    dao.upsertGroup(group)
                }


            }
            is GroupEvent.SetGroupMembers -> TODO()
            is GroupEvent.SetGroupName -> {
                _state.update {
                    it.copy(
                        name = event.name
                    )
                }
            }
            is GroupEvent.SetRingtoneUri -> {
                _state.update {
                    it.copy(
                        ringtoneUri = event.uri
                    )
                }
            }
            is GroupEvent.SetSelectedGroup -> {
                _state.update {
                    it.copy(
                        selectedGroup = event.group
                    )
                }
            }
        }
    }
}