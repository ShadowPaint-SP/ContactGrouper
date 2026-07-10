package de.drvlabs.contactgrouper.settings

data class AppSettings(
    val autoSyncDeviceGroupChanges: Boolean = false,
    val hasSeenMultipleGroupsRingtoneInfo: Boolean = false
)
