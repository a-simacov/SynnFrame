package com.synngate.synnframe.domain.entity.taskx

/**
 * Получение промежуточных результатов в виде карты
 */
fun FactLineWizardState.getIntermediateResults(): Map<String, Any?> {
    val results = mutableMapOf<String, Any?>()

    // Добавляем все заполненные поля
    storageProduct?.let { results["STORAGE_PRODUCT"] = it }
    storagePallet?.let { results["STORAGE_PALLET"] = it }
    placementPallet?.let { results["PLACEMENT_PALLET"] = it }
    placementBin?.let { results["PLACEMENT_BIN"] = it }
    wmsAction?.let { results["WMS_ACTION"] = it }

    return results
}

/**
 * Проверка, можно ли вернуться на предыдущий шаг
 */
fun FactLineWizardState.canGoBack(): Boolean {
    return currentGroupIndex > 0 || currentActionIndex > 0
}

/**
 * Проверка, является ли текущий шаг последним
 */
fun FactLineWizardState.isLastStep(): Boolean {
    return isCurrentGroupCompleted() && currentGroupIndex >= groups.size - 1
}

/**
 * Получение значения прогресса (от 0.0f до 1.0f)
 */
fun FactLineWizardState.getProgressValue(): Float {
    if (groups.isEmpty()) return 0f

    var completedSteps = 0
    var totalSteps = 0

    groups.forEachIndexed { groupIndex, group ->
        val actionsCount = group.actions.size
        totalSteps += actionsCount

        if (groupIndex < currentGroupIndex) {
            // Предыдущие группы полностью завершены
            completedSteps += actionsCount
        } else if (groupIndex == currentGroupIndex) {
            // Текущая группа
            completedSteps += currentActionIndex
        }
    }

    return if (totalSteps > 0) {
        completedSteps.toFloat() / totalSteps
    } else {
        0f
    }
}

/**
 * Получение текущего действия
 */
fun FactLineWizardState.getCurrentAction(): FactLineXAction? {
    if (currentGroupIndex >= groups.size) return null
    val currentGroup = groups[currentGroupIndex]
    if (currentActionIndex >= currentGroup.actions.size) return null
    return currentGroup.actions[currentActionIndex]
}

/**
 * Проверка завершения текущей группы
 */
fun FactLineWizardState.isCurrentGroupCompleted(): Boolean {
    if (currentGroupIndex >= groups.size) return true
    val currentGroup = groups[currentGroupIndex]
    return currentActionIndex >= currentGroup.actions.size
}

/**
 * Проверка завершения всего мастера
 */
fun FactLineWizardState.isCompleted(): Boolean {
    return currentGroupIndex >= groups.size
}