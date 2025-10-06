package de.drvlabs.contactgrouper.viewmodels

import android.content.Context
import androidx.room.*

@Database(entities = [ContactGroup::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class GroupDatabase : RoomDatabase() {

    abstract fun dao(): GroupDao

//    companion object {
//        @Volatile
//        private var INSTANCE: GroupDatabase? = null
//
//        fun getDatabase(context: Context): GroupDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    GroupDatabase::class.java,
//                    "contact_group_database" // Name der Datenbankdatei
//                ).build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
}