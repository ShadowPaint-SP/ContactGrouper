package de.drvlabs.contactgrouper.groups

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ContactGroup::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class GroupDatabase : RoomDatabase() {

    abstract val dao: GroupDao
}