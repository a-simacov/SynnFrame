package com.synngate.synnframe.presentation.ui.wizard

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.model.wizard.WizardStateMachine
import com.synngate.synnframe.domain.service.AutoFillManager
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ActionWizardViewModel(
    val taskId: String,
    val actionId: String,
    val wizardStateMachine: WizardStateMachine,
    val actionStepFactoryRegistry: ActionStepFactoryRegistry,
    private val taskContextManager: TaskContextManager
) : BaseViewModel<Unit, ActionWizardEvent>(Unit), Disposable {
    private val TAG = "ActionWizardViewModel"
    private var observingJob: Job? = null

    private val safeToNavigateBack = AtomicBoolean(false)

    // Создаем AutoFillManager для управления автозаполнением
    private val autoFillManager = AutoFillManager(taskContextManager)

    init {
        viewModelScope.launch {
            try {
                val initResult = wizardStateMachine.initialize(taskId, actionId)

                if (!initResult.isSuccess) {
                    val error = initResult.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    Timber.e("$TAG: Ошибка инициализации: $error")
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $error"))
                    sendEvent(ActionWizardEvent.NavigateBack)
                } else {
                    // Добавляем значительную задержку (2 секунды) перед активацией
                    // механизма автоматической навигации назад
                    viewModelScope.launch {
                        delay(2000) // 2 секунды
                        safeToNavigateBack.set(true)
                        startObservingState()
                    }

                    // Проверяем наличие сохраняемых объектов для текущего действия
                    checkSavableObjects()
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка инициализации визарда: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка инициализации: ${e.message}"))
                sendEvent(ActionWizardEvent.NavigateBack)
            }
        }
    }

    private fun startObservingState() {
        // Отменяем предыдущую задачу, если она существует
        observingJob?.cancel()

        observingJob = viewModelScope.launch {
            try {
                wizardStateMachine.state
                    .map { it.isUninitialized && wizardStateMachine.isInitialized() && safeToNavigateBack.get() }
                    .distinctUntilChanged()
                    .collectLatest { isReset ->
                        if (isReset) {
                            sendEvent(ActionWizardEvent.NavigateBack)
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка при наблюдении за состоянием: ${e.message}")
            }
        }
    }

    fun completeWizard() {
        launchIO {
            try {
                // Сохраняем объекты перед завершением
                processSavableObjectsFromWizard()

                val result = wizardStateMachine.complete()

                if (result.isSuccess) {
                    sendEvent(ActionWizardEvent.NavigateBackWithSuccess(actionId))
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: $errorMessage"))
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка завершения визарда: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка завершения: ${e.message}"))
            }
        }
    }

    fun retryCompleteWizard() {
        completeWizard()
    }

    fun cancelWizard() {
        wizardStateMachine.cancel()
        sendEvent(ActionWizardEvent.NavigateBack)
    }

    fun goBackToPreviousStep() {
        launchIO {
            try {
                wizardStateMachine.processStepResult(null)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка навигации: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка навигации: ${e.message}"))
            }
        }
    }

    fun processBarcodeFromScanner(barcode: String) {
        wizardStateMachine.processBarcodeFromScanner(barcode)
    }

    fun processStepResult(result: Any?) {
        launchIO {
            try {
                wizardStateMachine.processStepResult(result)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Ошибка обработки результата шага: ${e.message}")
                sendEvent(ActionWizardEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    private fun checkSavableObjects() {
        val currentState = wizardStateMachine.state.value
        if (!currentState.isInitialized) return

        val taskType = taskContextManager.lastTaskTypeX.value
        if (taskType == null || taskType.savableObjectTypes.isEmpty()) return

        Timber.d("$TAG: Checking savable objects for steps")

        val availableObjects = taskContextManager.savableObjects.value
        if (availableObjects.isNotEmpty()) {
            Timber.d("$TAG: Available savable objects: ${availableObjects.size}")
            availableObjects.forEach { obj ->
                Timber.d("$TAG: Savable object: ${obj.objectType} - ${obj.getShortDescription()}")
            }
        }
    }

    private fun processSavableObjectsFromWizard() {
        val currentState = wizardStateMachine.state.value
        if (!currentState.isInitialized) return

        val results = currentState.results

        val markedObjects = results.entries
            .filter { it.key.startsWith("savableObject_") }
            .mapNotNull { entry ->
                val typeStr = entry.key.removePrefix("savableObject_")
                val type = try {
                    ActionObjectType.valueOf(typeStr)
                } catch (e: Exception) {
                    null
                }

                if (type != null) {
                    type to entry.value
                } else null
            }
            .toMap()

        if (markedObjects.isNotEmpty()) {
            markedObjects.forEach { (type, data) ->
                taskContextManager.addSavableObject(type, data, "action:$actionId")
            }

            Timber.d("$TAG: Saved ${markedObjects.size} objects from wizard")
        }
    }

    override fun dispose() {
        Timber.d("$TAG: Освобождение ресурсов")
        super.dispose()

        try {
            observingJob?.cancel()
            observingJob = null

            actionStepFactoryRegistry.clearAllCaches()

            wizardStateMachine.dispose()

            actionStepFactoryRegistry.dispose()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Ошибка при освобождении ресурсов")
        }
    }

    fun onDispose() {
        dispose()
    }
}