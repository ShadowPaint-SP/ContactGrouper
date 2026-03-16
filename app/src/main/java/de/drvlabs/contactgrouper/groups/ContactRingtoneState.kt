package de.drvlabs.contactgrouper.groups

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ContactRingtoneState(
    @PrimaryKey
    val contactId: Long,
    val baselineRingtoneUri: String?,
    val lastAppliedGroupId: Int? = null,
    val lastAppliedRingtoneUri: String? = null
)
