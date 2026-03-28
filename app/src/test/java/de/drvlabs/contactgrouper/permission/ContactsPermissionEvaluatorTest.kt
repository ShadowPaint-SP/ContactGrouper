package de.drvlabs.contactgrouper.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsPermissionEvaluatorTest {

    @Test
    fun `both permissions granted clears permanent denial`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasReadPermission = true,
            hasWritePermission = true,
            hasRequestedPermission = true,
            shouldShowReadRationale = false,
            shouldShowWriteRationale = false
        )

        assertTrue(state.hasPermission)
        assertFalse(state.permanentlyDenied)
    }

    @Test
    fun `before first request the app is not permanently denied`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasReadPermission = false,
            hasWritePermission = false,
            hasRequestedPermission = false,
            shouldShowReadRationale = false,
            shouldShowWriteRationale = false
        )

        assertFalse(state.hasPermission)
        assertFalse(state.permanentlyDenied)
    }

    @Test
    fun `partial first install permission state is not permanently denied`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasReadPermission = true,
            hasWritePermission = false,
            hasRequestedPermission = false,
            shouldShowReadRationale = false,
            shouldShowWriteRationale = false
        )

        assertFalse(state.hasPermission)
        assertFalse(state.permanentlyDenied)
    }

    @Test
    fun `missing permissions stay recoverable while rationale is shown`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasReadPermission = false,
            hasWritePermission = false,
            hasRequestedPermission = true,
            shouldShowReadRationale = true,
            shouldShowWriteRationale = true
        )

        assertFalse(state.hasPermission)
        assertFalse(state.permanentlyDenied)
    }

    @Test
    fun `read granted and write denied with rationale is not permanent`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasReadPermission = true,
            hasWritePermission = false,
            hasRequestedPermission = true,
            shouldShowReadRationale = false,
            shouldShowWriteRationale = true
        )

        assertFalse(state.hasPermission)
        assertFalse(state.permanentlyDenied)
    }

    @Test
    fun `write granted and read denied with rationale is not permanent`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasReadPermission = false,
            hasWritePermission = true,
            hasRequestedPermission = true,
            shouldShowReadRationale = true,
            shouldShowWriteRationale = false
        )

        assertFalse(state.hasPermission)
        assertFalse(state.permanentlyDenied)
    }

    @Test
    fun `missing permission without rationale is treated as permanent`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasReadPermission = true,
            hasWritePermission = false,
            hasRequestedPermission = true,
            shouldShowReadRationale = false,
            shouldShowWriteRationale = false
        )

        assertFalse(state.hasPermission)
        assertTrue(state.permanentlyDenied)
    }

    @Test
    fun `both missing permissions without rationale are treated as permanent`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasReadPermission = false,
            hasWritePermission = false,
            hasRequestedPermission = true,
            shouldShowReadRationale = false,
            shouldShowWriteRationale = false
        )

        assertFalse(state.hasPermission)
        assertTrue(state.permanentlyDenied)
    }
}
