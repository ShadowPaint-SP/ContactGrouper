package de.drvlabs.contactgrouper.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupMembership
import de.drvlabs.contactgrouper.groups.GroupsRepository
import de.drvlabs.contactgrouper.settings.AppSettings
import de.drvlabs.contactgrouper.settings.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ContactsViewModel private constructor(
    private val deleteContact: suspend (Long) -> Boolean,
    contacts: Flow<List<Contact>>,
    groups: Flow<List<Group>>,
    memberships: Flow<List<GroupMembership>>,
    settings: StateFlow<AppSettings>
) : ViewModel() {

    constructor(
        contactsDataSource: ContactsDataSource,
        repository: GroupsRepository,
        settingsRepository: AppSettingsRepository
    ) : this(
        deleteContact = contactsDataSource::deleteContact,
        contacts = contactsDataSource.observeContacts(),
        groups = repository.observeGroups(),
        memberships = repository.observeMemberships(),
        settings = settingsRepository.settings
    )

    internal constructor(
        contacts: Flow<List<Contact>>,
        groups: Flow<List<Group>>,
        memberships: Flow<List<GroupMembership>>,
        settings: StateFlow<AppSettings>
    ) : this(
        deleteContact = { false },
        contacts = contacts,
        groups = groups,
        memberships = memberships,
        settings = settings
    )

    val state: StateFlow<ContactsListState> = combine(
        contacts,
        groups,
        memberships,
        settings
    ) { contacts, groups, memberships, settings ->
        ContactsStateMapper.build(contacts, groups, memberships, settings)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ContactsListState()
    )

    suspend fun deleteContact(contactId: Long): Boolean {
        return deleteContact.invoke(contactId)
    }

    companion object {
        fun factory(
            contactsDataSource: ContactsDataSource,
            repository: GroupsRepository,
            settingsRepository: AppSettingsRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ContactsViewModel(contactsDataSource, repository, settingsRepository) as T
                }
            }
        }
    }
}
