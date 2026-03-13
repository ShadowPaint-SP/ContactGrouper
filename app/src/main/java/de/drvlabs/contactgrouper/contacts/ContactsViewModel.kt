package de.drvlabs.contactgrouper.contacts

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.drvlabs.contactgrouper.groups.GroupMembership
import de.drvlabs.contactgrouper.groups.GroupsRepository
import de.drvlabs.contactgrouper.groups.RingtoneResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel(
    private val contentResolver: ContentResolver,
    private val repository: GroupsRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(ContactState())
    private val rawContacts = MutableStateFlow<List<Contact>>(emptyList())
    private val groups = repository.observeGroups()
    private val memberships = repository.observeMemberships()

    val contacts: StateFlow<List<Contact>> = combine(
        rawContacts,
        groups,
        memberships
    ) { contacts, groups, memberships ->
        val groupsById = groups.associateBy { it.id }
        val membershipsByContact = memberships.groupBy { it.contactId }

        contacts.map { contact ->
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
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val state = combine(mutableState, contacts) { currentState, contacts ->
        currentState.copy(
            contacts = contacts,
            selectedContact = currentState.selectedContact?.let { selected ->
                contacts.find { it.id == selected.id }
            }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ContactState()
    )

    fun onEvent(event: ContactEvent) {
        when (event) {
            is ContactEvent.SetSelectContact -> {
                mutableState.update {
                    it.copy(selectedContact = event.contact)
                }
            }

            is ContactEvent.SetRingtoneUri -> Unit

            is ContactEvent.ClearContactGroup -> Unit
        }
    }

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            loadContacts()
        }
    }

    init {
        loadContacts()
        contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            contactsObserver
        )
    }

    override fun onCleared() {
        super.onCleared()
        contentResolver.unregisterContentObserver(contactsObserver)
    }

    private fun loadContacts() {
        viewModelScope.launch {
            val contactsList = withContext(Dispatchers.IO) {
                fetchDetailedContacts()
            }
            rawContacts.value = contactsList
        }
    }

    private fun fetchDetailedContacts(): List<Contact> {
        val contactMap = mutableMapOf<Long, Contact>()

        val contactProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.CUSTOM_RINGTONE
        )

        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            contactProjection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            val thumbnailIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            val ringtoneIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CUSTOM_RINGTONE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                contactMap[id] = Contact(
                    id = id,
                    displayName = cursor.getString(nameIndex),
                    photoUri = cursor.getString(photoIndex),
                    thumbnailUri = cursor.getString(thumbnailIndex),
                    customRingtone = cursor.getString(ringtoneIndex)
                )
            }
        }

        if (contactMap.isEmpty()) {
            return emptyList()
        }

        val dataProjection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
        )

        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            dataProjection,
            "${ContactsContract.Data.CONTACT_ID} IN (${contactMap.keys.joinToString(",")})",
            null,
            null
        )?.use { cursor ->
            val contactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val mimeTypeIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
            val data1Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1)
            val data2Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2)
            val formattedAddressIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
            )

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(contactIdIndex)
                val contact = contactMap[contactId] ?: continue
                when (cursor.getString(mimeTypeIndex)) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        val number = cursor.getString(data1Index) ?: continue
                        val typeId = cursor.getInt(data2Index)
                        contactMap[contactId] = contact.copy(
                            phoneNumbers = contact.phoneNumbers + ContactDataItem(number, typeId)
                        )
                    }

                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        val email = cursor.getString(data1Index) ?: continue
                        val typeId = cursor.getInt(data2Index)
                        contactMap[contactId] = contact.copy(
                            emails = contact.emails + ContactDataItem(email, typeId)
                        )
                    }

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        val address = cursor.getString(formattedAddressIndex) ?: continue
                        val typeId = cursor.getInt(data2Index)
                        contactMap[contactId] = contact.copy(
                            addresses = contact.addresses + Address(address, typeId)
                        )
                    }
                }
            }
        }

        return contactMap.values.toList()
    }
}
