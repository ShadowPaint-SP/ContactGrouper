package de.drvlabs.contactgrouper.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsPermissionEvaluatorTest {

    @Test
    fun `permission granted clears permanent denial`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasPermission = true,
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
            hasPermission = false,
            hasRequestedPermission = false,
            shouldShowReadRationale = false,
            shouldShowWriteRationale = false
        )

        assertFalse(state.hasPermission)
        assertFalse(state.permanentlyDenied)
    }

    @Test
    fun `after request a denied permission without rationale is treated as permanent`() {
        val state = ContactsPermissionEvaluator.evaluate(
            hasPermission = false,
            hasRequestedPermission = true,
            shouldShowReadRationale = false,
            shouldShowWriteRationale = true
        )

        assertFalse(state.hasPermission)
        assertTrue(state.permanentlyDenied)
    }
}
