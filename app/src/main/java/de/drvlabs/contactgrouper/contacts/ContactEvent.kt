package de.drvlabs.contactgrouper.contacts

/**
 * Defines all possible user actions (events) that can occur on the contacts screens.
 */
sealed interface ContactEvent {
    /**
     * Event triggered when a user clicks on a contact to view its details.
     * @param contact The Contact to be displayed.
     */
    data class SetSelectContact(val contact: Contact) : ContactEvent

}
