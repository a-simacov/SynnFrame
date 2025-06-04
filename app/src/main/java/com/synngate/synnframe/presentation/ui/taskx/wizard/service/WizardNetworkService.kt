package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.StepCommandApi
import com.synngate.synnframe.data.remote.api.StepObjectApi
import com.synngate.synnframe.data.remote.dto.CommandExecutionRequestDto
import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.StepObjectMapperService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import com.synngate.synnframe.presentation.ui.taskx.dto.BinDto
import com.synngate.synnframe.presentation.ui.taskx.dto.PalletDto
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskProductDto
import com.synngate.synnframe.presentation.ui.taskx.entity.StepCommand
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class WizardNetworkService(
    private val taskXRepository: TaskXRepository,
    private val stepObjectApi: StepObjectApi,
    private val stepCommandApi: StepCommandApi,
    private val stepObjectMapperService: StepObjectMapperService,
    private val productUseCases: ProductUseCases // Добавлен для создания объектов из командных результатов
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
     * Выполняет команду с указанными параметрами
     */
    suspend fun executeCommand(
        command: StepCommand,
        stepId: String,
        factAction: FactAction,
        parameters: Map<String, String> = emptyMap(),
        additionalContext: Map<String, String> = emptyMap()
    ): NetworkResult<CommandExecutionResult> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Выполнение команды: ${command.name} (${command.id})")

            val requestDto = CommandExecutionRequestDto(
                commandId = command.id,
                stepId = stepId,
                factAction = FactActionRequestDto.fromDomain(factAction),
                parameters = parameters,
                additionalContext = additionalContext
            )

            val result = stepCommandApi.executeCommand(command.endpoint, requestDto)

            when (result) {
                is ApiResult.Success -> {
                    val response = result.data

                    val executionResult = CommandExecutionResult(
                        success = response.success,
                        message = response.message,
                        resultData = response.resultData ?: emptyMap(),
                        nextAction = response.nextAction,
                        updatedFactAction = mapUpdatedFactAction(response.updatedFactAction, factAction),
                        commandBehavior = command.executionBehavior
                    )

                    if (response.success) {
                        Timber.d("Команда ${command.id} выполнена успешно")
                        NetworkResult.success(executionResult)
                    } else {
                        Timber.w("Команда ${command.id} завершилась с ошибкой: ${response.message}")
                        NetworkResult.error(response.message ?: "Команда завершилась с ошибкой")
                    }
                }
                is ApiResult.Error -> {
                    Timber.e("Ошибка API при выполнении команды ${command.id}: ${result.message}")
                    NetworkResult.error(result.message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Исключение при выполнении команды ${command.id}")
            NetworkResult.error("Ошибка: ${e.message}")
        }
    }

    /**
     * Создает объект из данных результата команды
     */
    suspend fun createObjectFromResultData(
        resultData: Map<String, String>,
        fieldType: FactActionField
    ): Any? {
        try {
            return when (fieldType) {
                FactActionField.STORAGE_BIN, FactActionField.ALLOCATION_BIN -> {
                    val code = resultData["binCode"] ?: return null
                    val zone = resultData["binZone"] ?: ""
                    BinX(code, zone)
                }

                FactActionField.STORAGE_PALLET, FactActionField.ALLOCATION_PALLET -> {
                    val code = resultData["palletCode"] ?: return null
                    val isClosed = resultData["palletIsClosed"]?.toBooleanStrictOrNull() ?: false
                    Pallet(code, isClosed)
                }

                FactActionField.QUANTITY -> {
                    val quantity = resultData["quantity"]?.toFloatOrNull() ?: return null
                    quantity
                }

                FactActionField.STORAGE_PRODUCT_CLASSIFIER -> {
                    val productId = resultData["productId"] ?: return null
                    productUseCases.getProductById(productId)
                }

                FactActionField.STORAGE_PRODUCT -> {
                    val productId = resultData["productId"] ?: return null
                    val product = productUseCases.getProductById(productId) ?: return null

                    val expirationDate = resultData["expirationDate"]?.let {
                        try {
                            LocalDateTime.parse(it)
                        } catch (e: Exception) {
                            Timber.e(e, "Ошибка парсинга даты: $it")
                            null
                        }
                    }

                    TaskProduct(
                        id = resultData["taskProductId"] ?: UUID.randomUUID().toString(),
                        product = product,
                        expirationDate = expirationDate,
                        status = ProductStatus.fromString(resultData["productStatus"] ?: "STANDARD")
                    )
                }

                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании объекта из результата команды")
            return null
        }
    }

    /**
     * Преобразует DTO в доменную модель FactAction
     */
    private fun mapUpdatedFactAction(
        dto: FactActionRequestDto?,
        currentFactAction: FactAction
    ): FactAction? {
        if (dto == null) return null

        try {
            return currentFactAction.copy(
                // Обновляем базовые поля
                quantity = dto.quantity,

                // Обновляем объекты, если они есть в DTO
                storageProduct = dto.storageProduct?.let { mapToTaskProduct(it) }
                    ?: currentFactAction.storageProduct,

                storageProductClassifier = dto.storageProductClassifier?.let { mapToProduct(it) }
                    ?: currentFactAction.storageProductClassifier,

                storageBin = dto.storageBin?.let { mapToBinX(it) }
                    ?: currentFactAction.storageBin,

                storagePallet = dto.storagePallet?.let { mapToPallet(it) }
                    ?: currentFactAction.storagePallet,

                placementBin = dto.placementBin?.let { mapToBinX(it) }
                    ?: currentFactAction.placementBin,

                placementPallet = dto.placementPallet?.let { mapToPallet(it) }
                    ?: currentFactAction.placementPallet
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при маппинге FactActionRequestDto в FactAction")
            return null
        }
    }

    /**
     * Вспомогательные методы маппинга DTO в доменные объекты
     */
    private fun mapToProduct(dto: com.synngate.synnframe.data.remote.dto.ProductDto): Product {
        return Product(
            id = dto.id,
            name = dto.name,
            articleNumber = dto.articleNumber ?: ""
        )
    }

    private fun mapToTaskProduct(dto: TaskProductDto): TaskProduct {
        return TaskProduct(
            id = dto.id,
            product = mapToProduct(dto.product),
            expirationDate = dto.expirationDate?.let {
                try {
                    LocalDateTime.parse(it)
                } catch (e: Exception) {
                    null
                }
            },
            status = ProductStatus.fromString(dto.status)
        )
    }

    private fun mapToBinX(dto: BinDto): BinX {
        return BinX(
            code = dto.code,
            zone = dto.zone
        )
    }

    private fun mapToPallet(dto: PalletDto): Pallet {
        return Pallet(
            code = dto.code,
            isClosed = dto.isClosed
        )
    }

    /**
     * Проверяет, соответствует ли полученный объект ожидаемому типу поля
     */
    private fun isObjectCompatibleWithField(obj: Any, fieldType: FactActionField): Boolean {
        return when (fieldType) {
            FactActionField.STORAGE_BIN, FactActionField.ALLOCATION_BIN ->
                obj is BinX

            FactActionField.STORAGE_PALLET, FactActionField.ALLOCATION_PALLET ->
                obj is Pallet

            FactActionField.STORAGE_PRODUCT_CLASSIFIER ->
                obj is Product

            FactActionField.STORAGE_PRODUCT ->
                obj is TaskProduct

            FactActionField.QUANTITY ->
                obj is Number || obj is Float

            else -> false
        }
    }
}