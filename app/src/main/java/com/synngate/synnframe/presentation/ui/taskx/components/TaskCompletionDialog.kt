import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.ui.taskx.model.TaskCompletionResult

@Composable
fun TaskCompletionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOkClick: () -> Unit,
    isProcessing: Boolean = false,
    completionResult: TaskCompletionResult? = null
) {
    val showResult = completionResult != null
    
    AlertDialog(
        onDismissRequest = { 
            if (!isProcessing && !showResult) onDismiss() 
        },
        title = { 
            Text(
                text = when {
                    showResult && completionResult?.isSuccess == true -> "Success"
                    showResult && completionResult?.isSuccess == false -> "Error"
                    else -> "Task Completion"
                },
                color = when {
                    showResult && completionResult?.isSuccess == true -> Color.Green
                    showResult && completionResult?.isSuccess == false -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        },
        text = {
            Text(
                text = when {
                    showResult -> completionResult?.message ?: ""
                    else -> "All actions are completed. Do you want to complete the task?"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            if (showResult) {
                Button(onClick = onOkClick) {
                    Text("OK")
                }
            } else {
                Button(
                    onClick = onConfirm,
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Completing...")
                        }
                    } else {
                        Text("Complete Task")
                    }
                }
            }
        },
        dismissButton = {
            if (!showResult) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}