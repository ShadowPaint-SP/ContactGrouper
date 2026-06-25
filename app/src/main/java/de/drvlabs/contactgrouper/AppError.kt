package de.drvlabs.contactgrouper

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
            title: String,
            userMessage: String,
            throwable: Throwable,
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
            title: String,
            userMessage: String,
            throwable: Throwable,
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

fun interface AppErrorLogSink {
    fun persist(error: AppError)
}

class AppErrorFileLogStore(
    context: Context,
    private val maxEntries: Int = 10,
    private val fileName: String = "app-error-reports.txt"
) : AppErrorLogSink {
    private val logFile = File(context.filesDir, fileName)

    @Synchronized
    override fun persist(error: AppError) {
        val existingEntries = if (logFile.exists()) {
            logFile.readText()
                .split("\n\n====\n\n")
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        val updatedEntries = (existingEntries + formatEntry(error)).takeLast(maxEntries)

        logFile.parentFile?.mkdirs()
        logFile.writeText(updatedEntries.joinToString(separator = "\n\n====\n\n"))
    }

    private fun formatEntry(error: AppError): String {
        val timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .format(
                Instant.now().atZone(ZoneId.systemDefault())
            )

        return buildString {
            appendLine("timestamp: $timestamp")
            appendLine("kind: ${error.kind}")
            appendLine("origin: ${error.origin}")
            appendLine("title: ${error.title}")
            appendLine("userMessage: ${error.userMessage}")
            appendLine("technicalDetails:")
            append(error.technicalDetails)
        }
    }
}

class AppErrorReporter(
    private val logSink: AppErrorLogSink? = null
) {
    private val mutableCurrentError = MutableStateFlow<AppError?>(null)

    val currentError: StateFlow<AppError?> = mutableCurrentError.asStateFlow()

    fun report(error: AppError) {
        val current = mutableCurrentError.value
        if (current?.kind == AppErrorKind.StartupFatal) {
            return
        }
        logSink?.persist(error)
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
