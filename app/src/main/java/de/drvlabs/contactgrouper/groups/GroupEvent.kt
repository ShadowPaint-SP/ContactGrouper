package de.drvlabs.contactgrouper.groups

import android.net.Uri

sealed interface GroupEvent {
    object SaveGroup: GroupEvent
    data class SetGroupName(val name: String): GroupEvent
    data class SetRingtoneUri(val uri: Uri): GroupEvent
    data class SetGroupMembers(val contacts: List<String>): GroupEvent
    data class DeleteGroup(val group: ContactGroup): GroupEvent
    data class SetSelectedGroup(val group: ContactGroup): GroupEvent
}