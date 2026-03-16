package de.drvlabs.contactgrouper

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import de.drvlabs.contactgrouper.contacts.ContactsDataSource
import de.drvlabs.contactgrouper.groups.AndroidContactRingtoneGateway
import de.drvlabs.contactgrouper.groups.ContactsContractDeviceGroupSource
import de.drvlabs.contactgrouper.groups.ContactsContractDeviceGroupWriteGateway
import de.drvlabs.contactgrouper.groups.DeviceGroupSyncManager
import de.drvlabs.contactgrouper.groups.GroupDatabase
import de.drvlabs.contactgrouper.groups.RoomGroupsRepository

class AppContainer(
    context: Context
) {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    val database: GroupDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            GroupDatabase::class.java,
            "groups.db"
        ).addMigrations(GroupDatabase.MIGRATION_1_2).build()
    }

    val groupsRepository: RoomGroupsRepository by lazy {
        RoomGroupsRepository(
            database = database,
            ringtoneGateway = AndroidContactRingtoneGateway(contentResolver),
            deviceGroupWriteGateway = ContactsContractDeviceGroupWriteGateway(contentResolver)
        )
    }

    val contactsDataSource: ContactsDataSource by lazy {
        ContactsDataSource(contentResolver)
    }

    val deviceGroupSyncManager: DeviceGroupSyncManager by lazy {
        DeviceGroupSyncManager(
            contentResolver = contentResolver,
            source = ContactsContractDeviceGroupSource(contentResolver),
            repository = groupsRepository
        )
    }
}
