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

class FieldHandlerFactory(
    private val validationService: ValidationService,
    private val productUseCases: ProductUseCases
) {

    fun createHandler(fieldType: FactActionField): FieldHandler<*>? {
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

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> createHandlerForObject(obj: T, isStorage: Boolean): FieldHandler<T>? {
        return when (obj) {
            is BinX -> BinFieldHandler(validationService, isStorage) as FieldHandler<T>
            is Pallet -> PalletFieldHandler(validationService, isStorage) as FieldHandler<T>
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

    fun createHandlerForStep(step: ActionStepTemplate): FieldHandler<*>? {
        return createHandler(step.factActionField)
    }
}