package de.drvlabs.contactgrouper.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupViewModel(private val dao: GroupDao) : ViewModel() {

    private val _groups =
        dao.getAllGroups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())


    private val _state = MutableStateFlow(GroupState())
    val state = combine(_state, _groups) { state, groups ->
        state.copy(
            groups = groups
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        GroupState()
    )

    fun onEvent(event: GroupEvent) {
        when (event) {
            is GroupEvent.DeleteGroup -> {
                viewModelScope.launch {
                    dao.deleteGroup(event.group)
                }
            }

            GroupEvent.SaveGroup -> {
                val name = state.value.name
                val ringtoneUri = state.value.ringtoneUri
                val contacts = state.value.contacts


                if (name.isBlank()) {
                    return
                }

                val group = Group(
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

            is GroupEvent.AssignContactsToGroup -> {
                viewModelScope.launch {
                    val group = dao.getGroupById(event.groupId).firstOrNull()
                    group?.let {
                        val updatedContacts = it.contactIds.toMutableList()
                        updatedContacts.addAll(event.contactIds)
                        val updatedGroup = it.copy(contactIds = updatedContacts.distinct())
                        dao.upsertGroup(updatedGroup)
                    }
                }
            }
        }
    }
}
