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
    suspend fun upsertGroup(group: ContactGroup)

    @Delete
    suspend fun deleteGroup(group: ContactGroup)

    // Flow sorgt dafür, dass deine UI automatisch aktualisiert wird, wenn sich Daten ändern.
    @Query("SELECT * FROM ContactGroup ORDER BY name ASC")
    fun getAllGroups(): Flow<List<ContactGroup>>

    @Query("SELECT * FROM ContactGroup WHERE id = :groupId ORDER BY name ASC")
    fun getGroupById(groupId: Int): Flow<ContactGroup>

    @Query("SELECT * FROM ContactGroup WHERE contactIds LIKE '%' || :contactId || '%' ORDER BY name ASC")
    fun getAllGroupsByContact(contactId: String): Flow<List<ContactGroup>>
}