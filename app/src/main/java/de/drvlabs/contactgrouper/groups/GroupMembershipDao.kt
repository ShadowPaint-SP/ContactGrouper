package de.drvlabs.contactgrouper.groups

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMembershipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembership(membership: GroupMembership)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemberships(memberships: List<GroupMembership>)

    @Query("DELETE FROM GroupMembership WHERE groupId = :groupId AND contactId = :contactId")
    suspend fun deleteMembership(groupId: Int, contactId: Long)

    @Query("DELETE FROM GroupMembership WHERE groupId = :groupId")
    suspend fun deleteMembershipsForGroup(groupId: Int)

    @Query("SELECT * FROM GroupMembership ORDER BY assignedAt DESC")
    fun observeAllMemberships(): Flow<List<GroupMembership>>

    @Query("SELECT * FROM GroupMembership ORDER BY assignedAt DESC")
    suspend fun getAllMemberships(): List<GroupMembership>

    @Query("SELECT * FROM GroupMembership WHERE source = :source ORDER BY assignedAt DESC")
    suspend fun getMembershipsBySource(source: GroupSyncSource): List<GroupMembership>

    @Query("SELECT * FROM GroupMembership WHERE groupId = :groupId ORDER BY assignedAt DESC")
    suspend fun getMembershipsForGroup(groupId: Int): List<GroupMembership>

    @Query("SELECT * FROM GroupMembership WHERE contactId = :contactId ORDER BY assignedAt DESC")
    suspend fun getMembershipsForContact(contactId: Long): List<GroupMembership>
}
