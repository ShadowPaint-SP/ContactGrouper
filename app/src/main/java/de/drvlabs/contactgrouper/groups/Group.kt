package de.drvlabs.contactgrouper.groups

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Group(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val ringtoneUri: Uri? = null,
    val contactIds: List<Long>
)
