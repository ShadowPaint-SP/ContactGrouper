package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.drvlabs.contactgrouper.groups.GroupMutationResult.Success
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GroupViewModel(
    private val repository: GroupsRepository
) : ViewModel() {

    val state: StateFlow<GroupsListState> = repository.observeGroups()
        .map(::GroupsListState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GroupsListState()
        )

    private val mutableMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = mutableMessages.asSharedFlow()

    suspend fun assignContactsToGroups(
        groupIds: List<Int>,
        contactIds: List<Long>
    ): GroupMutationResult {
        return handleResult(repository.assignContactsToGroups(groupIds, contactIds))
    }

    suspend fun setContactGroups(
        contactId: Long,
        groupIds: List<Int>
    ): GroupMutationResult {
        return handleResult(repository.setContactGroups(contactId, groupIds))
    }

    suspend fun removeContactFromGroup(
        groupId: Int,
        contactId: Long
    ): GroupMutationResult {
        return handleResult(repository.removeContactFromGroup(groupId, contactId))
    }

    suspend fun changeGroupRingtone(
        groupId: Int,
        ringtoneUri: Uri?
    ): GroupMutationResult {
        return handleResult(repository.changeGroupRingtone(groupId, ringtoneUri))
    }

    suspend fun deleteGroup(groupId: Int): GroupMutationResult {
        return handleResult(repository.deleteGroup(groupId))
    }

    companion object {
        fun factory(repository: GroupsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GroupViewModel(repository) as T
                }
            }
        }
    }

    private suspend fun handleResult(result: GroupMutationResult): GroupMutationResult {
        if (result != Success) {
            result.userMessage()?.let { message ->
                mutableMessages.emit(message)
            }
        }
        return result
    }
}
