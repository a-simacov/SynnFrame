package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.StepObjectApi
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.StepObjectMapperService
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime

class WizardNetworkService(
    private val taskXRepository: TaskXRepository,
    private val stepObjectApi: StepObjectApi,
    private val stepObjectMapperService: StepObjectMapperService
) {
    suspend fun completeAction(
        factAction: FactAction,
        syncWithServer: Boolean
    ): NetworkResult<Unit> {
        try {
            val updatedFactAction = factAction.copy(completedAt = LocalDateTime.now())

            if (syncWithServer) {
                val endpoint = TaskXDataHolderSingleton.endpoint
                    ?: return NetworkResult.error("Endpoint не определен")
                val result = taskXRepository.addFactAction(updatedFactAction, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                    }
                    return NetworkResult.success()
                } else {
                    return NetworkResult.error("Ошибка отправки: ${result.exceptionOrNull()?.message}")
                }
            } else {
                TaskXDataHolderSingleton.addFactAction(updatedFactAction)
                return NetworkResult.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка завершения действия: ${e.message}")
            return NetworkResult.error("Ошибка: ${e.message}")
        }
    }

    /**
     * Получает объект шага с сервера через конфигурируемый endpoint
     *
     * @param endpoint Конечная точка API для получения объекта
     * @param factAction Текущий объект фактического действия (уже содержит все данные предыдущих шагов)
     * @param fieldType Тип поля, для которого запрашивается объект
     * @return Результат сетевого запроса с объектом или ошибкой
     */
    suspend fun getStepObject(
        endpoint: String,
        factAction: FactAction,
        fieldType: FactActionField
    ): NetworkResult<Any> = withContext(Dispatchers.IO) {
        if (endpoint.isEmpty()) {
            return@withContext NetworkResult.error("Endpoint не указан для получения объекта")
        }

        try {
            Timber.d("Запрос объекта с сервера: $endpoint для поля $fieldType")
            val result = stepObjectApi.getStepObject(endpoint, factAction)

            return@withContext when (result) {
                is ApiResult.Success -> {
                    val data = result.data
                    if (!data.success) {
                        Timber.w("Сервер вернул ошибку: ${data.errorMessage}")
                        NetworkResult.error(data.errorMessage ?: "Ошибка при получении объекта с сервера")
                    } else {
                        val mappedObject = stepObjectMapperService.mapResponseToObject(data, fieldType)
                        if (mappedObject != null) {
                            // Проверяем, соответствует ли тип объекта ожидаемому типу поля
                            val isCompatible = isObjectCompatibleWithField(mappedObject, fieldType)
                            if (isCompatible) {
                                Timber.d("Объект успешно получен с сервера: ${mappedObject.javaClass.simpleName}")
                                NetworkResult.success(mappedObject)
                            } else {
                                Timber.w("Сервер вернул объект несовместимого типа: ${mappedObject.javaClass.simpleName}")
                                NetworkResult.error("Сервер вернул объект неверного типа")
                            }
                        } else {
                            Timber.w("Не удалось преобразовать объект из ответа сервера")
                            NetworkResult.error("Не удалось преобразовать объект из ответа сервера")
                        }
                    }
                }
                is ApiResult.Error -> {
                    Timber.e("Ошибка API при получении объекта: ${result.message}")
                    NetworkResult.error(result.message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Исключение при запросе объекта с сервера: $endpoint")
            NetworkResult.error("Ошибка: ${e.message}")
        }
    }

    /**
     * Проверяет, соответствует ли полученный объект ожидаемому типу поля
     */
    private fun isObjectCompatibleWithField(obj: Any, fieldType: FactActionField): Boolean {
        return when (fieldType) {
            FactActionField.STORAGE_BIN, FactActionField.ALLOCATION_BIN ->
                obj is com.synngate.synnframe.domain.entity.taskx.BinX

            FactActionField.STORAGE_PALLET, FactActionField.ALLOCATION_PALLET ->
                obj is com.synngate.synnframe.domain.entity.taskx.Pallet

            FactActionField.STORAGE_PRODUCT_CLASSIFIER ->
                obj is com.synngate.synnframe.domain.entity.Product

            FactActionField.STORAGE_PRODUCT ->
                obj is com.synngate.synnframe.domain.entity.taskx.TaskProduct

            FactActionField.QUANTITY ->
                obj is Number || obj is Float

            else -> false
        }
    }
}