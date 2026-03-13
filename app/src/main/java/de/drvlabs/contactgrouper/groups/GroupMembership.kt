package de.drvlabs.contactgrouper.groups

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["groupId", "contactId"],
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("groupId"),
        Index("contactId")
    ]
)
data class GroupMembership(
    val groupId: Int,
    val contactId: Long,
    val assignedAt: Long,
    val source: GroupSyncSource = GroupSyncSource.LOCAL
)
