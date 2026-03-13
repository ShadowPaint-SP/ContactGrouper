package de.drvlabs.contactgrouper.groups

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface GroupsRepository {
    fun observeGroups(): Flow<List<Group>>
    fun observeMemberships(): Flow<List<GroupMembership>>
    suspend fun getGroup(groupId: Int): Group?
    suspend fun createLocalGroup(name: String, ringtoneUri: Uri?): Int
    suspend fun assignContactsToGroups(groupIds: List<Int>, contactIds: List<Long>)
    suspend fun removeContactFromGroup(groupId: Int, contactId: Long)
    suspend fun changeGroupRingtone(groupId: Int, ringtoneUri: Uri?)
    suspend fun deleteGroup(groupId: Int)
    suspend fun syncDeviceGroups(snapshot: DeviceGroupSnapshot)
}
