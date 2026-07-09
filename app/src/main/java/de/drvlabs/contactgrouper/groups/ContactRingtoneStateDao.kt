package de.drvlabs.contactgrouper.groups

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactRingtoneStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ContactRingtoneState)

    @Query("SELECT * FROM ContactRingtoneState WHERE contactId = :contactId")
    suspend fun getByContactId(contactId: Long): ContactRingtoneState?

    @Query("SELECT * FROM ContactRingtoneState WHERE contactId IN (:contactIds)")
    suspend fun getByContactIds(contactIds: List<Long>): List<ContactRingtoneState>

    @Query("SELECT * FROM ContactRingtoneState WHERE lastAppliedGroupId IS NOT NULL")
    suspend fun getAppliedStates(): List<ContactRingtoneState>
}
