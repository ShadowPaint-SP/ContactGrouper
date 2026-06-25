package de.drvlabs.contactgrouper.contacts

import android.content.ContentResolver
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
            ContactsContract.Contacts.CUSTOM_RINGTONE
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
            val ringtoneIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CUSTOM_RINGTONE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                contactMap[id] = Contact(
                    id = id,
                    displayName = resolveContactDisplayName(
                        primaryName = cursor.getString(primaryNameIndex),
                        displayName = cursor.getString(displayNameIndex),
                        fallbackName = getString(R.string.contact_unknown_name)
                    ),
                    photoUri = cursor.getString(photoIndex),
                    thumbnailUri = cursor.getString(thumbnailIndex),
                    customRingtone = cursor.getString(ringtoneIndex)
                )
            }
        }

        if (contactMap.isEmpty()) {
            return emptyList()
        }

        val targetMimeTypes = listOf(
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
        )
        val dataProjection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
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
                val formattedAddressIndex = cursor.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
                )

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(contactIdIndex)
                    val contact = contactMap[contactId] ?: continue
                    when (cursor.getString(mimeTypeIndex)) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                            val number = cursor.getString(data1Index) ?: continue
                            val typeId = cursor.getInt(data2Index)
                            contactMap[contactId] = contact.copy(
                                phoneNumbers = contact.phoneNumbers + ContactDataItem(number, typeId)
                            )
                        }

                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            val email = cursor.getString(data1Index) ?: continue
                            val typeId = cursor.getInt(data2Index)
                            contactMap[contactId] = contact.copy(
                                emails = contact.emails + ContactDataItem(email, typeId)
                            )
                        }

                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                            val address = cursor.getString(formattedAddressIndex) ?: continue
                            val typeId = cursor.getInt(data2Index)
                            contactMap[contactId] = contact.copy(
                                addresses = contact.addresses + Address(address, typeId)
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
