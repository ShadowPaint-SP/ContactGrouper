package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AddGroupViewModel(
    private val repository: GroupsRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(AddGroupState())
    val state = mutableState.asStateFlow()

    private val mutableMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = mutableMessages.asSharedFlow()

    fun setGroupName(name: String) {
        mutableState.update { it.copy(name = name) }
    }

    fun setRingtoneUri(uri: Uri?) {
        mutableState.update { it.copy(ringtoneUri = uri) }
    }

    fun resetDraft() {
        mutableState.value = AddGroupState()
    }

    suspend fun saveGroup(): GroupMutationResult {
        val result = repository.createLocalGroup(
            name = state.value.name,
            ringtoneUri = state.value.ringtoneUri
        )
        if (result.isSuccess) {
            resetDraft()
        } else {
            result.userMessage()?.let { message ->
                mutableMessages.emit(message)
            }
        }
        return result
    }

    companion object {
        fun factory(repository: GroupsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AddGroupViewModel(repository) as T
                }
            }
        }
    }
}
