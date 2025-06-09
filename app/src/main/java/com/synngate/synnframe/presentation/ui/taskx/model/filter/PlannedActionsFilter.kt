package com.synngate.synnframe.presentation.ui.taskx.model.filter

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.ui.taskx.entity.SearchActionFieldType
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.PlannedActionUI
import timber.log.Timber

class PlannedActionsFilter {
    private var storageBin: BinX? = null
    private var storagePallet: Pallet? = null
    private var storageProductClassifier: Product? = null
    private var storageProduct: TaskProduct? = null
    private var allocationBin: BinX? = null
    private var allocationPallet: Pallet? = null

    // Отслеживание порядка добавления фильтров для очистки последнего
    private val filterTimestamps = mutableMapOf<FactActionField, Long>()

    fun addFilter(field: FactActionField, value: Any) {
        when (field) {
            FactActionField.STORAGE_BIN -> {
                if (value is BinX) storageBin = value
            }
            FactActionField.STORAGE_PALLET -> {
                if (value is Pallet) storagePallet = value
            }
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> {
                if (value is Product) storageProductClassifier = value
            }
            FactActionField.STORAGE_PRODUCT -> {
                if (value is TaskProduct) storageProduct = value
            }
            FactActionField.ALLOCATION_BIN -> {
                if (value is BinX) allocationBin = value
            }
            FactActionField.ALLOCATION_PALLET -> {
                if (value is Pallet) allocationPallet = value
            }
            else -> {
                Timber.w("Не поддерживаемый тип поля для фильтра: $field")
                return
            }
        }

        // Обновляем или добавляем временную метку для этого фильтра
        filterTimestamps[field] = System.currentTimeMillis()

        Timber.d("Добавлен фильтр по полю $field")
    }

    fun removeFilter(field: FactActionField) {
        when (field) {
            FactActionField.STORAGE_BIN -> storageBin = null
            FactActionField.STORAGE_PALLET -> storagePallet = null
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> storageProductClassifier = null
            FactActionField.STORAGE_PRODUCT -> storageProduct = null
            FactActionField.ALLOCATION_BIN -> allocationBin = null
            FactActionField.ALLOCATION_PALLET -> allocationPallet = null
            else -> {
                Timber.w("Не поддерживаемый тип поля для удаления фильтра: $field")
                return
            }
        }

        // Удаляем временную метку для этого фильтра
        filterTimestamps.remove(field)

        Timber.d("Удален фильтр по полю $field")
    }

    fun clearAllFilters() {
        storageBin = null
        storagePallet = null
        storageProductClassifier = null
        storageProduct = null
        allocationBin = null
        allocationPallet = null

        filterTimestamps.clear()

        Timber.d("Очищены все фильтры")
    }

    fun getLastAddedFilterField(): FactActionField? {
        if (filterTimestamps.isEmpty()) return null

        return filterTimestamps.maxByOrNull { it.value }?.key
    }

    fun hasActiveFilters(): Boolean {
        return storageBin != null ||
                storagePallet != null ||
                storageProductClassifier != null ||
                storageProduct != null ||
                allocationBin != null ||
                allocationPallet != null
    }

    fun filterActions(actions: List<PlannedActionUI>): List<PlannedActionUI> {
        if (!hasActiveFilters()) return actions

        return actions.filter { actionUI ->
            val action = actionUI.action

            // Проверяем соответствие каждому установленному фильтру
            val matchesStorageBin = storageBin?.let { bin ->
                action.storageBin?.code == bin.code
            } ?: true

            val matchesStoragePallet = storagePallet?.let { pallet ->
                action.storagePallet?.code == pallet.code
            } ?: true

            val matchesStorageProductClassifier = storageProductClassifier?.let { product ->
                action.storageProductClassifier?.id == product.id
            } ?: true

            val matchesStorageProduct = storageProduct?.let { taskProduct ->
                action.storageProduct?.product?.id == taskProduct.product.id
            } ?: true

            val matchesAllocationBin = allocationBin?.let { bin ->
                action.placementBin?.code == bin.code
            } ?: true

            val matchesAllocationPallet = allocationPallet?.let { pallet ->
                action.placementPallet?.code == pallet.code
            } ?: true

            matchesStorageBin &&
                    matchesStoragePallet &&
                    matchesStorageProductClassifier &&
                    matchesStorageProduct &&
                    matchesAllocationBin &&
                    matchesAllocationPallet
        }
    }

    fun getActiveFilters(searchActionFieldsTypes: List<SearchActionFieldType>? = null): List<FilterItem> {
        val filters = mutableListOf<FilterItem>()

        storageBin?.let { bin ->
            filters.add(
                FilterItem(
                    field = FactActionField.STORAGE_BIN,
                    displayName = "Storage Bin",
                    value = bin.code,
                    data = bin,
                    timestamp = filterTimestamps[FactActionField.STORAGE_BIN] ?: 0
                )
            )
        }

        storagePallet?.let { pallet ->
            filters.add(
                FilterItem(
                    field = FactActionField.STORAGE_PALLET,
                    displayName = "Storage Pallet",
                    value = pallet.code,
                    data = pallet,
                    timestamp = filterTimestamps[FactActionField.STORAGE_PALLET] ?: 0
                )
            )
        }

        storageProductClassifier?.let { product ->
            filters.add(
                FilterItem(
                    field = FactActionField.STORAGE_PRODUCT_CLASSIFIER,
                    displayName = "Product",
                    value = product.name,
                    data = product,
                    timestamp = filterTimestamps[FactActionField.STORAGE_PRODUCT_CLASSIFIER] ?: 0
                )
            )
        }

        storageProduct?.let { taskProduct ->
            filters.add(
                FilterItem(
                    field = FactActionField.STORAGE_PRODUCT,
                    displayName = "Task Product",
                    value = taskProduct.product.name,
                    data = taskProduct,
                    timestamp = filterTimestamps[FactActionField.STORAGE_PRODUCT] ?: 0
                )
            )
        }

        allocationBin?.let { bin ->
            filters.add(
                FilterItem(
                    field = FactActionField.ALLOCATION_BIN,
                    displayName = "Placement Bin",
                    value = bin.code,
                    data = bin,
                    timestamp = filterTimestamps[FactActionField.ALLOCATION_BIN] ?: 0
                )
            )
        }

        allocationPallet?.let { pallet ->
            filters.add(
                FilterItem(
                    field = FactActionField.ALLOCATION_PALLET,
                    displayName = "Placement Pallet",
                    value = pallet.code,
                    data = pallet,
                    timestamp = filterTimestamps[FactActionField.ALLOCATION_PALLET] ?: 0
                )
            )
        }

        return if (searchActionFieldsTypes != null) {
            // Создаем карту приоритетов полей на основе порядка в настройках
            val fieldPriorities = searchActionFieldsTypes.mapIndexed { index, type ->
                type.actionField to index
            }.toMap()

            // Сортируем фильтры согласно приоритетам полей
            filters.sortedBy { fieldPriorities[it.field] ?: Int.MAX_VALUE }
        } else {
            // Если настройки не предоставлены, сортируем по времени добавления
            filters.sortedByDescending { it.timestamp }
        }
    }
}