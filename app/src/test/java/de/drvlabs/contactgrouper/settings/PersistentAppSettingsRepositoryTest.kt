package de.drvlabs.contactgrouper.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentAppSettingsRepositoryTest {

    @Test
    fun `updates state and saves settings for later repository instances`() {
        val store = InMemoryAppSettingsStore()
        val repository = PersistentAppSettingsRepository(store)

        repository.setPreferNicknameDisplayName(true)
        repository.setAutoSyncDeviceGroupChanges(true)
        repository.setHasSeenMultipleGroupsRingtoneInfo(true)

        val expected = AppSettings(
            preferNicknameDisplayName = true,
            autoSyncDeviceGroupChanges = true,
            hasSeenMultipleGroupsRingtoneInfo = true
        )
        assertEquals(expected, repository.settings.value)
        assertEquals(expected, store.savedSettings)

        val recreatedRepository = PersistentAppSettingsRepository(store)
        assertEquals(expected, recreatedRepository.settings.value)
    }

    @Test
    fun `single setting updates preserve other values`() {
        val repository = PersistentAppSettingsRepository(
            InMemoryAppSettingsStore(
                AppSettings(
                    preferNicknameDisplayName = true,
                    autoSyncDeviceGroupChanges = true,
                    hasSeenMultipleGroupsRingtoneInfo = true
                )
            )
        )

        repository.setAutoSyncDeviceGroupChanges(false)

        assertEquals(
            AppSettings(
                preferNicknameDisplayName = true,
                autoSyncDeviceGroupChanges = false,
                hasSeenMultipleGroupsRingtoneInfo = true
            ),
            repository.settings.value
        )
    }

    @Test
    fun `multiple groups ringtone info acknowledgement persists`() {
        val store = InMemoryAppSettingsStore()
        val repository = PersistentAppSettingsRepository(store)

        assertFalse(repository.settings.value.hasSeenMultipleGroupsRingtoneInfo)

        repository.setHasSeenMultipleGroupsRingtoneInfo(true)

        assertTrue(repository.settings.value.hasSeenMultipleGroupsRingtoneInfo)
        assertTrue(store.savedSettings.hasSeenMultipleGroupsRingtoneInfo)
        assertTrue(
            PersistentAppSettingsRepository(store)
                .settings
                .value
                .hasSeenMultipleGroupsRingtoneInfo
        )
    }

    private class InMemoryAppSettingsStore(
        initialSettings: AppSettings = AppSettings()
    ) : AppSettingsStore {
        var savedSettings: AppSettings = initialSettings

        override fun load(): AppSettings = savedSettings

        override fun save(settings: AppSettings) {
            savedSettings = settings
        }
    }
}
