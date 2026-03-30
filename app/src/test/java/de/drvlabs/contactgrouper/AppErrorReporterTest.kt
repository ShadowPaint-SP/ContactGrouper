package de.drvlabs.contactgrouper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorReporterTest {

    @Test
    fun `startup fatal error includes classification and cause details`() {
        val error = AppError.startupFatal(
            origin = AppErrorOrigin.Bootstrap,
            userMessage = "The app hit an unexpected error during startup.",
            throwable = IllegalStateException(
                "boom",
                IllegalArgumentException("bad input")
            ),
            heading = "Bootstrapping the app failed."
        )

        assertEquals(AppErrorKind.StartupFatal, error.kind)
        assertEquals("App Failed to Start", error.title)
        assertTrue(error.technicalDetails.contains("Bootstrapping the app failed."))
        assertTrue(error.technicalDetails.contains("java.lang.IllegalStateException: boom"))
        assertTrue(error.technicalDetails.contains("java.lang.IllegalArgumentException: bad input"))
    }

    @Test
    fun `runtime unexpected error is dismissible through reporter clear`() {
        val reporter = AppErrorReporter()

        reporter.report(
            AppError.runtimeUnexpected(
                origin = AppErrorOrigin.GroupMutation,
                userMessage = "Updating contact groups failed unexpectedly.",
                throwable = IllegalArgumentException("broken mutation")
            )
        )

        assertEquals(AppErrorKind.RuntimeUnexpected, reporter.currentError.value?.kind)

        reporter.clearCurrent()

        assertEquals(null, reporter.currentError.value)
    }

    @Test
    fun `startup fatal error is not cleared by runtime dismiss`() {
        val reporter = AppErrorReporter()

        reporter.report(
            AppError.startupFatal(
                origin = AppErrorOrigin.ContactsImport,
                userMessage = "Loading contacts failed during startup.",
                throwable = IllegalStateException("boom")
            )
        )

        reporter.clearCurrent()

        assertEquals(AppErrorKind.StartupFatal, reporter.currentError.value?.kind)
    }
}
