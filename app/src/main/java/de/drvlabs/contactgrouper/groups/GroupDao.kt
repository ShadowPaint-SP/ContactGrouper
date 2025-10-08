package de.drvlabs.contactgrouper.groups

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    // Insert und Update
    @Upsert
    suspend fun upsertGroup(group: Group)

    @Delete
    suspend fun deleteGroup(group: Group)

    // Flow sorgt dafür, dass deine UI automatisch aktualisiert wird, wenn sich Daten ändern.
    @Query("SELECT * FROM `Group` ORDER BY name ASC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM `Group` WHERE id = :groupId")
    fun getGroupById(groupId: Int): Flow<Group>

    @Query("SELECT * FROM `Group` WHERE contactIds LIKE '%' || :contactId || '%' ORDER BY name ASC")
    fun getAllGroupsByContact(contactId: String): Flow<List<Group>>
}