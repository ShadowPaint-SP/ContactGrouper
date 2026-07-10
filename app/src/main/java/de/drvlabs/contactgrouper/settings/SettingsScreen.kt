package de.drvlabs.contactgrouper.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.drvlabs.contactgrouper.R

internal const val AUTO_SYNC_DEVICE_GROUPS_SETTING_TAG = "settings-auto-sync-device-group-changes"

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    SettingsScreen(
        settings = settings,
        onAutoSyncDeviceGroupChangesChange = viewModel::setAutoSyncDeviceGroupChanges,
        modifier = modifier
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onAutoSyncDeviceGroupChangesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(
                    start = 24.dp,
                    top = 8.dp,
                    end = 24.dp,
                    bottom = 16.dp
                )
            )
        }
        item {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_auto_sync_device_group_changes),
                summary = stringResource(R.string.settings_auto_sync_device_group_changes_summary),
                checked = settings.autoSyncDeviceGroupChanges,
                onCheckedChange = onAutoSyncDeviceGroupChangesChange,
                testTag = AUTO_SYNC_DEVICE_GROUPS_SETTING_TAG
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ).testTag(testTag)
    )
}
