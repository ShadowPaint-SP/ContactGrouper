package de.drvlabs.contactgrouper.contacts

/**
 * Represents a single piece of typed data for a contact, like a phone number or email.
 *
 * @property value The actual data (e.g., the phone number or email address).
 * @property typeConstant The integer constant from ContactsContract for the type.
 */
data class ContactDataItem(
    val value: String,
    val typeConstant: Int
)

/**
 * Represents a postal address for a contact.
 *
 * @property formattedAddress The full, formatted address string.
 * @property typeConstant The integer constant from ContactsContract for the type.
 */
data class Address(
    val formattedAddress: String,
    val typeConstant: Int
)

/**
 * Represents a comprehensive model of a contact, holding a lot of the data
 * that can be fetched from the Android ContactsContract provider.
 *
 * @property id The unique identifier for the contact.
 * @property displayName The primary name to display for the contact.
 * @property photoUri A string URI for the contact's full-size photo.
 * @property thumbnailUri A string URI for the contact's smaller thumbnail photo.
 * @property customRingtone A string URI for the custom ringtone assigned to this contact.
 * @property nickname An alternative name for the contact.
 *
 * @property phoneNumbers A list of all phone numbers for the contact.
 * @property emails A list of all email addresses for the contact.
 * @property addresses A list of all postal addresses for the contact.
 *
 * @property groupId Internal group ID.
 */
data class Contact(
    // Core Identifiers
    val id: Long,
    val displayName: String,
    val photoUri: String?,
    val thumbnailUri: String?,

    // Status & Settings
    val customRingtone: String?,

    // Detailed Information (as lists of typed data)
    val phoneNumbers: List<ContactDataItem> = emptyList(),
    val emails: List<ContactDataItem> = emptyList(),
    val addresses: List<Address> = emptyList(),

    // Other Fields
    val nickname: String? = null,

    // Your app-specific field
    var groupId: Int? = null
)
