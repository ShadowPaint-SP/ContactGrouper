package de.drvlabs.contactgrouper.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import de.drvlabs.contactgrouper.groups.Group
import de.drvlabs.contactgrouper.groups.GroupMembership
import de.drvlabs.contactgrouper.settings.AppSettings
import de.drvlabs.contactgrouper.settings.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `settings changes remap display names without changing provider display name`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val settingsRepository = FakeAppSettingsRepository()
            val viewModelStore = ViewModelStore()
            val contacts = MutableStateFlow(
                listOf(
                    Contact(
                        id = 42L,
                        displayName = "Robert Smith",
                        photoUri = null,
                        thumbnailUri = null,
                        customRingtone = null,
                        nickname = "Bobby"
                    )
                )
            )
            val groups = MutableStateFlow(emptyList<Group>())
            val memberships = MutableStateFlow(emptyList<GroupMembership>())
            val viewModel = ViewModelProvider(
                viewModelStore,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ContactsViewModel(
                            contacts = contacts,
                            groups = groups,
                            memberships = memberships,
                            settings = settingsRepository.settings
                        ) as T
                    }
                }
            )[ContactsViewModel::class.java]
            val collectJob = backgroundScope.launch {
                viewModel.state.collect {}
            }

            try {
                val providerNameState = withTimeout(5_000) {
                    viewModel.state.first { it.contacts.isNotEmpty() }
                }
                assertEquals("Robert Smith", providerNameState.contacts.single().displayName)
                assertEquals("Robert Smith", providerNameState.contacts.single().providerDisplayName)

                settingsRepository.setPreferNicknameDisplayName(true)

                val nicknameState = withTimeout(5_000) {
                    viewModel.state.first {
                        it.contacts.singleOrNull()?.displayName == "Bobby"
                    }
                }
                assertEquals("Bobby", nicknameState.contacts.single().displayName)
                assertEquals("Robert Smith", nicknameState.contacts.single().providerDisplayName)
            } finally {
                collectJob.cancel()
                viewModelStore.clear()
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeAppSettingsRepository(
    initialSettings: AppSettings = AppSettings()
) : AppSettingsRepository {
    private val mutableSettings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<AppSettings> = mutableSettings

    override fun setPreferNicknameDisplayName(enabled: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(preferNicknameDisplayName = enabled)
    }

    override fun setAutoSyncDeviceGroupChanges(enabled: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(autoSyncDeviceGroupChanges = enabled)
    }

    override fun setHasSeenMultipleGroupsRingtoneInfo(seen: Boolean) {
        mutableSettings.value = mutableSettings.value.copy(hasSeenMultipleGroupsRingtoneInfo = seen)
    }
}
