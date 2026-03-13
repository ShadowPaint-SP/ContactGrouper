package de.drvlabs.contactgrouper.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupViewModel(
    private val repository: GroupsRepository
) : ViewModel() {

    private val groupsFlow =
        repository.observeGroups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val draftState = MutableStateFlow(GroupState())

    val state = combine(draftState, groupsFlow) { draft, groups ->
        draft.copy(
            groups = groups,
            selectedGroup = draft.selectedGroup?.let { selected ->
                groups.find { it.id == selected.id }
            }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GroupState()
    )

    fun onEvent(event: GroupEvent) {
        when (event) {
            GroupEvent.SaveGroup -> {
                val name = state.value.name.trim()
                if (name.isBlank()) {
                    return
                }

                viewModelScope.launch {
                    repository.createLocalGroup(name, state.value.ringtoneUri)
                    draftState.update {
                        it.copy(name = "", ringtoneUri = null)
                    }
                }
            }

            is GroupEvent.SetGroupName -> {
                draftState.update { it.copy(name = event.name) }
            }

            is GroupEvent.SetRingtoneUri -> {
                draftState.update { it.copy(ringtoneUri = event.uri) }
            }

            is GroupEvent.SetSelectedGroup -> {
                draftState.update { it.copy(selectedGroup = event.group) }
            }

            is GroupEvent.AssignContactsToGroups -> {
                viewModelScope.launch {
                    repository.assignContactsToGroups(event.groupIds, event.contactIds)
                }
            }

            is GroupEvent.RemoveGroupMember -> {
                viewModelScope.launch {
                    repository.removeContactFromGroup(event.group.id, event.contact.id)
                }
            }

            is GroupEvent.ChangeGroupRingtone -> {
                viewModelScope.launch {
                    repository.changeGroupRingtone(event.groupId, event.ringtoneUri)
                }
            }

            is GroupEvent.DeleteGroup -> {
                viewModelScope.launch {
                    repository.deleteGroup(event.group.id)
                    draftState.update { current ->
                        if (current.selectedGroup?.id == event.group.id) {
                            current.copy(selectedGroup = null)
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }
}
