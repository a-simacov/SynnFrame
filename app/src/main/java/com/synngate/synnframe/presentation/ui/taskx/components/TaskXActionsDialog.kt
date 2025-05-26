package com.synngate.synnframe.presentation.ui.taskx.components

//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.Pause
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import com.synngate.synnframe.presentation.ui.taskx.model.StatusActionData
//
//@Composable
//fun TaskXActionsDialog(
//    onDismiss: () -> Unit,
//    onNavigateBack: () -> Unit,
//    statusActions: List<StatusActionData>,
//    isProcessing: Boolean = false,
//    modifier: Modifier = Modifier
//) {
//    Dialog(
//        onDismissRequest = {
//            if (!isProcessing) onDismiss()
//        }
//    ) {
//        Card(
//            modifier = modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.surface
//            ),
//            elevation = CardDefaults.cardElevation(
//                defaultElevation = 6.dp
//            )
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(8.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = "Действия",
//                    style = MaterialTheme.typography.titleLarge,
//                    textAlign = TextAlign.Center,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Text(
//                    text = "Выберите действие для задания или вернитесь к задаче",
//                    style = MaterialTheme.typography.bodyMedium,
//                    textAlign = TextAlign.Center,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                if (isProcessing) {
//                    CircularProgressIndicator(
//                        modifier = Modifier
//                            .size(48.dp)
//                            .padding(bottom = 16.dp)
//                    )
//                    Text(
//                        text = "Обработка запроса...",
//                        style = MaterialTheme.typography.bodyMedium,
//                        textAlign = TextAlign.Center
//                    )
//                    Spacer(modifier = Modifier.height(16.dp))
//                } else {
//                    if (statusActions.isNotEmpty()) {
//                        statusActions.forEach { actionData ->
//                            Button(
//                                onClick = {
//                                    actionData.onClick()
//                                },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(vertical = 4.dp),
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = when (actionData.id) {
//                                        "start" -> MaterialTheme.colorScheme.primary
//                                        "finish" -> MaterialTheme.colorScheme.secondary
//                                        "pause" -> MaterialTheme.colorScheme.tertiary
//                                        "resume" -> MaterialTheme.colorScheme.primary
//                                        else -> MaterialTheme.colorScheme.primary
//                                    }
//                                ),
//                                enabled = !isProcessing
//                            ) {
//                                Icon(
//                                    imageVector = getIconByName(actionData.iconName),
//                                    contentDescription = actionData.description,
//                                    modifier = Modifier.padding(end = 8.dp)
//                                )
//                                Text(actionData.text)
//                            }
//                        }
//                    } else {
//                        Text(
//                            text = "Нет доступных действий",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            textAlign = TextAlign.Center,
//                            modifier = Modifier.fillMaxWidth()
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    OutlinedButton(
//                        onClick = onDismiss,
//                        modifier = Modifier.weight(1f),
//                        enabled = !isProcessing
//                    ) {
//                        Text("Остаться")
//                    }
//
//                    Spacer(modifier = Modifier.width(4.dp))
//
//                    Button(
//                        onClick = onNavigateBack,
//                        modifier = Modifier.weight(1f),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.errorContainer,
//                            contentColor = MaterialTheme.colorScheme.onErrorContainer
//                        ),
//                        enabled = !isProcessing
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Close,
//                            contentDescription = "Выйти",
//                            modifier = Modifier.padding(end = 4.dp)
//                        )
//                        Text("Выйти")
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun getIconByName(name: String): ImageVector {
//    return when (name) {
//        "play_arrow" -> Icons.Default.PlayArrow
//        "pause" -> Icons.Default.Pause
//        "check_circle" -> Icons.Default.CheckCircle
//        // другие иконки...
//        else -> Icons.Default.Info
//    }
//}