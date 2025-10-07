package de.drvlabs.contactgrouper.groups

import android.net.Uri

data class GroupState(
    val groups: List<ContactGroup> = emptyList(),
    val name: String="",
    var ringtoneUri: Uri? = null,
    val contacts: List<String> = emptyList(),
    val selectedGroup: ContactGroup? = null
    )
