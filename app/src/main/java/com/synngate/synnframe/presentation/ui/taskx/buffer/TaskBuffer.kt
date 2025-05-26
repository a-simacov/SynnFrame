package com.synngate.synnframe.presentation.ui.taskx.buffer

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import java.time.LocalDateTime

/**
 * Буфер задания для хранения часто используемых объектов
 * Автоматически заполняет поля в визарде действий
 */
class TaskBuffer {

    private val items = mutableListOf<BufferItem>()

    fun addProduct(product: Product, source: String) {
        val item = BufferItem(
            id = generateId(),
            type = BufferItemType.PRODUCT,
            data = product,
            source = source,
            savedAt = LocalDateTime.now()
        )
        items.add(0, item) // Добавляем в начало для быстрого доступа
        limitBufferSize()
    }

    fun addTaskProduct(taskProduct: TaskProduct, source: String) {
        val item = BufferItem(
            id = generateId(),
            type = BufferItemType.TASK_PRODUCT,
            data = taskProduct,
            source = source,
            savedAt = LocalDateTime.now()
        )
        items.add(0, item)
        limitBufferSize()
    }

    fun addPallet(pallet: Pallet, source: String) {
        val item = BufferItem(
            id = generateId(),
            type = BufferItemType.PALLET,
            data = pallet,
            source = source,
            savedAt = LocalDateTime.now()
        )
        items.add(0, item)
        limitBufferSize()
    }

    fun addBin(bin: BinX, source: String) {
        val item = BufferItem(
            id = generateId(),
            type = BufferItemType.BIN,
            data = bin,
            source = source,
            savedAt = LocalDateTime.now()
        )
        items.add(0, item)
        limitBufferSize()
    }

    fun getLastPallet(): Pair<Pallet, String>? {
        val item = items.firstOrNull { it.type == BufferItemType.PALLET }
        return if (item != null) Pair(item.data as Pallet, item.source) else null
    }

    fun getLastBin(): Pair<BinX, String>? {
        val item = items.firstOrNull { it.type == BufferItemType.BIN }
        return if (item != null) Pair(item.data as BinX, item.source) else null
    }

    fun getLastProduct(): Pair<Product, String>? {
        val item = items.firstOrNull { it.type == BufferItemType.PRODUCT }
        return if (item != null) Pair(item.data as Product, item.source) else null
    }

    fun getLastTaskProduct(): Pair<TaskProduct, String>? {
        val item = items.firstOrNull { it.type == BufferItemType.TASK_PRODUCT }
        return if (item != null) Pair(item.data as TaskProduct, item.source) else null
    }

    fun getItemForField(field: FactActionField): Pair<Any, String>? {
        return when (field) {
            FactActionField.STORAGE_PALLET, FactActionField.ALLOCATION_PALLET -> getLastPallet()
            FactActionField.STORAGE_BIN, FactActionField.ALLOCATION_BIN -> getLastBin()
            FactActionField.STORAGE_PRODUCT_CLASSIFIER -> getLastProduct()
            FactActionField.STORAGE_PRODUCT -> getLastTaskProduct()
            else -> null
        }
    }

    fun getAllItems(): List<BufferItem> = items.toList()

    fun clear() {
        items.clear()
    }

    fun removeItem(id: String) {
        items.removeAll { it.id == id }
    }

    private fun limitBufferSize() {
        // Ограничиваем буфер 20 элементами
        if (items.size > 20) {
            items.subList(20, items.size).clear()
        }
    }

    private fun generateId(): String = "buffer_${System.currentTimeMillis()}"
}

data class BufferItem(
    val id: String,
    val type: BufferItemType,
    val data: Any,
    val source: String,
    val savedAt: LocalDateTime
)

enum class BufferItemType {
    PRODUCT,
    TASK_PRODUCT,
    PALLET,
    BIN
}