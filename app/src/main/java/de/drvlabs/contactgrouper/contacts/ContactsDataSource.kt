@file:Suppress("DEPRECATION")

package de.drvlabs.contactgrouper.contacts

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import de.drvlabs.contactgrouper.AppError
import de.drvlabs.contactgrouper.AppErrorOrigin
import de.drvlabs.contactgrouper.AppErrorReporter
import de.drvlabs.contactgrouper.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
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
    private val contentResolver: ContentResolver,
    private val appErrorReporter: AppErrorReporter = AppErrorReporter(),
    private val getString: (Int) -> String = { "" },
    private val debounceMillis: Long = 300L,
    private val contactsLoader: (() -> List<Contact>)? = null,
    private val contentObserverFactory: (() -> Unit) -> ContentObserver = { onChange ->
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                onChange()
            }
        }
    },
    private val registerObservers: (ContentObserver) -> Unit = { observer ->
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
    },
    private val unregisterObservers: (ContentObserver) -> Unit = { observer ->
        contentResolver.unregisterContentObserver(observer)
    }
) {
    private val initialLoadSucceeded = AtomicBoolean(false)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun observeContacts(): Flow<List<Contact>> {
        return callbackFlow {
            val observer = contentObserverFactory {
                trySend(Unit)
            }

            trySend(Unit)
            try {
                registerObservers(observer)
            } catch (throwable: Throwable) {
                handleLoadFailure(
                    throwable = throwable,
                    operation = "registerObservers"
                )
            }

            awaitClose {
                runCatching {
                    unregisterObservers(observer)
                }
            }
        }
            .conflate()
            .debounce(debounceMillis)
            .mapLatest {
                withContext(Dispatchers.IO) {
                    try {
                        (contactsLoader ?: ::fetchDetailedContacts).invoke().also {
                            initialLoadSucceeded.set(true)
                        }
                    } catch (throwable: Throwable) {
                        handleLoadFailure(
                            throwable = throwable,
                            operation = "fetchDetailedContacts"
                        )
                        emptyList()
                    }
                }
            }
    }

    suspend fun deleteContact(contactId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.delete(
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
                null,
                null
            ) > 0
        } catch (throwable: Throwable) {
            when (throwable) {
                is CancellationException -> throw throwable
                is SecurityException,
                is Exception -> {
                    appErrorReporter.report(
                        AppError.runtimeUnexpected(
                            origin = AppErrorOrigin.ContactsImport,
                            title = "Delete Contact Failed",
                            userMessage = "The app could not delete this contact.",
                            throwable = throwable,
                            heading = "Deleting a contact failed.",
                            context = mapOf("contactId" to contactId.toString())
                        )
                    )
                    false
                }

                else -> throw throwable
            }
        }
    }

    private fun handleLoadFailure(
        throwable: Throwable,
        operation: String
    ) {
        when (throwable) {
            is CancellationException -> throw throwable
            is SecurityException,
            is Exception -> {
                val isStartupFailure = !initialLoadSucceeded.get()
                appErrorReporter.report(
                    if (isStartupFailure) {
                        AppError.startupFatal(
                            origin = AppErrorOrigin.ContactsImport,
                            title = getString(R.string.app_error_start_failed_title),
                            userMessage = getString(R.string.app_error_contacts_startup_message),
                            throwable = throwable,
                            heading = getString(R.string.app_error_contacts_startup_heading),
                            context = mapOf("operation" to operation)
                        )
                    } else {
                        AppError.runtimeUnexpected(
                            origin = AppErrorOrigin.ContactsImport,
                            title = getString(R.string.app_error_contacts_refresh_failed_title),
                            userMessage = getString(R.string.app_error_contacts_refresh_message),
                            throwable = throwable,
                            heading = getString(R.string.app_error_contacts_refresh_heading),
                            context = mapOf("operation" to operation)
                        )
                    }
                )
            }

            else -> throw throwable
        }
    }

    private fun fetchDetailedContacts(): List<Contact> {
        val contactMap = mutableMapOf<Long, Contact>()

        val contactProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.PHOTO_FILE_ID,
            ContactsContract.Contacts.CUSTOM_RINGTONE,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.SEND_TO_VOICEMAIL
        )

        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            contactProjection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val primaryNameIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val displayNameIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
            val thumbnailIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            val photoIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID)
            val photoFileIdIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_FILE_ID)
            val ringtoneIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CUSTOM_RINGTONE)
            val starredIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)
            val voicemailIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.SEND_TO_VOICEMAIL)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val providerDisplayName = resolveContactDisplayName(
                    primaryName = cursor.getString(primaryNameIndex),
                    displayName = cursor.getString(displayNameIndex),
                    fallbackName = getString(R.string.contact_unknown_name)
                )
                contactMap[id] = Contact(
                    id = id,
                    displayName = providerDisplayName,
                    providerDisplayName = providerDisplayName,
                    photoUri = cursor.getString(photoIndex),
                    thumbnailUri = cursor.getString(thumbnailIndex),
                    photoVersion = cursor.getNullableLong(photoFileIdIndex)
                        ?: cursor.getNullableLong(photoIdIndex),
                    customRingtone = cursor.getString(ringtoneIndex),
                    starred = cursor.getInt(starredIndex) == 1,
                    sendToVoicemail = cursor.getInt(voicemailIndex) == 1
                )
            }
        }

        if (contactMap.isEmpty()) {
            return emptyList()
        }

        val targetMimeTypes = listOf(
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
        )
        val dataProjection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA3,
            ContactsContract.Data.DATA4,
            ContactsContract.Data.DATA5,
            ContactsContract.Data.DATA6,
            ContactsContract.Data.DATA7,
            ContactsContract.Data.DATA8,
            ContactsContract.Data.DATA9,
            ContactsContract.Data.DATA10,
            ContactsContract.Data.DATA11,
            ContactsContract.Data.DATA12,
            ContactsContract.Data.DATA13,
            ContactsContract.Data.DATA14,
            ContactsContract.Data.DATA15
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
                val data3Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3)
                val data4Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA4)
                val data5Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA5)
                val data6Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA6)
                val data7Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA7)
                val data8Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA8)
                val data9Index = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA9)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(contactIdIndex)
                    val contact = contactMap[contactId] ?: continue
                    val data1 = cursor.getTrimmedString(data1Index)
                    val typeId = cursor.getInt(data2Index)
                    val label = cursor.getTrimmedString(data3Index)
                    when (cursor.getString(mimeTypeIndex)) {
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                            contactMap[contactId] = contact.copy(
                                structuredName = StructuredName(
                                    givenName = cursor.getTrimmedString(data2Index),
                                    familyName = cursor.getTrimmedString(data3Index),
                                    prefix = cursor.getTrimmedString(data4Index),
                                    middleName = cursor.getTrimmedString(data5Index),
                                    suffix = cursor.getTrimmedString(data6Index),
                                    phoneticGivenName = cursor.getTrimmedString(data7Index),
                                    phoneticMiddleName = cursor.getTrimmedString(data8Index),
                                    phoneticFamilyName = cursor.getTrimmedString(data9Index)
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                            contactMap[contactId] = contact.copy(nickname = data1)
                        }

                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            val number = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                phoneNumbers = contact.phoneNumbers + ContactDataItem(
                                    value = number,
                                    typeConstant = typeId,
                                    label = label
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            val email = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                emails = contact.emails + ContactDataItem(
                                    value = email,
                                    typeConstant = typeId,
                                    label = label
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                            val address = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                addresses = contact.addresses + Address(
                                    formattedAddress = address,
                                    typeConstant = typeId,
                                    label = label
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                            val company = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                organizations = contact.organizations + Organization(
                                    company = company,
                                    typeConstant = typeId,
                                    label = label,
                                    title = cursor.getTrimmedString(data4Index),
                                    department = cursor.getTrimmedString(data5Index)
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                            val url = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                websites = contact.websites + Website(
                                    url = url,
                                    typeConstant = typeId,
                                    label = label
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                            val date = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                events = contact.events + ContactEvent(
                                    date = date,
                                    typeConstant = typeId,
                                    label = label
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE -> {
                            val name = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                relations = contact.relations + Relation(
                                    name = name,
                                    typeConstant = typeId,
                                    label = label
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                            val handle = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                instantMessages = contact.instantMessages + InstantMessage(
                                    handle = handle,
                                    protocolConstant = cursor.getInt(data5Index),
                                    customProtocol = cursor.getTrimmedString(data6Index),
                                    typeConstant = typeId,
                                    label = label
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE -> {
                            val address = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                sipAddresses = contact.sipAddresses + ContactDataItem(
                                    value = address,
                                    typeConstant = typeId,
                                    label = label
                                )
                            )
                        }

                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                            val note = data1 ?: continue
                            contactMap[contactId] = contact.copy(
                                notes = contact.notes + note
                            )
                        }
                    }
                }
            }
        }

        return contactMap.values.toList()
    }
}

internal fun resolveContactDisplayName(
    primaryName: String?,
    displayName: String?,
    fallbackName: String
): String {
    return primaryName.toTrimmedStringOrNull()
        ?: displayName.toTrimmedStringOrNull()
        ?: fallbackName
}

private fun String?.toTrimmedStringOrNull(): String? {
    return this
        ?.trim()
        ?.takeUnless { it.isEmpty() }
}

private fun android.database.Cursor.getTrimmedString(columnIndex: Int): String? {
    return getString(columnIndex).toTrimmedStringOrNull()
}

private fun android.database.Cursor.getNullableLong(columnIndex: Int): Long? {
    return if (isNull(columnIndex)) null else getLong(columnIndex)
}
