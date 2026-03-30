package de.drvlabs.contactgrouper.groups

import de.drvlabs.contactgrouper.AppErrorDetailsFormatter

internal object GroupSyncDiagnostics {

    fun buildTechnicalDetails(
        operation: String,
        throwable: Throwable,
        context: Map<String, Any?> = emptyMap()
    ): String {
        return AppErrorDetailsFormatter.format(
            throwable = throwable,
            heading = "Operation: $operation",
            context = context
        )
    }

    fun reportFailure(
        operation: String,
        throwable: Throwable,
        context: Map<String, Any?> = emptyMap()
    ) {
        val contextSuffix = if (context.isEmpty()) {
            ""
        } else {
            context.entries.joinToString(prefix = " [", postfix = "]") { (key, value) ->
                "$key=$value"
            }
        }

        System.err.println(
            "Group sync failure during $operation$contextSuffix: " +
                "${throwable::class.java.simpleName}: ${throwable.message.orEmpty()}"
        )
        throwable.printStackTrace()
    }
}
