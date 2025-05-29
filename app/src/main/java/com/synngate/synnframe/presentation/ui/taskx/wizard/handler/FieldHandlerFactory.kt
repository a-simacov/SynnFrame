package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import timber.log.Timber

/**
 * Фабрика для создания обработчиков полей в зависимости от типа поля
 */
class FieldHandlerFactory(
    private val validationService: ValidationService,
    private val productUseCases: ProductUseCases
) {
    /**
     * Создает подходящий обработчик для указанного типа поля
     *
     * @param fieldType Тип поля, для которого нужен обработчик
     * @return Подходящий обработчик или null, если тип не поддерживается
     */
    fun createHandler(fieldType: FactActionField): FieldHandler<*>? {
        // Используем методы isApplicableField для более гибкого определения подходящего обработчика
        return when {
            BinFieldHandler.isApplicableField(fieldType, true) ->
                BinFieldHandler(validationService, true)

            BinFieldHandler.isApplicableField(fieldType, false) ->
                BinFieldHandler(validationService, false)

            PalletFieldHandler.isApplicableField(fieldType, true) ->
                PalletFieldHandler(validationService, true)

            PalletFieldHandler.isApplicableField(fieldType, false) ->
                PalletFieldHandler(validationService, false)

            ProductClassifierHandler.isApplicableField(fieldType) ->
                ProductClassifierHandler(validationService, productUseCases)

            TaskProductHandler.isApplicableField(fieldType) ->
                TaskProductHandler(validationService, productUseCases)

            QuantityFieldHandler.isApplicableField(fieldType) ->
                QuantityFieldHandler(validationService)

            else -> {
                Timber.w("Неподдерживаемый тип поля: $fieldType")
                null
            }
        }
    }

    /**
     * Создает обработчик для указанного объекта
     *
     * @param obj Объект, для которого нужен обработчик
     * @return Подходящий обработчик или null, если объект не поддерживается
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> createHandlerForObject(obj: T): FieldHandler<T>? {
        return when (obj) {
            is BinX -> BinFieldHandler(validationService, true) as FieldHandler<T>
            is Pallet -> PalletFieldHandler(validationService, true) as FieldHandler<T>
            is Product -> ProductClassifierHandler(validationService, productUseCases) as FieldHandler<T>
            is TaskProduct -> TaskProductHandler(validationService, productUseCases) as FieldHandler<T>
            is Float -> QuantityFieldHandler(validationService) as FieldHandler<T>
            is Number -> QuantityFieldHandler(validationService) as FieldHandler<T>
            else -> {
                Timber.w("Неподдерживаемый тип объекта: ${obj.javaClass.simpleName}")
                null
            }
        }
    }

    /**
     * Создает обработчик для указанного шага
     *
     * @param step Шаг, для которого нужен обработчик
     * @return Подходящий обработчик или null, если тип не поддерживается
     */
    fun createHandlerForStep(step: ActionStepTemplate): FieldHandler<*>? {
        return createHandler(step.factActionField)
    }
}