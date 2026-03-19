package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.ContactsContract
import kotlinx.coroutines.CancellationException

interface ContactRingtoneGateway {
    suspend fun getCurrentRingtone(contactId: Long): String?
    suspend fun applyRingtone(contactId: Long, ringtoneUri: String?): Boolean
}

class AndroidContactRingtoneGateway(
    private val contentResolver: ContentResolver
) : ContactRingtoneGateway {

    override suspend fun getCurrentRingtone(contactId: Long): String? {
        return providerCall(
            operation = "getCurrentRingtone",
            fallback = null,
            context = mapOf("contactId" to contactId)
        ) {
            contentResolver.query(
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
    }

    override suspend fun applyRingtone(contactId: Long, ringtoneUri: String?): Boolean {
        val values = ContentValues().apply {
            if (ringtoneUri == null) {
                putNull(ContactsContract.Contacts.CUSTOM_RINGTONE)
            } else {
                put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri)
            }
        }

        return providerCall(
            operation = "applyRingtone",
            fallback = false,
            context = mapOf(
                "contactId" to contactId,
                "ringtoneUri" to ringtoneUri
            )
        ) {
            contentResolver.update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId.toString())
            ) > 0
        }
    }

    private inline fun <T> providerCall(
        operation: String,
        fallback: T,
        context: Map<String, Any?> = emptyMap(),
        block: () -> T
    ): T {
        return try {
            block()
        } catch (throwable: Throwable) {
            when (throwable) {
                is CancellationException -> throw throwable
                is SecurityException -> throw throwable
                is Exception -> {
                    GroupSyncDiagnostics.reportFailure(operation, throwable, context)
                    fallback
                }

                else -> throw throwable
            }
        }
    }
}
