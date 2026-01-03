package de.drvlabs.contactgrouper.groups

import android.net.Uri
import de.drvlabs.contactgrouper.contacts.Contact

sealed interface GroupEvent {
    object SaveGroup: GroupEvent
    data class SetGroupName(val name: String): GroupEvent
    data class SetRingtoneUri(val uri: Uri?): GroupEvent
    data class SetGroupMembers(val contacts: List<Contact>): GroupEvent
    data class RemoveGroupMember(val contact: Contact, val group: Group): GroupEvent
    data class DeleteGroup(val group: Group): GroupEvent
    data class SetSelectedGroup(val group: Group): GroupEvent
    data class AssignContactsToGroup(val groupId: Int, val contactIds: List<Long>): GroupEvent
    data class ChangeGroupRingtone(val groupId: Int, val ringtoneUri: Uri?): GroupEvent
}