package de.drvlabs.contactgrouper.settings

interface AppSettingsStore {
    fun load(): AppSettings

    fun save(settings: AppSettings)
}
