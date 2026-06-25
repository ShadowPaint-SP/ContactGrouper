package de.drvlabs.contactgrouper.groups

data class GroupDeletionConfirmation(
    val title: String,
    val message: String,
    val confirmLabel: String
)

fun buildGroupDeletionConfirmation(group: Group): GroupDeletionConfirmation {
    return if (group.deletesFromDevice) {
        GroupDeletionConfirmation(
            title = "Delete Group From Device?",
            message = "Deleting this group will remove it from this app and from the device's contact groups.",
            confirmLabel = "Delete Everywhere"
        )
    } else {
        GroupDeletionConfirmation(
            title = "Delete Group?",
            message = "Deleting this group will remove it from this app. Contacts will stay on your device.",
            confirmLabel = "Delete Group"
        )
    }
}

fun buildGroupDeletionConfirmation(groups: List<Group>): GroupDeletionConfirmation {
    val deletableGroups = groups.filter { it.canDelete }
    val blockedCount = groups.size - deletableGroups.size
    val deletesFromDevice = deletableGroups.any { it.deletesFromDevice }
    val groupLabel = if (deletableGroups.size == 1) "group" else "groups"
    val pronoun = if (deletableGroups.size == 1) "it" else "them"
    val blockedMessage = if (blockedCount > 0) {
        "\n\n$blockedCount selected ${if (blockedCount == 1) "group is" else "groups are"} read-only and will not be deleted."
    } else {
        ""
    }

    return if (deletesFromDevice) {
        GroupDeletionConfirmation(
            title = "Delete Selected Groups From Device?",
            message = "Deleting ${deletableGroups.size} $groupLabel will remove $pronoun from this app and from the device's contact groups.$blockedMessage",
            confirmLabel = "Delete Eligible"
        )
    } else {
        GroupDeletionConfirmation(
            title = "Delete Selected Groups?",
            message = "Deleting ${deletableGroups.size} $groupLabel will remove $pronoun from this app. Contacts will stay on your device.$blockedMessage",
            confirmLabel = "Delete Eligible"
        )
    }
}
