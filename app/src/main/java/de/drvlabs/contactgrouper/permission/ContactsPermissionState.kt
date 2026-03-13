package de.drvlabs.contactgrouper.permission

import android.Manifest
import android.app.Activity

data class ContactsPermissionState(
    val hasPermission: Boolean,
    val permanentlyDenied: Boolean
)

object ContactsPermissionEvaluator {
    fun evaluate(
        activity: Activity,
        hasPermission: Boolean,
        hasRequestedPermission: Boolean
    ): ContactsPermissionState {
        return evaluate(
            hasPermission = hasPermission,
            hasRequestedPermission = hasRequestedPermission,
            shouldShowReadRationale = activity.shouldShowRequestPermissionRationale(
                Manifest.permission.READ_CONTACTS
            ),
            shouldShowWriteRationale = activity.shouldShowRequestPermissionRationale(
                Manifest.permission.WRITE_CONTACTS
            )
        )
    }

    fun evaluate(
        hasPermission: Boolean,
        hasRequestedPermission: Boolean,
        shouldShowReadRationale: Boolean,
        shouldShowWriteRationale: Boolean
    ): ContactsPermissionState {
        if (hasPermission) {
            return ContactsPermissionState(
                hasPermission = true,
                permanentlyDenied = false
            )
        }

        val permanentlyDenied = hasRequestedPermission &&
            (!shouldShowReadRationale || !shouldShowWriteRationale)

        return ContactsPermissionState(
            hasPermission = false,
            permanentlyDenied = permanentlyDenied
        )
    }
}
