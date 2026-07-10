package de.drvlabs.contactgrouper.settings

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesAppSettingsStore(
    private val sharedPreferences: SharedPreferences
) : AppSettingsStore {
    override fun load(): AppSettings {
        return AppSettings(
            autoSyncDeviceGroupChanges = sharedPreferences.getBoolean(
                KEY_AUTO_SYNC_DEVICE_GROUP_CHANGES,
                false
            ),
            hasSeenMultipleGroupsRingtoneInfo = sharedPreferences.getBoolean(
                KEY_HAS_SEEN_MULTIPLE_GROUPS_RINGTONE_INFO,
                false
            )
        )
    }

    override fun save(settings: AppSettings) {
        sharedPreferences.edit()
            .putBoolean(
                KEY_AUTO_SYNC_DEVICE_GROUP_CHANGES,
                settings.autoSyncDeviceGroupChanges
            )
            .putBoolean(
                KEY_HAS_SEEN_MULTIPLE_GROUPS_RINGTONE_INFO,
                settings.hasSeenMultipleGroupsRingtoneInfo
            )
            .commit()
    }

    companion object {
        private const val PREFERENCES_NAME = "app_settings"
        private const val KEY_AUTO_SYNC_DEVICE_GROUP_CHANGES = "auto_sync_device_group_changes"
        private const val KEY_HAS_SEEN_MULTIPLE_GROUPS_RINGTONE_INFO =
            "has_seen_multiple_groups_ringtone_info"

        fun create(context: Context): SharedPreferencesAppSettingsStore {
            return SharedPreferencesAppSettingsStore(
                context.applicationContext.getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE
                )
            )
        }
    }
}
