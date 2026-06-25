package de.drvlabs.contactgrouper.groups

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import de.drvlabs.contactgrouper.R

sealed interface GroupDeletionConfirmation {
    @get:StringRes
    val titleResId: Int

    @get:StringRes
    val confirmLabelResId: Int
}

data class SingleGroupDeletionConfirmation(
    @StringRes override val titleResId: Int,
    @StringRes val messageResId: Int,
    @StringRes override val confirmLabelResId: Int
) : GroupDeletionConfirmation

data class MultipleGroupDeletionConfirmation(
    @StringRes override val titleResId: Int,
    @PluralsRes val messagePluralResId: Int,
    @StringRes override val confirmLabelResId: Int,
    val deletableCount: Int,
    val blockedCount: Int
) : GroupDeletionConfirmation

fun buildGroupDeletionConfirmation(group: Group): SingleGroupDeletionConfirmation {
    return if (group.deletesFromDevice) {
        SingleGroupDeletionConfirmation(
            titleResId = R.string.delete_group_from_device_title,
            messageResId = R.string.delete_group_from_device_message,
            confirmLabelResId = R.string.delete_group_everywhere
        )
    } else {
        SingleGroupDeletionConfirmation(
            titleResId = R.string.delete_group_title,
            messageResId = R.string.delete_group_message,
            confirmLabelResId = R.string.groups_delete
        )
    }
}

fun buildGroupDeletionConfirmation(groups: List<Group>): MultipleGroupDeletionConfirmation {
    val deletableGroups = groups.filter { it.canDelete }
    val blockedCount = groups.size - deletableGroups.size
    val deletesFromDevice = deletableGroups.any { it.deletesFromDevice }

    return if (deletesFromDevice) {
        MultipleGroupDeletionConfirmation(
            titleResId = R.string.delete_selected_groups_from_device_title,
            messagePluralResId = R.plurals.delete_selected_groups_from_device_message,
            confirmLabelResId = R.string.delete_eligible,
            deletableCount = deletableGroups.size,
            blockedCount = blockedCount
        )
    } else {
        MultipleGroupDeletionConfirmation(
            titleResId = R.string.delete_selected_groups_title,
            messagePluralResId = R.plurals.delete_selected_groups_message,
            confirmLabelResId = R.string.delete_eligible,
            deletableCount = deletableGroups.size,
            blockedCount = blockedCount
        )
    }
}
