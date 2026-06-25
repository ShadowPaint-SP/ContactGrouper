package de.drvlabs.contactgrouper.groups

private const val MY_CONTACTS_GROUP_NAME = "My Contacts"

fun isReservedSystemGroupName(name: String): Boolean {
    return name.trim().equals(MY_CONTACTS_GROUP_NAME, ignoreCase = true)
}

fun DeviceGroupSnapshot.withoutReservedSystemGroups(): DeviceGroupSnapshot {
    val reservedDeviceGroupIds = groups
        .filter { isReservedSystemGroupName(it.title) }
        .map(DeviceGroupRecord::deviceGroupId)
        .toSet()

    if (reservedDeviceGroupIds.isEmpty()) {
        return this
    }

    return DeviceGroupSnapshot(
        groups = groups.filter { it.deviceGroupId !in reservedDeviceGroupIds },
        memberships = memberships.filter { it.deviceGroupId !in reservedDeviceGroupIds }
    )
}
