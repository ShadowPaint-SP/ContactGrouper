package de.drvlabs.contactgrouper.groups

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupViewModelDeleteGroupsTest {

    @Test
    fun `delete groups deletes each group in order`() = runBlocking {
        val deletedGroupIds = mutableListOf<Int>()

        val result = deleteGroupsSequentially(listOf(3, 1, 2)) { groupId ->
            deletedGroupIds.add(groupId)
            GroupMutationResult.Success
        }

        assertEquals(GroupMutationResult.Success, result)
        assertEquals(listOf(3, 1, 2), deletedGroupIds)
    }

    @Test
    fun `delete groups stops at first failure`() = runBlocking {
        val deletedGroupIds = mutableListOf<Int>()

        val result = deleteGroupsSequentially(listOf(3, 1, 2)) { groupId ->
            deletedGroupIds.add(groupId)
            if (groupId == 1) {
                GroupMutationResult.Conflict
            } else {
                GroupMutationResult.Success
            }
        }

        assertEquals(GroupMutationResult.Conflict, result)
        assertEquals(listOf(3, 1), deletedGroupIds)
    }
}
