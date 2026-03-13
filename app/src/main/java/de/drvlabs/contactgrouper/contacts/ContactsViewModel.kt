package de.drvlabs.contactgrouper.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.drvlabs.contactgrouper.groups.GroupMembership
import de.drvlabs.contactgrouper.groups.GroupsRepository
import de.drvlabs.contactgrouper.groups.RingtoneResolution
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ContactsViewModel(
    contactsDataSource: ContactsDataSource,
    repository: GroupsRepository
) : ViewModel() {

    val state: StateFlow<ContactsListState> = combine(
        contactsDataSource.observeContacts(),
        repository.observeGroups(),
        repository.observeMemberships()
    ) { contacts, groups, memberships ->
        val groupsById = groups.associateBy { it.id }
        val membershipsByContact = memberships.groupBy { it.contactId }

        val mappedContacts = contacts.map { contact ->
            val contactMemberships = membershipsByContact[contact.id].orEmpty()
            val orderedGroupIds = contactMemberships
                .sortedByDescending(GroupMembership::assignedAt)
                .map(GroupMembership::groupId)
                .distinct()
            val winningMembership =
                RingtoneResolution.resolveWinningMembership(groupsById, contactMemberships)

            contact.copy(
                groupIds = orderedGroupIds,
                effectiveRingtoneGroupId = winningMembership?.group?.id
            )
        }

        ContactsListState(contacts = mappedContacts)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ContactsListState()
    )

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
