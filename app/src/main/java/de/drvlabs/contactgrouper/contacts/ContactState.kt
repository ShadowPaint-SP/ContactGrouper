package de.drvlabs.contactgrouper.contacts

data class ContactState(
    val contacts: List<Contact> = emptyList(),
    val selectedContact: Contact? = null
)
