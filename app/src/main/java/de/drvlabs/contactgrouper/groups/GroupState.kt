package de.drvlabs.contactgrouper.groups

import android.net.Uri

data class GroupState(
    val groups: List<Group> = emptyList(),
    val name: String="",
    var ringtoneUri: Uri? = null,
    val contacts: List<Long> = emptyList(),
    val selectedGroup: Group? = null
    )
