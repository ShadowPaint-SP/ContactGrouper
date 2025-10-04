package de.drvlabs.contactgrouper

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

data class Contact(val name: String)

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
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC"
            )

            cursor?.use {
                val nameIndex =
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                if (nameIndex != -1) {
                    while (it.moveToNext()) {
                        val name = it.getString(nameIndex)
                        contactsList.add(Contact(name))
                    }
                }
            }
            // Use distinctBy to remove duplicates, as before
            _contacts.value = contactsList.distinctBy { it.name }
        }
    }
}