package de.drvlabs.contactgrouper.contacts

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.drvlabs.contactgrouper.groups.GroupDao
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
    groupDao: GroupDao
) : ViewModel() {
    private val _state = MutableStateFlow(ContactState())

    // 1. Updated StateFlow to use the new DetailedContact class
    private val _rawContacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = combine(
        _rawContacts,
        groupDao.getAllGroups()
    ) { contacts, groups ->
        // Create a map of [Contact ID] -> [Group ID] for efficient lookup.
        // The key is a Long to match the contact ID type.
        // IMPORTANT: Each contact can only be in ONE group. If a contact appears in multiple groups,
        // the FIRST group wins. This ensures data consistency.
        val contactToGroupMap = mutableMapOf<Long, Int>()
        groups.forEach { group ->
            group.contactIds.forEach { contactId ->
                // Only add if not already assigned to another group
                if (!contactToGroupMap.containsKey(contactId)) {
                    contactToGroupMap[contactId] = group.id
                }
            }
        }

        // Map the groupId to each contact. Using .copy() ensures immutability.
        contacts.map { contact ->
            contact.copy(groupId = contactToGroupMap[contact.id])
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val state = combine(_state, contacts) {state, contacts ->
        state.copy(
            contacts = contacts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ContactState()
    )

    fun onEvent(event: ContactEvent) {
        when (event) {
            is ContactEvent.SetSelectContact -> {
                _state.update {
                    it.copy(
                        selectedContact = event.contact
                    )
                }
            }
            is ContactEvent.SetRingtoneUri -> TODO()
            is ContactEvent.ClearContactGroup -> {
                _state.update { state ->
                    state.selectedContact?.let { contact ->
                        if (contact.id == event.contactId) {
                            // Update the selected contact to remove the group assignment
                            state.copy(selectedContact = contact.copy(groupId = null))
                        } else {
                            state
                        }
                    } ?: state
                }
            }
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
            true, // Notify descendants of this URI as well
            contactsObserver
        )
    }

    override fun onCleared() {
        super.onCleared()
        contentResolver.unregisterContentObserver(contactsObserver)
    }

    /**
     * Kicks off the contact loading process on a background thread.
     */
    private fun loadContacts() {
        viewModelScope.launch {
            // Perform heavy cursor operations on the IO dispatcher to avoid blocking the UI.
            val contactsList = withContext(Dispatchers.IO) {
                fetchDetailedContacts()
            }
            _rawContacts.value = contactsList
        }
    }

    /**
     * Fetches a complete list of contacts with all their detailed data from the ContentResolver.
     * This function is designed to be run on a background thread.
     */
    private fun fetchDetailedContacts(): List<Contact> {
        // Use a map to build contacts. This is efficient for adding details from different data rows.
        val contactMap = mutableMapOf<Long, Contact>()

        // === Step 1: Query basic info from the main Contacts table to get a unique list of people ===
        val contactProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.CUSTOM_RINGTONE
        )

        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            contactProjection,
            null,
            null,
            // This sort order ensures correct, case-insensitive alphabetical sorting.
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex =
                cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val thumbnailIndex =
                cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            val ringtoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.CUSTOM_RINGTONE)

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

        if (contactMap.isEmpty()) return emptyList()

        // === Step 2: Query the Data table to get all details (phone, email, etc.) for all contacts at once ===
        val dataProjection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1, // Generic column for number, email, note, etc.
            ContactsContract.Data.DATA2, // Generic column for type (e.g., home, work)
            ContactsContract.Data.DATA4, // For Organization Title
            // Address-specific columns
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
        )

        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            dataProjection,
            // A WHERE clause to only get data for the contacts we found in Step 1.
            "${ContactsContract.Data.CONTACT_ID} IN (${contactMap.keys.joinToString(",")})",
            null,
            null
        )?.use { cursor ->
            // Column indices for faster access in the loop
            val contactIdIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val mimeTypeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Index = cursor.getColumnIndex(ContactsContract.Data.DATA1)
            val data2Index = cursor.getColumnIndex(ContactsContract.Data.DATA2)
            val formattedAddressIndex =
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(contactIdIndex)
                val contact = contactMap[contactId]
                    ?: continue // Skip if contact somehow isn't in our map
                val mimeType = cursor.getString(mimeTypeIndex)

                when (mimeType) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        val number = cursor.getString(data1Index) ?: continue
                        val typeId = cursor.getInt(data2Index)
                        val updatedPhones =
                            contact.phoneNumbers + ContactDataItem(number, typeId)
                        contactMap[contactId] = contact.copy(phoneNumbers = updatedPhones)
                    }

                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        val email = cursor.getString(data1Index) ?: continue
                        val typeId = cursor.getInt(data2Index)
                        val updatedEmails = contact.emails + ContactDataItem(email, typeId)
                        contactMap[contactId] = contact.copy(emails = updatedEmails)
                    }

                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                        val typeId = cursor.getInt(data2Index)
                        val address = Address(
                            formattedAddress = cursor.getString(formattedAddressIndex) ?: "",
                            typeId
                        )
                        val updatedAddresses = contact.addresses + address
                        contactMap[contactId] = contact.copy(addresses = updatedAddresses)
                    }

                    ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                        contactMap[contactId] =
                            contact.copy(nickname = cursor.getString(data1Index))
                    }
                }
            }
        }
        return contactMap.values.toList()
    }
}
