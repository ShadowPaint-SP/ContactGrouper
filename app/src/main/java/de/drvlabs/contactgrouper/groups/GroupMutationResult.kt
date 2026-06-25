package de.drvlabs.contactgrouper.groups

import androidx.annotation.StringRes
import de.drvlabs.contactgrouper.R

enum class GroupMutationAction {
    CREATE_GROUP,
    ASSIGN_CONTACTS,
    SET_CONTACT_GROUPS,
    REMOVE_MEMBERSHIP,
    CHANGE_RINGTONE,
    DELETE_GROUP,
    SYNC_DEVICE_GROUPS
}

sealed interface GroupMutationResult {
    data object Success : GroupMutationResult
    data class ProviderWriteFailed(
        val action: GroupMutationAction
    ) : GroupMutationResult
    data object PermissionDenied : GroupMutationResult
    data object Conflict : GroupMutationResult
    data object InvalidRequest : GroupMutationResult
}

val GroupMutationResult.isSuccess: Boolean
    get() = this is GroupMutationResult.Success

@StringRes
fun GroupMutationResult.userMessageResId(): Int? {
    return when (this) {
        GroupMutationResult.Success -> null
        GroupMutationResult.PermissionDenied -> R.string.mutation_permission_denied

        GroupMutationResult.Conflict -> R.string.mutation_conflict

        GroupMutationResult.InvalidRequest -> R.string.mutation_invalid_request

        is GroupMutationResult.ProviderWriteFailed -> when (action) {
            GroupMutationAction.CREATE_GROUP -> R.string.mutation_create_group_provider_failed

            GroupMutationAction.ASSIGN_CONTACTS -> R.string.mutation_assign_contacts_provider_failed

            GroupMutationAction.SET_CONTACT_GROUPS -> R.string.mutation_set_contact_groups_provider_failed

            GroupMutationAction.REMOVE_MEMBERSHIP -> R.string.mutation_remove_membership_provider_failed

            GroupMutationAction.CHANGE_RINGTONE -> R.string.mutation_change_ringtone_provider_failed

            GroupMutationAction.DELETE_GROUP -> R.string.mutation_delete_group_provider_failed

            GroupMutationAction.SYNC_DEVICE_GROUPS -> R.string.mutation_sync_device_groups_provider_failed
        }
    }
}
