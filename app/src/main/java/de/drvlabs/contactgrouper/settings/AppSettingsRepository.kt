package de.drvlabs.contactgrouper.settings

import kotlinx.coroutines.flow.StateFlow

interface AppSettingsRepository {
    val settings: StateFlow<AppSettings>

    fun setAutoSyncDeviceGroupChanges(enabled: Boolean)

    fun setHasSeenMultipleGroupsRingtoneInfo(seen: Boolean)
}
