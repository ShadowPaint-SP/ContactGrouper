package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["deviceGroupId"], unique = true)
    ]
)
data class Group(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val color: Color,
    val ringtoneUri: Uri? = null,
    val syncSource: GroupSyncSource = GroupSyncSource.LOCAL,
    val deviceGroupId: Long? = null,
    val accountName: String? = null,
    val accountType: String? = null,
    val dataSet: String? = null,
    val isReadOnly: Boolean = false,
    val isVisible: Boolean = true
) {
    val isDeviceBacked: Boolean
        get() = syncSource == GroupSyncSource.DEVICE

    val isMembershipEditable: Boolean
        get() = syncSource == GroupSyncSource.LOCAL || !isReadOnly

    val canDelete: Boolean
        get() = syncSource == GroupSyncSource.LOCAL || !isReadOnly

    val deletesFromDevice: Boolean
        get() = deviceGroupId != null
}
