package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.domain.usecase.wizard.FactLineWizardUseCases
import java.time.LocalDateTime
import java.util.UUID

/**
 * Сервис для работы со строками факта
 */
class FactLineService(
    private val factLineWizardUseCases: FactLineWizardUseCases
) {
    /**
     * Создает строку факта из результатов визарда
     */
    fun createFactLineFromResults(
        taskId: String,
        results: WizardResultModel,
        startTime: LocalDateTime
    ): FactLineX {
        // Получаем данные для строки факта
        val storageProduct = results.storageProduct
        val storagePallet = results.storagePallet
        val placementPallet = results.placementPallet
        val placementBin = results.placementBin
        val wmsAction = results.wmsAction ?: WmsAction.RECEIPT

        // Проверка наличия обязательных данных
        if (storageProduct == null) {
            throw IllegalStateException("Product was not specified")
        }

        // Создаем строку факта
        return FactLineX(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            storageProduct = storageProduct,
            storagePallet = storagePallet,
            wmsAction = wmsAction,
            placementPallet = placementPallet,
            placementBin = placementBin,
            startedAt = startTime,
            completedAt = LocalDateTime.now()
        )
    }

    /**
     * Добавляет строку факта в задание
     */
    suspend fun addFactLineToTask(factLine: FactLineX): Result<TaskX> {
        return factLineWizardUseCases.addFactLine(factLine)
    }
}