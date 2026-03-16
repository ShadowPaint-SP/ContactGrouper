package de.drvlabs.contactgrouper.contacts

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class ContactsDataSource(
    private val contentResolver: ContentResolver
) {
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun observeContacts(): Flow<List<Contact>> {
        return callbackFlow {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    trySend(Unit)
                }
            }

            trySend(Unit)
            contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer
            )
            contentResolver.registerContentObserver(
                ContactsContract.Data.CONTENT_URI,
                true,
                observer
            )

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
            .conflate()
            .debounce(300)
            .mapLatest {
                withContext(Dispatchers.IO) {
                    fetchDetailedContacts()
                }
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

        val targetMimeTypes = listOf(
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
        )
        val dataProjection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
        )

        contactMap.keys.toList().chunked(200).forEach { chunk ->
            val contactPlaceholders = chunk.joinToString(",") { "?" }
            val mimePlaceholders = targetMimeTypes.joinToString(",") { "?" }
            val selection = buildString {
                append("${ContactsContract.Data.CONTACT_ID} IN ($contactPlaceholders)")
                append(" AND ")
                append("${ContactsContract.Data.MIMETYPE} IN ($mimePlaceholders)")
            }
            val selectionArgs = chunk.map(Long::toString) + targetMimeTypes

            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                dataProjection,
                selection,
                selectionArgs.toTypedArray(),
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
        }

        return contactMap.values.toList()
    }
}
