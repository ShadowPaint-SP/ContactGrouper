package de.drvlabs.contactgrouper.groups

interface DeviceGroupSource {
    suspend fun loadSnapshot(): DeviceGroupSnapshot
}
