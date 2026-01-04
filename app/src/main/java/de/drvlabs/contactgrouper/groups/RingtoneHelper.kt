package de.drvlabs.contactgrouper.groups

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Helper utility for applying ringtones to contacts through the Android ContactsContract.
 */
object RingtoneHelper {
    
    /**
     * Check if the app has the WRITE_CONTACTS permission.
     * 
     * @param context Android context
     * @return true if WRITE_CONTACTS permission is granted, false otherwise
     */
    private fun hasWriteContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
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
        // Check if we have write permission
        if (!hasWriteContactsPermission(context)) {
            return false
        }
        
        return try {
            val contentValues = ContentValues().apply {
                // Properly handle null case: use null instead of the string "null"
                if (ringtoneUri != null) {
                    put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri.toString())
                } else {
                    putNull(ContactsContract.Contacts.CUSTOM_RINGTONE)
                }
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
