package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.base.StepViewState
import com.synngate.synnframe.presentation.ui.wizard.action.pallet.PalletSelectionViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun <T: Any> WizardStepContainer(
    state: StepViewState<T>,
    step: ActionStep,
    action: PlannedAction,
    viewModel: BaseStepViewModel<T>,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onCancel: () -> Unit = {},
    forwardEnabled: Boolean = state.data != null,
    isProcessingGlobal: Boolean = false,
    isFirstStep: Boolean = false,
    forwardText: String = "",
    backText: String = "",
    content: @Composable () -> Unit
) {
    var showLoading by remember { mutableStateOf(false) }
    var previousLoadingState by remember { mutableStateOf(false) }

    val isReallyLoading = state.isLoading || isProcessingGlobal

    LaunchedEffect(isReallyLoading) {
        if (isReallyLoading && !previousLoadingState) {
            delay(300)
            showLoading = isReallyLoading
        } else if (!isReallyLoading && previousLoadingState) {
            showLoading = false
        }
        previousLoadingState = isReallyLoading
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            WizardStepHeader(step)

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                state.error?.let {
                    WizardStateMessage(
                        message = it,
                        type = StateType.ERROR,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (showLoading) {
                    WizardLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        content()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isFirstStep) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        enabled = !isReallyLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(backText)
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        // Добавлена специальная обработка для разных типов ViewModel
                        // В зависимости от типа ViewModel вызываем соответствующий метод
                        when (viewModel) {
                            // Для PalletSelectionViewModel вызываем специальный метод
                            is PalletSelectionViewModel -> {
                                viewModel.manuallyCompleteStep()
                            }
                            // Для остальных типов используем общий подход
                            else -> {
                                // Если есть данные, пытаемся вызвать validateAndCompleteIfValid
                                if (state.data != null) {
                                    // Проверяем, есть ли у ViewModel метод validateAndCompleteIfValid
                                    try {
                                        viewModel.validateAndCompleteIfValid(state.data)
                                    } catch (e: Exception) {
                                        Timber.e("Ошибка при вызове validateAndCompleteIfValid: ${e.message}")
                                        onForward()
                                    }
                                } else {
                                    onForward()
                                }
                            }
                        }
                    },
                    enabled = forwardEnabled && !isReallyLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(forwardText)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Вперед",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardStepHeader(step: ActionStep) {
    Text(
        text = step.promptText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}