package de.drvlabs.contactgrouper.groups

import android.net.Uri

data class AddGroupState(
    val name: String = "",
    val ringtoneUri: Uri? = null
)
