package de.drvlabs.contactgrouper.groups

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

fun GroupMutationResult.userMessage(): String? {
    return when (this) {
        GroupMutationResult.Success -> null
        GroupMutationResult.PermissionDenied -> {
            "Contacts permission is required to complete this action."
        }

        GroupMutationResult.Conflict -> {
            "The contacts database changed while this action was running. Please try again."
        }

        GroupMutationResult.InvalidRequest -> {
            "This action could not be completed."
        }

        is GroupMutationResult.ProviderWriteFailed -> when (action) {
            GroupMutationAction.CREATE_GROUP -> {
                "The group was created in the app, but syncing it to device contacts failed."
            }

            GroupMutationAction.ASSIGN_CONTACTS -> {
                "The contacts were updated in the app, but device contact syncing failed."
            }

            GroupMutationAction.SET_CONTACT_GROUPS -> {
                "The contact's groups were updated in the app, but device contact syncing failed."
            }

            GroupMutationAction.REMOVE_MEMBERSHIP -> {
                "The contact was updated in the app, but device contact syncing failed."
            }

            GroupMutationAction.CHANGE_RINGTONE -> {
                "The group ringtone was saved, but applying it to contacts failed."
            }

            GroupMutationAction.DELETE_GROUP -> {
                "Deleting the group from device contacts failed."
            }

            GroupMutationAction.SYNC_DEVICE_GROUPS -> {
                "Syncing device contact groups failed."
            }
        }
    }
}
