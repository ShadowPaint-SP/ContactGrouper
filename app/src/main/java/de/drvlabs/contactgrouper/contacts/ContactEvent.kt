package de.drvlabs.contactgrouper.contacts

import android.net.Uri

/**
 * Defines all possible user actions (events) that can occur on the contacts screens.
 */
sealed interface ContactEvent {
    /**
     * Event triggered when a user clicks on a contact to view its details.
     * @param contact The Contact to be displayed.
     */
    data class SetSelectContact(val contact: Contact) : ContactEvent

    /**
     * Event triggered when a user clicks on the "Add Contact" button.
     */
    data class SetRingtoneUri(val uri: Uri) : ContactEvent

    /**
     * Event triggered when a contact is removed from a group.
     * Updates the selectedContact to reflect the group change.
     * @param contactId The ID of the contact whose group was removed.
     */
    data class ClearContactGroup(val contactId: Long) : ContactEvent


}
