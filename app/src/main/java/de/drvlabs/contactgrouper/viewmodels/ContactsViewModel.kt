package de.drvlabs.contactgrouper.viewmodels

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Contact(
    val id: Int,
    val name: String,
    val photoUri: String?,
    val groupId: Int? = null // null means it's not in any group
)

class ContactsViewModel(private val contentResolver: ContentResolver) : ViewModel() {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

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

    private fun loadContacts() {
        viewModelScope.launch {
            val contactsList = mutableListOf<Contact>()
            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )

            cursor?.use {

                val idIndex = it.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val nameIndex =
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val photoUriIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                if (nameIndex != -1) {
                    while (it.moveToNext()) {
                        val id = it.getString(idIndex).toInt()
                        val name = it.getString(nameIndex)
                        val photoUri = it.getString(photoUriIndex)
                        contactsList.add(Contact(id = id, name = name, photoUri = photoUri, groupId = null))
                    }
                }
            }
            _contacts.value = contactsList.distinctBy { it.name }
        }
    }
}