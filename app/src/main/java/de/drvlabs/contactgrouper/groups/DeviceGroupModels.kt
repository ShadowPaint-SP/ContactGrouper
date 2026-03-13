package de.drvlabs.contactgrouper.groups

data class DeviceGroupSnapshot(
    val groups: List<DeviceGroupRecord>,
    val memberships: List<DeviceGroupMembershipRecord>
)

data class DeviceGroupRecord(
    val deviceGroupId: Long,
    val title: String,
    val accountName: String?,
    val accountType: String?,
    val dataSet: String?,
    val isReadOnly: Boolean,
    val isVisible: Boolean
)

data class DeviceGroupMembershipRecord(
    val deviceGroupId: Long,
    val contactId: Long
)
