package de.drvlabs.contactgrouper.groups

import android.content.ContentResolver
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
            ${ContactsContract.Groups.GROUP_VISIBLE} = 1 AND
            ${ContactsContract.Groups.TITLE} IS NOT NULL AND
            TRIM(${ContactsContract.Groups.TITLE}) != ''
            """.trimIndent(),
            null,
            "${ContactsContract.Groups.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Groups._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(ContactsContract.Groups.TITLE)
            val accountNameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Groups.ACCOUNT_NAME)
            val accountTypeIndex = cursor.getColumnIndexOrThrow(ContactsContract.Groups.ACCOUNT_TYPE)
            val dataSetIndex = cursor.getColumnIndexOrThrow(ContactsContract.Groups.DATA_SET)
            val readOnlyIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.Groups.GROUP_IS_READ_ONLY)
            val visibleIndex = cursor.getColumnIndexOrThrow(ContactsContract.Groups.GROUP_VISIBLE)

            while (cursor.moveToNext()) {
                groups += DeviceGroupRecord(
                    deviceGroupId = cursor.getLong(idIndex),
                    title = cursor.getString(titleIndex),
                    accountName = cursor.getString(accountNameIndex),
                    accountType = cursor.getString(accountTypeIndex),
                    dataSet = cursor.getString(dataSetIndex),
                    isReadOnly = cursor.getInt(readOnlyIndex) != 0,
                    isVisible = cursor.getInt(visibleIndex) != 0
                )
            }
        }

        if (groups.isEmpty()) {
            return DeviceGroupSnapshot(emptyList(), emptyList())
        }

        val visibleDeviceGroupIds = groups.map { it.deviceGroupId }.toSet()
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
                if (deviceGroupId !in visibleDeviceGroupIds) {
                    continue
                }
                memberships += DeviceGroupMembershipRecord(
                    deviceGroupId = deviceGroupId,
                    contactId = cursor.getLong(contactIdIndex)
                )
            }
        }

        return DeviceGroupSnapshot(
            groups = groups,
            memberships = memberships.distinct()
        )
    }
}
