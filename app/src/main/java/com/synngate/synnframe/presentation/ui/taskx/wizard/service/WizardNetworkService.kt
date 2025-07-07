package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.StepCommandApi
import com.synngate.synnframe.data.remote.api.StepObjectApi
import com.synngate.synnframe.data.remote.dto.CommandExecutionRequestDto
import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.data.remote.service.StepObjectMapperService
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
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
                    ?: return NetworkResult.error("Endpoint not defined")
                val result = taskXRepository.addFactAction(updatedFactAction, endpoint)

                if (result.isSuccess) {
                    val updatedTask = result.getOrNull()
                    if (updatedTask != null) {
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                    }
                    return NetworkResult.success()
                } else {
                    return NetworkResult.error("Sending error: ${result.exceptionOrNull()?.message}")
                }
            } else {
                TaskXDataHolderSingleton.addFactAction(updatedFactAction)
                return NetworkResult.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error completing action: ${e.message}")
            return NetworkResult.error("Error: ${e.message}")
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
            return@withContext NetworkResult.error("Endpoint not specified for object retrieval")
        }

        try {
            Timber.d("Request object from server: $endpoint for field $fieldType")
            val result = stepObjectApi.getStepObject(endpoint, factAction)

            return@withContext when (result) {
                is ApiResult.Success -> {
                    val data = result.data
                    if (!data.success) {
                        Timber.w("Server returned error: ${data.errorMessage}")
                        NetworkResult.error(data.errorMessage ?: "Error retrieving object from server")
                    } else {
                        val mappedObject = stepObjectMapperService.mapResponseToObject(data, fieldType)
                        if (mappedObject != null) {
                            // Проверяем, соответствует ли тип объекта ожидаемому типу поля
                            val isCompatible = isObjectCompatibleWithField(mappedObject, fieldType)
                            if (isCompatible) {
                                Timber.d("Object successfully received from server: ${mappedObject.javaClass.simpleName}")
                                NetworkResult.success(mappedObject)
                            } else {
                                Timber.w("Server returned incompatible type object: ${mappedObject.javaClass.simpleName}")
                                NetworkResult.error("Server returned object of incorrect type")
                            }
                        } else {
                            Timber.w("Failed to convert object from server response")
                            NetworkResult.error("Failed to convert object from server response")
                        }
                    }
                }
                is ApiResult.Error -> {
                    Timber.e("API error retrieving object: ${result.message}")
                    NetworkResult.error(result.message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception when requesting object from server: $endpoint")
            NetworkResult.error("Error: ${e.message}")
        }
    }

    /**
     * Получает обновления для полей объекта с сервера
     */
    suspend fun getFieldUpdates(
        endpoint: String,
        factAction: FactAction,
        selectedObject: Any,
        fieldType: FactActionField
    ): NetworkResult<Any> = withContext(Dispatchers.IO) {
        if (endpoint.isEmpty()) {
            return@withContext NetworkResult.error("Update endpoint not specified")
        }

        try {
            Timber.d("Request field updates from server: $endpoint for field $fieldType")
            
            // Заменяем плейсхолдеры в endpoint
            var processedEndpoint = endpoint
            if (processedEndpoint.contains("{taskId}")) {
                processedEndpoint = processedEndpoint.replace("{taskId}", factAction.taskId)
            }

            // Делаем GET запрос с factAction в качестве параметров
            val result = stepObjectApi.getStepObject(processedEndpoint, factAction)
            
            return@withContext when (result) {
                is ApiResult.Success -> {
                    val data = result.data
                    if (!data.success) {
                        Timber.w("Server returned error: ${data.errorMessage}")
                        NetworkResult.error(data.errorMessage ?: "Error retrieving field updates from server")
                    } else {
                        // Маппим ответ в объект того же типа, что и selectedObject
                        val updateObject = stepObjectMapperService.mapResponseToObject(data, fieldType)
                        if (updateObject != null) {
                            // Объединяем поля из updateObject с selectedObject
                            val updatedObject = mergeObjects(selectedObject, updateObject, fieldType)
                            Timber.d("Field updates successfully received and applied")
                            NetworkResult.success(updatedObject)
                        } else {
                            Timber.w("Failed to convert field updates from server response")
                            NetworkResult.error("Failed to convert field updates from server response")
                        }
                    }
                }
                is ApiResult.Error -> {
                    Timber.e("API error retrieving field updates: ${result.message}")
                    NetworkResult.error(result.message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception when requesting field updates from server: $endpoint")
            NetworkResult.error("Error: ${e.message}")
        }
    }
    
    /**
     * Объединяет поля из updateObject в selectedObject
     */
    private fun mergeObjects(selectedObject: Any, updateObject: Any, fieldType: FactActionField): Any {
        return when (fieldType) {
            FactActionField.STORAGE_BIN, FactActionField.ALLOCATION_BIN -> {
                val selected = selectedObject as BinX
                val update = updateObject as BinX
                selected.copy(
                    code = update.code.ifEmpty { selected.code },
                    zone = update.zone.ifEmpty { selected.zone }
                )
            }
            
            FactActionField.STORAGE_PALLET, FactActionField.ALLOCATION_PALLET -> {
                val selected = selectedObject as Pallet
                val update = updateObject as Pallet
                selected.copy(
                    code = update.code.ifEmpty { selected.code },
                    isClosed = update.isClosed
                )
            }
            
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> {
                val selected = selectedObject as Product
                val update = updateObject as Product
                selected.copy(
                    id = update.id.ifEmpty { selected.id },
                    name = update.name.ifEmpty { selected.name },
                    articleNumber = update.articleNumber.ifEmpty { selected.articleNumber },
                    weight = if (update.weight != 0.0f) update.weight else selected.weight,
                    accountingModel = if (update.accountingModel != selected.accountingModel) update.accountingModel else selected.accountingModel,
                    mainUnitId = update.mainUnitId.ifEmpty { selected.mainUnitId },
                    units = if (update.units.isNotEmpty()) update.units else selected.units
                )
            }
            
            FactActionField.STORAGE_PRODUCT -> {
                val selected = selectedObject as TaskProduct
                val update = updateObject as TaskProduct
                
                // Объединяем поля Product внутри TaskProduct
                val mergedProduct = if (update.product.id.isNotEmpty()) {
                    selected.product.copy(
                        id = update.product.id.ifEmpty { selected.product.id },
                        name = update.product.name.ifEmpty { selected.product.name },
                        articleNumber = update.product.articleNumber.ifEmpty { selected.product.articleNumber },
                        weight = if (update.product.weight != 0.0f) update.product.weight else selected.product.weight,
                        accountingModel = if (update.product.accountingModel != selected.product.accountingModel) update.product.accountingModel else selected.product.accountingModel,
                        mainUnitId = update.product.mainUnitId.ifEmpty { selected.product.mainUnitId },
                        units = if (update.product.units.isNotEmpty()) update.product.units else selected.product.units
                    )
                } else {
                    selected.product
                }
                
                selected.copy(
                    id = update.id.ifEmpty { selected.id },
                    product = mergedProduct,
                    expirationDate = update.expirationDate ?: selected.expirationDate,
                    status = if (update.status != ProductStatus.STANDARD) update.status else selected.status
                )
            }
            
            FactActionField.QUANTITY -> {
                // Для количества просто возвращаем новое значение
                updateObject
            }
            
            else -> selectedObject
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
            Timber.d("Executing command: ${command.name} (${command.id})")

            val requestDto = CommandExecutionRequestDto(
                commandId = command.id,
                stepId = stepId,
                factAction = FactActionRequestDto.fromDomain(factAction),
                parameters = parameters,
                additionalContext = additionalContext
            )

            var taskIdEndpoint = command.endpoint
            if (taskIdEndpoint.contains("{taskId}")) {
                taskIdEndpoint = taskIdEndpoint.replace("{taskId}", factAction.taskId)
            }

            val result = stepCommandApi.executeCommand(taskIdEndpoint, requestDto)

            when (result) {
                is ApiResult.Success -> {
                    val response = result.data

                    val executionResult = CommandExecutionResult(
                        success = response.success,
                        message = response.message,
                        resultData = response.resultData ?: emptyMap(),
                        nextAction = response.nextAction,
                        updatedFactAction = response.updatedFactAction,
                        commandBehavior = command.executionBehavior
                    )

                    if (response.success) {
                        Timber.d("Command ${command.id} executed successfully")
                        NetworkResult.success(executionResult)
                    } else {
                        Timber.w("Command ${command.id} completed with error: ${response.message}")
                        NetworkResult.error(response.message ?: "Command completed with error")
                    }
                }
                is ApiResult.Error -> {
                    Timber.e("API error executing command ${command.id}: ${result.message}")
                    NetworkResult.error(result.message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while executing command ${command.id}")
            NetworkResult.error("Error: ${e.message}")
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
                            Timber.e(e, "Error parsing date: $it")
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
            Timber.e(e, "Error creating object from command result")
            return null
        }
    }

    /**
     * Публичный метод для преобразования DTO в доменную модель FactAction
     * Используется из ViewModel
     */
    fun mapDtoToFactAction(
        dto: FactActionRequestDto,
        currentFactAction: FactAction
    ): FactAction? {
        return mapUpdatedFactAction(dto, currentFactAction)
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
            // Получаем доменные объекты из DTO
            val storageProduct = dto.storageProduct?.let { dtoTaskProduct ->
                // Сначала нужно получить доменный Product из ProductDto
                val productDto = dtoTaskProduct.product
                val product = Product(
                    id = productDto.id,
                    name = productDto.name,
                    articleNumber = productDto.articleNumber ?: ""
                )

                // Теперь создаем TaskProduct
                TaskProduct(
                    id = dtoTaskProduct.id,
                    product = product,
                    expirationDate = dtoTaskProduct.expirationDate?.let {
                        try { LocalDateTime.parse(it.toString()) } catch (e: Exception) { null }
                    },
                    status = ProductStatus.fromString(dtoTaskProduct.status.toString())
                )
            }

            val storageProductClassifier = dto.storageProductClassifier?.let { productDto ->
                Product(
                    id = productDto.id,
                    name = productDto.name,
                    articleNumber = productDto.articleNumber ?: ""
                )
            }

            val storageBin = dto.storageBin?.let { binDto ->
                BinX(code = binDto.code, zone = binDto.zone)
            }

            val storagePallet = dto.storagePallet?.let { palletDto ->
                Pallet(code = palletDto.code, isClosed = palletDto.isClosed)
            }

            val placementBin = dto.placementBin?.let { binDto ->
                BinX(code = binDto.code, zone = binDto.zone)
            }

            val placementPallet = dto.placementPallet?.let { palletDto ->
                Pallet(code = palletDto.code, isClosed = palletDto.isClosed)
            }

            return currentFactAction.copy(
                quantity = dto.quantity,
                storageProduct = storageProduct ?: currentFactAction.storageProduct,
                storageProductClassifier = storageProductClassifier ?: currentFactAction.storageProductClassifier,
                storageBin = storageBin ?: currentFactAction.storageBin,
                storagePallet = storagePallet ?: currentFactAction.storagePallet,
                placementBin = placementBin ?: currentFactAction.placementBin,
                placementPallet = placementPallet ?: currentFactAction.placementPallet
            )
        } catch (e: Exception) {
            Timber.e(e, "Error mapping FactActionRequestDto to FactAction")
            return null
        }
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