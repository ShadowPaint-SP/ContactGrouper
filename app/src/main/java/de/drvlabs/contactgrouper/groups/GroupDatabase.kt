package de.drvlabs.contactgrouper.groups

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Group::class, GroupMembership::class, ContactRingtoneState::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GroupDatabase : RoomDatabase() {

    abstract val groupDao: GroupDao
    abstract val membershipDao: GroupMembershipDao
    abstract val contactRingtoneStateDao: ContactRingtoneStateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Group` RENAME TO `GroupLegacy`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `Group` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `ringtoneUri` TEXT,
                        `syncSource` TEXT NOT NULL,
                        `deviceGroupId` INTEGER,
                        `accountName` TEXT,
                        `accountType` TEXT,
                        `dataSet` TEXT,
                        `isReadOnly` INTEGER NOT NULL,
                        `isVisible` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `Group` (
                        `id`, `name`, `color`, `ringtoneUri`, `syncSource`,
                        `deviceGroupId`, `accountName`, `accountType`, `dataSet`,
                        `isReadOnly`, `isVisible`
                    )
                    SELECT
                        `id`, `name`, `color`, `ringtoneUri`, 'LOCAL',
                        NULL, NULL, NULL, NULL,
                        0, 1
                    FROM `GroupLegacy`
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_Group_deviceGroupId`
                    ON `Group` (`deviceGroupId`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `GroupMembership` (
                        `groupId` INTEGER NOT NULL,
                        `contactId` INTEGER NOT NULL,
                        `assignedAt` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        PRIMARY KEY(`groupId`, `contactId`),
                        FOREIGN KEY(`groupId`) REFERENCES `Group`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_GroupMembership_groupId` ON `GroupMembership` (`groupId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_GroupMembership_contactId` ON `GroupMembership` (`contactId`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ContactRingtoneState` (
                        `contactId` INTEGER NOT NULL,
                        `baselineRingtoneUri` TEXT,
                        `lastAppliedGroupId` INTEGER,
                        `lastAppliedRingtoneUri` TEXT,
                        PRIMARY KEY(`contactId`)
                    )
                    """.trimIndent()
                )

                val counter = longArrayOf(0L)
                db.query(
                    "SELECT id, contactIds FROM `GroupLegacy` ORDER BY name DESC, id DESC"
                ).use { cursor ->
                    val idIndex = cursor.getColumnIndex("id")
                    val contactIdsIndex = cursor.getColumnIndex("contactIds")

                    while (cursor.moveToNext()) {
                        val groupId = cursor.getInt(idIndex)
                        val serializedContactIds = cursor.getString(contactIdsIndex).orEmpty()
                        if (serializedContactIds.isBlank()) {
                            continue
                        }

                        serializedContactIds
                            .split(",")
                            .mapNotNull { it.trim().toLongOrNull() }
                            .forEach { contactId ->
                                counter[0] += 1L
                                db.execSQL(
                                    """
                                    INSERT OR REPLACE INTO `GroupMembership` (`groupId`, `contactId`, `assignedAt`, `source`)
                                    VALUES (?, ?, ?, ?)
                                    """.trimIndent(),
                                    arrayOf(groupId, contactId, counter[0], GroupSyncSource.LOCAL.name)
                                )
                            }
                    }
                }

                db.execSQL("DROP TABLE `GroupLegacy`")
            }
        }
    }
}
