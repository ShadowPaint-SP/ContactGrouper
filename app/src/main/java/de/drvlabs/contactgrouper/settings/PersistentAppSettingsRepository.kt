package de.drvlabs.contactgrouper.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PersistentAppSettingsRepository(
    private val store: AppSettingsStore
) : AppSettingsRepository {
    private val mutableSettings = MutableStateFlow(store.load())
    override val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    override fun setAutoSyncDeviceGroupChanges(enabled: Boolean) {
        update { it.copy(autoSyncDeviceGroupChanges = enabled) }
    }

    override fun setHasSeenMultipleGroupsRingtoneInfo(seen: Boolean) {
        update { it.copy(hasSeenMultipleGroupsRingtoneInfo = seen) }
    }

    @Synchronized
    private fun update(transform: (AppSettings) -> AppSettings) {
        val nextSettings = transform(mutableSettings.value)
        store.save(nextSettings)
        mutableSettings.value = nextSettings
    }
}
