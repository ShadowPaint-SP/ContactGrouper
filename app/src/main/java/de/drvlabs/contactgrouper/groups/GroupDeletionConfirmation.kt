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
