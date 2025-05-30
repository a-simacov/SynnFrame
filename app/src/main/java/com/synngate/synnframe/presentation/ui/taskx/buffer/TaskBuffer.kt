package com.synngate.synnframe.presentation.ui.taskx.buffer

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.buffer.BufferDisplayItem
import java.time.LocalDateTime

/**
 * Буфер задания для хранения часто используемых объектов
 * Автоматически заполняет поля в визарде действий
 */
class TaskBuffer {
    // Хранение одного объекта для каждого типа поля с указанием источника
    private var storageBin: Pair<BinX, String>? = null
    private var storagePallet: Pair<Pallet, String>? = null
    private var storageProductClassifier: Pair<Product, String>? = null
    private var storageProduct: Pair<TaskProduct, String>? = null
    private var allocationBin: Pair<BinX, String>? = null
    private var allocationPallet: Pair<Pallet, String>? = null

    // Список всех добавленных объектов для отображения истории
    private val history = mutableListOf<BufferItem>()

    // Максимальный размер истории
    private val MAX_HISTORY_SIZE = 20

    /**
     * Добавление ячейки в буфер
     */
    fun addBin(bin: BinX, isStorage: Boolean, source: String) {
        val type = if (isStorage) BufferItemType.STORAGE_BIN else BufferItemType.ALLOCATION_BIN
        val item = BufferItem(
            id = generateId(),
            type = type,
            data = bin,
            source = source,
            savedAt = LocalDateTime.now()
        )

        if (isStorage) {
            storageBin = Pair(bin, source)
        } else {
            allocationBin = Pair(bin, source)
        }

        addToHistory(item)
    }

    /**
     * Добавление паллеты в буфер
     */
    fun addPallet(pallet: Pallet, isStorage: Boolean, source: String) {
        val type = if (isStorage) BufferItemType.STORAGE_PALLET else BufferItemType.ALLOCATION_PALLET
        val item = BufferItem(
            id = generateId(),
            type = type,
            data = pallet,
            source = source,
            savedAt = LocalDateTime.now()
        )

        if (isStorage) {
            storagePallet = Pair(pallet, source)
        } else {
            allocationPallet = Pair(pallet, source)
        }

        addToHistory(item)
    }

    /**
     * Добавление товара классификатора в буфер
     */
    fun addProduct(product: Product, source: String) {
        val item = BufferItem(
            id = generateId(),
            type = BufferItemType.PRODUCT,
            data = product,
            source = source,
            savedAt = LocalDateTime.now()
        )

        storageProductClassifier = Pair(product, source)
        addToHistory(item)
    }

    /**
     * Добавление товара задания в буфер
     */
    fun addTaskProduct(taskProduct: TaskProduct, source: String) {
        val item = BufferItem(
            id = generateId(),
            type = BufferItemType.TASK_PRODUCT,
            data = taskProduct,
            source = source,
            savedAt = LocalDateTime.now()
        )

        storageProduct = Pair(taskProduct, source)
        addToHistory(item)
    }

    /**
     * Получение объекта из буфера по типу поля
     */
    fun getObjectForField(field: FactActionField): Pair<Any, String>? {
        return when (field) {
            FactActionField.STORAGE_BIN -> storageBin
            FactActionField.STORAGE_PALLET -> storagePallet
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> storageProductClassifier
            FactActionField.STORAGE_PRODUCT -> storageProduct
            FactActionField.ALLOCATION_BIN -> allocationBin
            FactActionField.ALLOCATION_PALLET -> allocationPallet
            else -> null
        }
    }

    /**
     * Очистка поля в буфере
     */
    fun clearField(field: FactActionField) {
        when (field) {
            FactActionField.STORAGE_BIN -> storageBin = null
            FactActionField.STORAGE_PALLET -> storagePallet = null
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> storageProductClassifier = null
            FactActionField.STORAGE_PRODUCT -> storageProduct = null
            FactActionField.ALLOCATION_BIN -> allocationBin = null
            FactActionField.ALLOCATION_PALLET -> allocationPallet = null
            else -> {} // Другие поля не хранятся в буфере
        }
    }

    /**
     * Полная очистка буфера
     */
    fun clear() {
        storageBin = null
        storagePallet = null
        storageProductClassifier = null
        storageProduct = null
        allocationBin = null
        allocationPallet = null
        history.clear()
    }

    /**
     * Получение истории буфера
     */
    fun getHistory(): List<BufferItem> = history.toList()

    /**
     * Получение всех активных элементов буфера для отображения
     */
    fun getActiveBufferItems(): List<BufferDisplayItem> {
        val items = mutableListOf<BufferDisplayItem>()

        storageBin?.let { (bin, source) ->
            items.add(
                BufferDisplayItem(
                    field = FactActionField.STORAGE_BIN,
                    displayName = "Ячейка хранения",
                    value = bin.code,
                    data = bin,
                    source = source
                )
            )
        }

        storagePallet?.let { (pallet, source) ->
            items.add(
                BufferDisplayItem(
                    field = FactActionField.STORAGE_PALLET,
                    displayName = "Паллета хранения",
                    value = pallet.code,
                    data = pallet,
                    source = source
                )
            )
        }

        storageProductClassifier?.let { (product, source) ->
            items.add(
                BufferDisplayItem(
                    field = FactActionField.STORAGE_PRODUCT_CLASSIFIER,
                    displayName = "Товар",
                    value = product.name,
                    data = product,
                    source = source
                )
            )
        }

        storageProduct?.let { (taskProduct, source) ->
            items.add(
                BufferDisplayItem(
                    field = FactActionField.STORAGE_PRODUCT,
                    displayName = "Товар задания",
                    value = taskProduct.product.name,
                    data = taskProduct,
                    source = source
                )
            )
        }

        allocationBin?.let { (bin, source) ->
            items.add(
                BufferDisplayItem(
                    field = FactActionField.ALLOCATION_BIN,
                    displayName = "Ячейка размещения",
                    value = bin.code,
                    data = bin,
                    source = source
                )
            )
        }

        allocationPallet?.let { (pallet, source) ->
            items.add(
                BufferDisplayItem(
                    field = FactActionField.ALLOCATION_PALLET,
                    displayName = "Паллета размещения",
                    value = pallet.code,
                    data = pallet,
                    source = source
                )
            )
        }

        return items
    }

    /**
     * Добавление элемента в историю с ограничением размера
     */
    private fun addToHistory(item: BufferItem) {
        // Добавляем в начало списка
        history.add(0, item)

        // Ограничиваем размер истории
        if (history.size > MAX_HISTORY_SIZE) {
            history.subList(MAX_HISTORY_SIZE, history.size).clear()
        }
    }

    /**
     * Генерация уникального ID для элемента буфера
     */
    private fun generateId(): String = "buffer_${System.currentTimeMillis()}"
}

/**
 * Элемент буфера с метаданными
 */
data class BufferItem(
    val id: String,
    val type: BufferItemType,
    val data: Any,
    val source: String,
    val savedAt: LocalDateTime
)

/**
 * Типы объектов в буфере
 */
enum class BufferItemType {
    PRODUCT,
    TASK_PRODUCT,
    STORAGE_PALLET,
    ALLOCATION_PALLET,
    STORAGE_BIN,
    ALLOCATION_BIN
}