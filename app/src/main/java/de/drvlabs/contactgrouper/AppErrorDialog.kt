package de.drvlabs.contactgrouper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppErrorDialog(
    error: AppError,
    onDismiss: () -> Unit,
    onCloseApp: () -> Unit
) {
    val dismissible = error.kind == AppErrorKind.RuntimeUnexpected

    AlertDialog(
        onDismissRequest = {
            if (dismissible) {
                onDismiss()
            }
        },
        title = { Text(error.title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(error.userMessage)
                Text(
                    text = "Technical details",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = error.technicalDetails,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (dismissible) {
                        onDismiss()
                    } else {
                        onCloseApp()
                    }
                }
            ) {
                Text(if (dismissible) "OK" else "Close app")
            }
        }
    )
}
