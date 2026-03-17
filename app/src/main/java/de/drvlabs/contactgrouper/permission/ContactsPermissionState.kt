package de.drvlabs.contactgrouper.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class ContactsPermissionState(
    val hasPermission: Boolean,
    val permanentlyDenied: Boolean
)

object ContactsPermissionEvaluator {
    fun evaluate(
        activity: Activity,
        hasRequestedPermission: Boolean
    ): ContactsPermissionState {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        return evaluate(
            hasReadPermission = hasReadPermission,
            hasWritePermission = hasWritePermission,
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
        hasReadPermission: Boolean,
        hasWritePermission: Boolean,
        hasRequestedPermission: Boolean,
        shouldShowReadRationale: Boolean,
        shouldShowWriteRationale: Boolean
    ): ContactsPermissionState {
        val hasPermission = hasReadPermission && hasWritePermission
        if (hasPermission) {
            return ContactsPermissionState(
                hasPermission = true,
                permanentlyDenied = false
            )
        }

        val missingReadPermission = !hasReadPermission
        val missingWritePermission = !hasWritePermission
        val permanentlyDenied = hasRequestedPermission &&
            (
                (missingReadPermission && !shouldShowReadRationale) ||
                    (missingWritePermission && !shouldShowWriteRationale)
                )

        return ContactsPermissionState(
            hasPermission = false,
            permanentlyDenied = permanentlyDenied
        )
    }
}
