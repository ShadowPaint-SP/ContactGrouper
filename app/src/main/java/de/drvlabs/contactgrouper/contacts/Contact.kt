package de.drvlabs.contactgrouper.contacts

/**
 * Represents a single piece of typed data for a contact, like a phone number or email.
 *
 * @property value The actual data (e.g., the phone number or email address).
 * @property typeConstant The integer constant from ContactsContract for the type.
 */
data class ContactDataItem(
    val value: String,
    val typeConstant: Int,
    val label: String? = null
)

/**
 * Represents a postal address for a contact.
 *
 * @property formattedAddress The full, formatted address string.
 * @property typeConstant The integer constant from ContactsContract for the type.
 */
data class Address(
    val formattedAddress: String,
    val typeConstant: Int,
    val label: String? = null
)

data class StructuredName(
    val givenName: String? = null,
    val middleName: String? = null,
    val familyName: String? = null,
    val prefix: String? = null,
    val suffix: String? = null,
    val phoneticGivenName: String? = null,
    val phoneticMiddleName: String? = null,
    val phoneticFamilyName: String? = null
)

data class Organization(
    val company: String,
    val title: String? = null,
    val department: String? = null,
    val typeConstant: Int,
    val label: String? = null
)

data class Website(
    val url: String,
    val typeConstant: Int,
    val label: String? = null
)

data class ContactEvent(
    val date: String,
    val typeConstant: Int,
    val label: String? = null
)

data class Relation(
    val name: String,
    val typeConstant: Int,
    val label: String? = null
)

data class InstantMessage(
    val handle: String,
    val protocolConstant: Int,
    val customProtocol: String? = null,
    val typeConstant: Int,
    val label: String? = null
)

/**
 * Represents a comprehensive model of a contact, holding a lot of the data
 * that can be fetched from the Android ContactsContract provider.
 *
 * @property id The unique identifier for the contact.
 * @property displayName The primary name to display for the contact.
 * @property photoUri A string URI for the contact's full-size photo.
 * @property thumbnailUri A string URI for the contact's smaller thumbnail photo.
 * @property photoVersion The provider photo identifier used to refresh cached image loads.
 * @property customRingtone A string URI for the custom ringtone assigned to this contact.
 * @property nickname An alternative name for the contact.
 *
 * @property phoneNumbers A list of all phone numbers for the contact.
 * @property emails A list of all email addresses for the contact.
 * @property addresses A list of all postal addresses for the contact.
 *
 * @property groupIds Internal group IDs sorted by latest membership first.
 * @property effectiveRingtoneGroupId Group ID currently controlling the ringtone.
 */
data class Contact(
    // Core Identifiers
    val id: Long,
    val displayName: String,
    val photoUri: String?,
    val thumbnailUri: String?,
    val photoVersion: Long? = null,

    // Status & Settings
    val customRingtone: String?,
    val starred: Boolean = false,
    val sendToVoicemail: Boolean = false,

    // Detailed Information (as lists of typed data)
    val structuredName: StructuredName? = null,
    val phoneNumbers: List<ContactDataItem> = emptyList(),
    val emails: List<ContactDataItem> = emptyList(),
    val addresses: List<Address> = emptyList(),
    val organizations: List<Organization> = emptyList(),
    val websites: List<Website> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val relations: List<Relation> = emptyList(),
    val instantMessages: List<InstantMessage> = emptyList(),
    val sipAddresses: List<ContactDataItem> = emptyList(),
    val notes: List<String> = emptyList(),

    // Other Fields
    val nickname: String? = null,

    // App-specific fields
    val groupIds: List<Int> = emptyList(),
    val effectiveRingtoneGroupId: Int? = null
)
