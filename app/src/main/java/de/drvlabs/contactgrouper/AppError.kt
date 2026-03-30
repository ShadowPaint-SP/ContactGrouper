package de.drvlabs.contactgrouper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppErrorKind {
    StartupFatal,
    RuntimeUnexpected
}

enum class AppErrorOrigin {
    Bootstrap,
    ContactsImport,
    DeviceGroupSync,
    GroupMutation
}

data class AppError(
    val title: String,
    val userMessage: String,
    val technicalDetails: String,
    val kind: AppErrorKind,
    val origin: AppErrorOrigin
) {
    companion object {
        fun startupFatal(
            origin: AppErrorOrigin,
            userMessage: String,
            throwable: Throwable,
            title: String = "App Failed to Start",
            heading: String? = null,
            context: Map<String, Any?> = emptyMap()
        ): AppError {
            return AppError(
                title = title,
                userMessage = userMessage,
                technicalDetails = AppErrorDetailsFormatter.format(
                    throwable = throwable,
                    heading = heading,
                    context = context
                ),
                kind = AppErrorKind.StartupFatal,
                origin = origin
            )
        }

        fun runtimeUnexpected(
            origin: AppErrorOrigin,
            userMessage: String,
            throwable: Throwable,
            title: String = "Unexpected Error",
            heading: String? = null,
            context: Map<String, Any?> = emptyMap()
        ): AppError {
            return AppError(
                title = title,
                userMessage = userMessage,
                technicalDetails = AppErrorDetailsFormatter.format(
                    throwable = throwable,
                    heading = heading,
                    context = context
                ),
                kind = AppErrorKind.RuntimeUnexpected,
                origin = origin
            )
        }
    }
}

class AppErrorReporter {
    private val mutableCurrentError = MutableStateFlow<AppError?>(null)

    val currentError: StateFlow<AppError?> = mutableCurrentError.asStateFlow()

    fun report(error: AppError) {
        val current = mutableCurrentError.value
        if (current?.kind == AppErrorKind.StartupFatal) {
            return
        }
        mutableCurrentError.value = error
    }

    fun clearCurrent() {
        if (mutableCurrentError.value?.kind == AppErrorKind.RuntimeUnexpected) {
            mutableCurrentError.value = null
        }
    }
}

internal object AppErrorDetailsFormatter {
    fun format(
        throwable: Throwable,
        heading: String? = null,
        context: Map<String, Any?> = emptyMap()
    ): String {
        val lines = mutableListOf<String>()

        heading?.takeUnless { it.isBlank() }?.let(lines::add)

        if (context.isNotEmpty()) {
            lines += "Context:"
            context.entries
                .sortedBy { it.key }
                .forEach { (key, value) ->
                    lines += "$key: ${value ?: "null"}"
                }
        }

        generateSequence(throwable) { it.cause }
            .take(5)
            .forEachIndexed { index, cause ->
                val prefix = if (index == 0) "Error" else "Caused by"
                val message = cause.message?.takeUnless { it.isBlank() } ?: "No message"
                lines += "$prefix: ${cause::class.java.name}: $message"
            }

        return lines.joinToString("\n")
    }
}
