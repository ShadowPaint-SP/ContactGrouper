package de.drvlabs.contactgrouper.groups

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/**
 * Helper utility for applying ringtones to contacts through the Android ContactsContract.
 */
object RingtoneHelper {
    
    /**
     * Apply a ringtone to a contact in the system contacts database.
     * 
     * @param context Android context for ContentResolver access
     * @param contactId The unique ID of the contact to update
     * @param ringtoneUri The URI of the ringtone to apply, or null to clear the ringtone
     * @return true if the update was successful, false otherwise
     */
    fun applyRingtoneToContact(
        context: Context,
        contactId: Long,
        ringtoneUri: Uri?
    ): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri?.toString())
            }
            
            val uri = ContactsContract.Contacts.CONTENT_URI
            val selection = "${ContactsContract.Contacts._ID} = ?"
            val selectionArgs = arrayOf(contactId.toString())
            
            val rowsUpdated = context.contentResolver.update(uri, contentValues, selection, selectionArgs)
            rowsUpdated > 0
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Apply a ringtone to multiple contacts in the system contacts database.
     * 
     * @param context Android context for ContentResolver access
     * @param contactIds List of contact IDs to update
     * @param ringtoneUri The URI of the ringtone to apply, or null to clear the ringtone
     * @return Number of contacts successfully updated
     */
    fun applyRingtoneToContacts(
        context: Context,
        contactIds: List<Long>,
        ringtoneUri: Uri?
    ): Int {
        var updateCount = 0
        for (contactId in contactIds) {
            if (applyRingtoneToContact(context, contactId, ringtoneUri)) {
                updateCount++
            }
        }
        return updateCount
    }
    
    /**
     * Clear the ringtone from a contact (set to default system ringtone).
     * 
     * @param context Android context for ContentResolver access
     * @param contactId The unique ID of the contact to update
     * @return true if the update was successful, false otherwise
     */
    fun clearRingtone(context: Context, contactId: Long): Boolean {
        return applyRingtoneToContact(context, contactId, null)
    }
}
