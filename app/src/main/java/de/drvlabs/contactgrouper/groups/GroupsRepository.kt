package de.drvlabs.contactgrouper.groups

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface GroupsRepository {
    fun observeGroups(): Flow<List<Group>>
    fun observeMemberships(): Flow<List<GroupMembership>>
    suspend fun getGroup(groupId: Int): Group?
    suspend fun createLocalGroup(name: String, ringtoneUri: Uri?): GroupMutationResult
    suspend fun assignContactsToGroups(groupIds: List<Int>, contactIds: List<Long>): GroupMutationResult
    suspend fun removeContactFromGroup(groupId: Int, contactId: Long): GroupMutationResult
    suspend fun changeGroupRingtone(groupId: Int, ringtoneUri: Uri?): GroupMutationResult
    suspend fun deleteGroup(groupId: Int): GroupMutationResult
    suspend fun syncDeviceGroups(snapshot: DeviceGroupSnapshot): GroupMutationResult
}
