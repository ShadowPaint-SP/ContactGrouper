package de.drvlabs.contactgrouper.settings

data class AppSettings(
    val preferNicknameDisplayName: Boolean = false,
    val autoSyncDeviceGroupChanges: Boolean = false,
    val hasSeenMultipleGroupsRingtoneInfo: Boolean = false
)
