package de.drvlabs.contactgrouper.settings

import kotlinx.coroutines.flow.StateFlow

interface AppSettingsRepository {
    val settings: StateFlow<AppSettings>

    fun setPreferNicknameDisplayName(enabled: Boolean)

    fun setAutoSyncDeviceGroupChanges(enabled: Boolean)

    fun setHasSeenMultipleGroupsRingtoneInfo(seen: Boolean)
}
