package de.drvlabs.contactgrouper.groups

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Insert
    suspend fun insertGroup(group: Group): Long

    @Update
    suspend fun updateGroup(group: Group)

    @Delete
    suspend fun deleteGroup(group: Group)

    @Query("SELECT * FROM `Group` ORDER BY name ASC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM `Group` WHERE id = :groupId")
    fun observeGroupById(groupId: Int): Flow<Group?>

    @Query("SELECT * FROM `Group` WHERE id = :groupId")
    suspend fun getGroupById(groupId: Int): Group?

    @Query("SELECT * FROM `Group` WHERE deviceGroupId = :deviceGroupId")
    suspend fun getGroupByDeviceGroupId(deviceGroupId: Long): Group?

    @Query("SELECT * FROM `Group` WHERE id IN (:groupIds)")
    suspend fun getGroupsByIds(groupIds: List<Int>): List<Group>

    @Query("SELECT * FROM `Group` WHERE syncSource = :source ORDER BY name ASC")
    suspend fun getGroupsBySource(source: GroupSyncSource): List<Group>
}
