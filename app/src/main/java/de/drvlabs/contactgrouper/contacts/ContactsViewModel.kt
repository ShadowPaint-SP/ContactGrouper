package de.drvlabs.contactgrouper.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.drvlabs.contactgrouper.groups.GroupsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ContactsViewModel(
    private val contactsDataSource: ContactsDataSource,
    repository: GroupsRepository
) : ViewModel() {

    val state: StateFlow<ContactsListState> = combine(
        contactsDataSource.observeContacts(),
        repository.observeGroups(),
        repository.observeMemberships()
    ) { contacts, groups, memberships ->
        ContactsStateMapper.build(contacts, groups, memberships)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ContactsListState()
    )

    suspend fun deleteContact(contactId: Long): Boolean {
        return contactsDataSource.deleteContact(contactId)
    }

    companion object {
        fun factory(
            contactsDataSource: ContactsDataSource,
            repository: GroupsRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ContactsViewModel(contactsDataSource, repository) as T
                }
            }
        }
    }
}
