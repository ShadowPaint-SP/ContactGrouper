package de.drvlabs.contactgrouper.groups

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GroupViewModel(private val dao: GroupDao, private val context: Context) : ViewModel() {

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
                    color = Color(Random.nextLong(0xFFFFFF)).copy(alpha = 0.5f),
                    contactIds = contacts
                )
                viewModelScope.launch {
                    dao.upsertGroup(group)
                }
            }

            is GroupEvent.SetGroupMembers -> TODO()
            is GroupEvent.RemoveGroupMember -> {
                viewModelScope.launch {
                    event.group.let {
                        val contacts = it.contactIds.toMutableList()
                        contacts.remove(event.contact.id)
                        val updatedGroup = it.copy(contactIds = contacts)
                        dao.upsertGroup(updatedGroup)
                        
                        // Clear the ringtone from the contact since it's no longer in the group
                        RingtoneHelper.clearRingtone(context, event.contact.id)
                    }
                }
            }
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
                    // Get the target group
                    val targetGroup = dao.getGroupById(event.groupId).firstOrNull()
                    targetGroup?.let { group ->
                        // Get all groups
                        val allGroups = state.value.groups
                        
                        // Remove the contact IDs from all other groups
                        allGroups.forEach { otherGroup ->
                            if (otherGroup.id != event.groupId) {
                                // Remove contact IDs from this group
                                val updatedContacts = otherGroup.contactIds.toMutableList()
                                updatedContacts.removeAll(event.contactIds.toSet())
                                if (updatedContacts != otherGroup.contactIds) {
                                    dao.upsertGroup(otherGroup.copy(contactIds = updatedContacts))
                                }
                            }
                        }
                        
                        // Add the contact IDs to the target group
                        val targetContacts = group.contactIds.toMutableList()
                        targetContacts.addAll(event.contactIds)
                        val updatedGroup = group.copy(contactIds = targetContacts.distinct())
                        dao.upsertGroup(updatedGroup)
                        
                        // Apply the group's ringtone to all newly assigned contacts
                        if (group.ringtoneUri != null) {
                            RingtoneHelper.applyRingtoneToContacts(context, event.contactIds, group.ringtoneUri)
                        }
                    }
                }
            }

            is GroupEvent.ChangeGroupRingtone -> {
                viewModelScope.launch {
                    val group = dao.getGroupById(event.groupId).firstOrNull()
                    group?.let {
                        val updatedGroup = it.copy(ringtoneUri = event.ringtoneUri)
                        dao.upsertGroup(updatedGroup)
                        
                        // Update the selectedGroup immediately for UI refresh
                        _state.update { state ->
                            if (state.selectedGroup?.id == event.groupId) {
                                state.copy(selectedGroup = updatedGroup)
                            } else {
                                state
                            }
                        }
                        
                        // Apply the new ringtone to all contacts in the group
                        RingtoneHelper.applyRingtoneToContacts(context, group.contactIds, event.ringtoneUri)
                    }
                }
            }
        }
    }
}
