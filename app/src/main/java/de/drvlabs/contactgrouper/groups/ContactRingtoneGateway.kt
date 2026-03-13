package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.ContactsContract

interface ContactRingtoneGateway {
    suspend fun getCurrentRingtone(contactId: Long): String?
    suspend fun applyRingtone(contactId: Long, ringtoneUri: String?): Boolean
}

class AndroidContactRingtoneGateway(
    private val contentResolver: ContentResolver
) : ContactRingtoneGateway {

    override suspend fun getCurrentRingtone(contactId: Long): String? {
        return contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.CUSTOM_RINGTONE),
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CUSTOM_RINGTONE)
                )
            } else {
                null
            }
        }
    }

    override suspend fun applyRingtone(contactId: Long, ringtoneUri: String?): Boolean {
        val values = ContentValues().apply {
            if (ringtoneUri == null) {
                putNull(ContactsContract.Contacts.CUSTOM_RINGTONE)
            } else {
                put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri)
            }
        }

        return contentResolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString())
        ) > 0
    }
}
