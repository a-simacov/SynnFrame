package com.synngate.synnframe.presentation.ui.tasks

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailEvent
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailState
import com.synngate.synnframe.presentation.ui.tasks.model.TaskLineItem
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.UUID

class TaskDetailViewModel(
    private val taskId: String,
    private val taskUseCases: TaskUseCases,
    private val productUseCases: ProductUseCases,
    private val userUseCases: UserUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<TaskDetailState, TaskDetailEvent>(TaskDetailState(taskId = taskId)) {

    init {
        loadTask()
    }

    /**
     * Загружает задание и его детали
     */
    private fun loadTask() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val task = taskUseCases.getTaskById(taskId)

                if (task != null) {
                    // Получаем информацию о продуктах для строк плана и факта
                    val productIds = task.planLines.map { it.productId }.toSet()
                    val products = mutableMapOf<String, Product>()

                    for (productId in productIds) {
                        val product = productUseCases.getProductById(productId)
                        if (product != null) {
                            products[productId] = product
                        }
                    }

                    // Комбинируем строки плана и факта
                    val taskLines = task.planLines.map { planLine ->
                        val factLine = task.factLines.find { it.productId == planLine.productId }
                        val product = products[planLine.productId]

                        TaskLineItem(
                            planLine = planLine,
                            factLine = factLine,
                            product = product
                        )
                    }

                    // Проверяем, доступно ли редактирование задания
                    val isEditable = task.status == TaskStatus.IN_PROGRESS

                    updateState { state ->
                        state.copy(
                            task = task,
                            taskLines = taskLines,
                            isLoading = false,
                            isEditable = isEditable,
                            error = null
                        )
                    }
                } else {
                    updateState { state ->
                        state.copy(
                            isLoading = false,
                            error = "Задание не найдено"
                        )
                    }
                    sendEvent(TaskDetailEvent.ShowSnackbar("Задание не найдено"))
                    sendEvent(TaskDetailEvent.NavigateBack)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading task")
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки задания"
                    )
                }
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка загрузки задания: ${e.message}"))
            }
        }
    }

    /**
     * Обрабатывает ввод в поле поиска товаров
     */
    fun onSearchQueryChanged(query: String) {
        updateState { it.copy(searchQuery = query) }

        // Обработка специальных команд
        when (query) {
            "0" -> {
                // Переход к списку товаров
                sendEvent(TaskDetailEvent.NavigateToProductsList)
                updateState { it.copy(searchQuery = "") }
                return
            }
            // Можно добавить другие специальные команды при необходимости
        }

        // Если введено достаточно символов для поиска, обрабатываем как штрихкод
        if (query.length >= MIN_BARCODE_LENGTH) {
            processBarcode(query)
        }
    }

    companion object {
        private const val MIN_BARCODE_LENGTH = 8 // Минимальная длина штрихкода для автоматического поиска
    }

    /**
     * Обрабатывает сканированный штрихкод
     */
    fun processBarcode(barcode: String) {
        if (barcode.isBlank()) {
            return
        }

        val task = uiState.value.task ?: return
        if (task.status != TaskStatus.IN_PROGRESS) {
            return
        }

        launchIO {
            try {
                // Поиск товара по штрихкоду
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    // Проверяем, есть ли товар в плане задания
                    val isInPlan = task.planLines.any { it.productId == product.id }

                    if (isInPlan) {
                        // Находим строку факта для этого товара или создаем новую
                        val factLine = task.factLines.find { it.productId == product.id }
                            ?: TaskFactLine(
                                id = UUID.randomUUID().toString(),
                                taskId = task.id,
                                productId = product.id,
                                quantity = 0f
                            )

                        updateState { state ->
                            state.copy(
                                scannedBarcode = barcode,
                                scannedProduct = product,
                                selectedFactLine = factLine,
                                isFactLineDialogVisible = true
                            )
                        }

                        sendEvent(TaskDetailEvent.ShowFactLineDialog(factLine))
                    } else {
                        updateState { state ->
                            state.copy(
                                scannedBarcode = barcode,
                                scannedProduct = product
                            )
                        }

                        sendEvent(TaskDetailEvent.ShowSnackbar("Товар '${product.name}' не входит в план задания"))
                    }
                } else {
                    updateState { state ->
                        state.copy(
                            scannedBarcode = barcode,
                            scannedProduct = null
                        )
                    }

                    sendEvent(TaskDetailEvent.ShowSnackbar("Товар с штрихкодом '$barcode' не найден"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обработки штрихкода: ${e.message}"))
            }
        }
    }

    /**
     * Показывает диалог сканирования штрихкодов
     */
    fun showScanDialog() {
        updateState { it.copy(isScanDialogVisible = true) }
        sendEvent(TaskDetailEvent.ShowScanDialog)
    }

    /**
     * Показывает диалог редактирования строки факта
     */
    fun showFactLineDialog(productId: String) {
        val state = uiState.value
        val task = state.task ?: return

        // Находим строку факта для указанного товара или создаем новую
        val factLine = task.factLines.find { it.productId == productId }
            ?: TaskFactLine(
                id = UUID.randomUUID().toString(),
                taskId = task.id,
                productId = productId,
                quantity = 0f
            )

        updateState {
            it.copy(
                selectedFactLine = factLine,
                isFactLineDialogVisible = true
            )
        }

        sendEvent(TaskDetailEvent.ShowFactLineDialog(factLine))
    }

    /**
     * Закрывает диалоги
     */
    fun closeDialog() {
        updateState {
            it.copy(
                isScanDialogVisible = false,
                isFactLineDialogVisible = false,
                isCompleteConfirmationVisible = false,
                selectedFactLine = null,
                additionalQuantity = ""
            )
        }

        sendEvent(TaskDetailEvent.CloseDialog)
    }

    /**
     * Обновляет дополнительное количество для строки факта
     */
    fun updateAdditionalQuantity(quantity: String) {
        updateState { it.copy(additionalQuantity = quantity) }
    }

    /**
     * Применяет изменения количества в строке факта
     */
    fun applyQuantityChange(factLine: TaskFactLine, additionalQuantity: String) {
        val task = uiState.value.task ?: return

        try {
            val addValue = additionalQuantity.toFloatOrNull() ?: 0f
            if (addValue == 0f) {
                return
            }

            val updatedQuantity = factLine.quantity + addValue
            val updatedFactLine = factLine.copy(quantity = updatedQuantity)

            launchIO {
                try {
                    taskUseCases.updateTaskFactLine(updatedFactLine)

                    // Обновляем задание
                    loadTask()

                    sendEvent(TaskDetailEvent.UpdateSuccess)
                    sendEvent(TaskDetailEvent.CloseDialog)
                } catch (e: Exception) {
                    Timber.e(e, "Error updating task fact line")
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обновления: ${e.message}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing quantity")
            sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка ввода количества"))
        }
    }

    /**
     * Начинает выполнение задания
     */
    fun startTask() {
        val task = uiState.value.task ?: return

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Получаем текущего пользователя
                val currentUser = userUseCases.getCurrentUser().first()

                if (currentUser != null) {
                    val result = taskUseCases.startTask(task.id, currentUser.id)

                    if (result.isSuccess) {
                        loadTask()
                        sendEvent(TaskDetailEvent.ShowSnackbar("Задание успешно начато"))
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                        sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: $error"))
                    }
                } else {
                    sendEvent(TaskDetailEvent.ShowSnackbar("Необходимо авторизоваться"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting task")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Показывает диалог подтверждения завершения задания
     */
    fun showCompleteConfirmation() {
        updateState { it.copy(isCompleteConfirmationVisible = true) }
    }

    /**
     * Завершает выполнение задания
     */
    fun completeTask() {
        val task = uiState.value.task ?: return

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskUseCases.completeTask(task.id)

                if (result.isSuccess) {
                    loadTask()
                    sendEvent(TaskDetailEvent.ShowSnackbar("Задание успешно завершено"))
                    sendEvent(TaskDetailEvent.CloseDialog)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing task")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState {
                    it.copy(
                        isProcessing = false,
                        isCompleteConfirmationVisible = false
                    )
                }
            }
        }
    }

    /**
     * Выгружает задание на сервер
     */
    fun uploadTask() {
        val task = uiState.value.task ?: return

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskUseCases.uploadTask(task.id)

                if (result.isSuccess) {
                    loadTask()
                    sendEvent(TaskDetailEvent.ShowSnackbar("Задание успешно выгружено"))
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка выгрузки: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error uploading task")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка выгрузки: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Обрабатывает выбранный товар со страницы выбора товаров
     */
    fun handleSelectedProduct(product: Product) {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            // Проверяем, есть ли товар в плане задания
            val isInPlan = task.planLines.any { it.productId == product.id }

            if (isInPlan) {
                // Находим или создаем строку факта
                val factLine = task.factLines.find { it.productId == product.id }
                    ?: TaskFactLine(
                        id = UUID.randomUUID().toString(),
                        taskId = task.id,
                        productId = product.id,
                        quantity = 0f
                    )

                // Показываем диалог редактирования строки факта
                updateState { state ->
                    state.copy(
                        scannedProduct = product,
                        selectedFactLine = factLine,
                        isFactLineDialogVisible = true
                    )
                }
            } else {
                // Если товара нет в плане задания, показываем сообщение
                sendEvent(TaskDetailEvent.ShowSnackbar(
                    "Товар '${product.name}' не входит в план задания"
                ))
            }
        }
    }

    /**
     * Выполняет пакетное обновление строк факта
     */
    fun batchUpdateFactLines(updates: List<Pair<TaskFactLine, Float>>) {
        if (updates.isEmpty()) return

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Обновляем все строки факта одной транзакцией
                updates.forEach { (factLine, newQuantity) ->
                    val updatedFactLine = factLine.copy(quantity = newQuantity)
                    taskUseCases.updateTaskFactLine(updatedFactLine)
                }

                // Перезагружаем задание после всех обновлений
                loadTask()

                sendEvent(TaskDetailEvent.UpdateSuccess)
            } catch (e: Exception) {
                Timber.e(e, "Error batch updating task fact lines")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обновления: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Улучшенная обработка результатов сканирования штрихкода
     */
    fun processScanResult(barcode: String) {
        // Зафиксируем время начала сканирования для расчета длительности операции
        val startTime = System.currentTimeMillis()

        launchIO {
            updateState { it.copy(isProcessing = true, scannedBarcode = barcode) }

            try {
                // Поиск товара по штрихкоду
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    val task = uiState.value.task
                    if (task == null) {
                        sendEvent(TaskDetailEvent.ShowSnackbar("Задание не найдено"))
                        return@launchIO
                    }

                    // Проверяем, есть ли товар в плане задания
                    val isInPlan = task.isProductInPlan(product.id)

                    if (isInPlan) {
                        // Находим или создаем строку факта
                        val factLine = task.getFactLineByProductId(product.id)
                            ?: TaskFactLine(
                                id = UUID.randomUUID().toString(),
                                taskId = task.id,
                                productId = product.id,
                                quantity = 0f
                            )

                        // Обновляем состояние для отображения диалога
                        updateState { state ->
                            state.copy(
                                scannedProduct = product,
                                selectedFactLine = factLine,
                                isFactLineDialogVisible = true,
                                isProcessing = false
                            )
                        }

                        // Звуковое оповещение об успешном сканировании (можно реализовать позже)
                        // playSuccessSound()
                    } else {
                        // Если товара нет в плане, показываем сообщение
                        sendEvent(TaskDetailEvent.ShowSnackbar(
                            "Товар '${product.name}' не входит в план задания"
                        ))

                        updateState { state ->
                            state.copy(
                                scannedProduct = product,
                                isProcessing = false
                            )
                        }

                        // Звуковое оповещение об ошибке (можно реализовать позже)
                        // playErrorSound()
                    }
                } else {
                    // Если товар не найден, показываем сообщение
                    sendEvent(TaskDetailEvent.ShowSnackbar(
                        "Товар со штрихкодом '$barcode' не найден"
                    ))

                    updateState { state ->
                        state.copy(
                            scannedProduct = null,
                            isProcessing = false
                        )
                    }

                    // Звуковое оповещение об ошибке (можно реализовать позже)
                    // playErrorSound()
                }

                // Логируем длительность операции сканирования для аналитики
                val duration = System.currentTimeMillis() - startTime
                Timber.d("Scan processing took $duration ms")

            } catch (e: Exception) {
                Timber.e(e, "Error processing barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обработки штрихкода: ${e.message}"))
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Возвращается на предыдущий экран
     */
    fun navigateBack() {
        sendEvent(TaskDetailEvent.NavigateBack)
    }
}