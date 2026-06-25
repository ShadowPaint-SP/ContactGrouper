package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract

class ContactsContractDeviceGroupSource(
    private val contentResolver: ContentResolver
) : DeviceGroupSource {

    override suspend fun loadSnapshot(): DeviceGroupSnapshot {
        val groups = mutableListOf<DeviceGroupRecord>()
        val memberships = mutableListOf<DeviceGroupMembershipRecord>()

        val groupProjection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.ACCOUNT_NAME,
            ContactsContract.Groups.ACCOUNT_TYPE,
            ContactsContract.Groups.DATA_SET,
            ContactsContract.Groups.GROUP_IS_READ_ONLY,
            ContactsContract.Groups.GROUP_VISIBLE,
            ContactsContract.Groups.DELETED
        )

        contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            groupProjection,
            """
            ${ContactsContract.Groups.DELETED} = 0 AND
            ${ContactsContract.Groups.TITLE} IS NOT NULL
            """.trimIndent(),
            null,
            "${ContactsContract.Groups.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Groups._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(ContactsContract.Groups.TITLE)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIndex).orEmpty().trim()
                if (title.isBlank()) {
                    continue
                }

                groups += DeviceGroupRecord(
                    deviceGroupId = cursor.getLong(idIndex),
                    title = title,
                    accountName = cursor.getOptionalString(ContactsContract.Groups.ACCOUNT_NAME),
                    accountType = cursor.getOptionalString(ContactsContract.Groups.ACCOUNT_TYPE),
                    dataSet = cursor.getOptionalString(ContactsContract.Groups.DATA_SET),
                    isReadOnly = cursor.getOptionalBoolean(
                        ContactsContract.Groups.GROUP_IS_READ_ONLY,
                        defaultValue = false
                    ),
                    isVisible = cursor.getOptionalBoolean(
                        ContactsContract.Groups.GROUP_VISIBLE,
                        defaultValue = true
                    )
                )
            }
        }

        if (groups.isEmpty()) {
            return DeviceGroupSnapshot(emptyList(), emptyList())
        }

        val visibleGroups = groups.filterNot { isReservedSystemGroupName(it.title) }
        if (visibleGroups.isEmpty()) {
            return DeviceGroupSnapshot(emptyList(), emptyList())
        }

        val importedDeviceGroupIds = visibleGroups.map { it.deviceGroupId }.toSet()
        val membershipProjection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
        )

        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            membershipProjection,
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            val contactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val groupRowIdIndex = cursor.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
            )

            while (cursor.moveToNext()) {
                val deviceGroupId = cursor.getLong(groupRowIdIndex)
                if (deviceGroupId !in importedDeviceGroupIds) {
                    continue
                }
                memberships += DeviceGroupMembershipRecord(
                    deviceGroupId = deviceGroupId,
                    contactId = cursor.getLong(contactIdIndex)
                )
            }
        }

        return DeviceGroupSnapshot(
            groups = visibleGroups,
            memberships = memberships.distinct()
        )
    }
}

private fun Cursor.getOptionalString(columnName: String): String? {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) {
        return null
    }
    return getString(index)
}

private fun Cursor.getOptionalBoolean(
    columnName: String,
    defaultValue: Boolean
): Boolean {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) {
        return defaultValue
    }
    return getInt(index) != 0
}
