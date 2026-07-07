package de.drvlabs.contactgrouper.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SettingsViewModel(
    private val repository: AppSettingsRepository
) : ViewModel() {
    val settings = repository.settings

    fun setPreferNicknameDisplayName(enabled: Boolean) {
        repository.setPreferNicknameDisplayName(enabled)
    }

    fun setAutoSyncDeviceGroupChanges(enabled: Boolean) {
        repository.setAutoSyncDeviceGroupChanges(enabled)
    }

    companion object {
        fun factory(repository: AppSettingsRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return SettingsViewModel(repository) as T
                }
            }
        }
    }
}
