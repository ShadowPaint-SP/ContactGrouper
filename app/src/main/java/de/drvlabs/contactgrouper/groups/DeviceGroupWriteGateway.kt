package de.drvlabs.contactgrouper.groups

import android.content.ContentUris
import android.content.ContentValues
import android.content.ContentResolver
import android.provider.ContactsContract
import kotlinx.coroutines.CancellationException

data class ContactAccount(
    val rawContactId: Long,
    val accountName: String?,
    val accountType: String?,
    val dataSet: String?
)

class ContactAccountLookupCache {
    internal val accountsByContactId = mutableMapOf<Long, List<ContactAccount>>()
}

interface DeviceGroupWriteGateway {
    suspend fun findAccountForContact(
        contactId: Long,
        cache: ContactAccountLookupCache? = null
    ): ContactAccount?

    suspend fun ensureGroup(title: String, account: ContactAccount?): Long?

    suspend fun addContactToGroup(
        contactId: Long,
        group: Group,
        cache: ContactAccountLookupCache? = null
    ): Boolean

    suspend fun removeContactFromGroup(contactId: Long, deviceGroupId: Long): Boolean
    suspend fun deleteGroup(deviceGroupId: Long): Boolean
}

class ContactsContractDeviceGroupWriteGateway(
    private val contentResolver: ContentResolver
) : DeviceGroupWriteGateway {

    override suspend fun findAccountForContact(
        contactId: Long,
        cache: ContactAccountLookupCache?
    ): ContactAccount? {
        val rawContacts = queryRawContacts(contactId, cache)
        return rawContacts
            .sortedWith(
                compareByDescending<ContactAccount> { !it.accountType.isNullOrBlank() }
                    .thenByDescending { !it.accountName.isNullOrBlank() }
            )
            .firstOrNull()
    }

    override suspend fun ensureGroup(title: String, account: ContactAccount?): Long? {
        return providerCall(
            operation = "ensureGroup",
            fallback = null,
            context = mapOf(
                "title" to title,
                "accountName" to account?.accountName,
                "accountType" to account?.accountType,
                "dataSet" to account?.dataSet
            )
        ) {
            findExistingGroupId(title, account)?.let { return@providerCall it }

            val values = ContentValues().apply {
                put(ContactsContract.Groups.TITLE, title)
                put(ContactsContract.Groups.GROUP_VISIBLE, 1)
                account?.accountName?.let { put(ContactsContract.Groups.ACCOUNT_NAME, it) }
                account?.accountType?.let { put(ContactsContract.Groups.ACCOUNT_TYPE, it) }
                account?.dataSet?.let { put(ContactsContract.Groups.DATA_SET, it) }
            }

            val insertedUri = contentResolver.insert(ContactsContract.Groups.CONTENT_URI, values)
            insertedUri?.lastPathSegment?.toLongOrNull()
        }
    }

    override suspend fun addContactToGroup(
        contactId: Long,
        group: Group,
        cache: ContactAccountLookupCache?
    ): Boolean {
        val deviceGroupId = group.deviceGroupId ?: return false
        if (membershipExists(contactId, deviceGroupId)) {
            return true
        }

        val rawContacts = queryRawContacts(contactId, cache)
        val candidateRawContact = selectRawContactForGroup(rawContacts, group) ?: return false

        val values = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, candidateRawContact.rawContactId)
            put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
            )
            put(
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                deviceGroupId
            )
        }

        return providerCall(
            operation = "addContactToGroup",
            fallback = false,
            context = mapOf(
                "contactId" to contactId,
                "deviceGroupId" to deviceGroupId,
                "groupId" to group.id,
                "rawContactId" to candidateRawContact.rawContactId
            )
        ) {
            contentResolver.insert(ContactsContract.Data.CONTENT_URI, values) != null
        }
    }

    override suspend fun removeContactFromGroup(contactId: Long, deviceGroupId: Long): Boolean {
        return providerCall(
            operation = "removeContactFromGroup",
            fallback = false,
            context = mapOf(
                "contactId" to contactId,
                "deviceGroupId" to deviceGroupId
            )
        ) {
            val membershipRowIds = mutableListOf<Long>()
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                """
                ${ContactsContract.Data.MIMETYPE} = ? AND
                ${ContactsContract.Data.CONTACT_ID} = ? AND
                ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?
                """.trimIndent(),
                arrayOf(
                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                    contactId.toString(),
                    deviceGroupId.toString()
                ),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID)
                while (cursor.moveToNext()) {
                    membershipRowIds += cursor.getLong(idIndex)
                }
            }

            membershipRowIds.forEach { rowId ->
                contentResolver.delete(
                    ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, rowId),
                    null,
                    null
                )
            }
            membershipRowIds.isNotEmpty()
        }
    }

    override suspend fun deleteGroup(deviceGroupId: Long): Boolean {
        return providerCall(
            operation = "deleteGroup",
            fallback = false,
            context = mapOf("deviceGroupId" to deviceGroupId)
        ) {
            val rows = contentResolver.delete(
                ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, deviceGroupId),
                null,
                null
            )
            rows > 0
        }
    }

    private fun queryRawContacts(
        contactId: Long,
        cache: ContactAccountLookupCache?
    ): List<ContactAccount> {
        cache?.accountsByContactId?.get(contactId)?.let { return it }

        val accounts = providerCall(
            operation = "queryRawContacts",
            fallback = emptyList(),
            context = mapOf("contactId" to contactId)
        ) {
            val resolvedAccounts = mutableListOf<ContactAccount>()
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts._ID,
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    ContactsContract.RawContacts.ACCOUNT_TYPE,
                    ContactsContract.RawContacts.DATA_SET
                ),
                "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                val rawContactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
                val accountNameIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)
                val accountTypeIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
                val dataSetIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.DATA_SET)

                while (cursor.moveToNext()) {
                    resolvedAccounts += ContactAccount(
                        rawContactId = cursor.getLong(rawContactIdIndex),
                        accountName = cursor.getString(accountNameIndex),
                        accountType = cursor.getString(accountTypeIndex),
                        dataSet = cursor.getString(dataSetIndex)
                    )
                }
            }
            resolvedAccounts
        }
        cache?.accountsByContactId?.put(contactId, accounts)
        return accounts
    }

    private fun membershipExists(contactId: Long, deviceGroupId: Long): Boolean {
        return providerCall(
            operation = "membershipExists",
            fallback = false,
            context = mapOf(
                "contactId" to contactId,
                "deviceGroupId" to deviceGroupId
            )
        ) {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                """
                ${ContactsContract.Data.MIMETYPE} = ? AND
                ${ContactsContract.Data.CONTACT_ID} = ? AND
                ${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?
                """.trimIndent(),
                arrayOf(
                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                    contactId.toString(),
                    deviceGroupId.toString()
                ),
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        }
    }

    private fun findExistingGroupId(title: String, account: ContactAccount?): Long? {
        val selectionParts = mutableListOf(
            "${ContactsContract.Groups.TITLE} = ?",
            "${ContactsContract.Groups.DELETED} = 0"
        )
        val selectionArgs = mutableListOf(title)

        if (account?.accountName == null) {
            selectionParts += "${ContactsContract.Groups.ACCOUNT_NAME} IS NULL"
        } else {
            selectionParts += "${ContactsContract.Groups.ACCOUNT_NAME} = ?"
            selectionArgs += account.accountName
        }

        if (account?.accountType == null) {
            selectionParts += "${ContactsContract.Groups.ACCOUNT_TYPE} IS NULL"
        } else {
            selectionParts += "${ContactsContract.Groups.ACCOUNT_TYPE} = ?"
            selectionArgs += account.accountType
        }

        if (account?.dataSet == null) {
            selectionParts += "${ContactsContract.Groups.DATA_SET} IS NULL"
        } else {
            selectionParts += "${ContactsContract.Groups.DATA_SET} = ?"
            selectionArgs += account.dataSet
        }

        return providerCall(
            operation = "findExistingGroupId",
            fallback = null,
            context = mapOf(
                "title" to title,
                "accountName" to account?.accountName,
                "accountType" to account?.accountType,
                "dataSet" to account?.dataSet
            )
        ) {
            contentResolver.query(
                ContactsContract.Groups.CONTENT_URI,
                arrayOf(ContactsContract.Groups._ID),
                selectionParts.joinToString(" AND "),
                selectionArgs.toTypedArray(),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Groups._ID))
                } else {
                    null
                }
            }
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

internal fun selectRawContactForGroup(
    rawContacts: List<ContactAccount>,
    group: Group
): ContactAccount? {
    val hasScopedAccount = normalizeAccountComponent(group.accountName) != null ||
        normalizeAccountComponent(group.accountType) != null ||
        normalizeAccountComponent(group.dataSet) != null

    return if (hasScopedAccount) {
        rawContacts.firstOrNull { rawContact -> matchesGroupAccount(rawContact, group) }
    } else {
        rawContacts.firstOrNull()
    }
}

internal fun matchesGroupAccount(
    rawContact: ContactAccount,
    group: Group
): Boolean {
    return normalizeAccountComponent(rawContact.accountName) == normalizeAccountComponent(group.accountName) &&
        normalizeAccountComponent(rawContact.accountType) == normalizeAccountComponent(group.accountType) &&
        normalizeAccountComponent(rawContact.dataSet) == normalizeAccountComponent(group.dataSet)
}

private fun normalizeAccountComponent(value: String?): String? {
    return value?.takeUnless { it.isBlank() }
}
