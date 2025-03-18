package com.synngate.synnframe.domain.entity

import java.time.LocalDateTime

/**
 * Доменная модель задания
 */
data class Task(
    /**
     * Идентификатор задания
     */
    val id: String,

    /**
     * Наименование задания
     */
    val name: String,

    /**
     * Тип задания (приемка, отбор)
     */
    val type: TaskType,

    /**
     * Штрихкод задания
     */
    val barcode: String,

    /**
     * Дата создания
     */
    val createdAt: LocalDateTime,

    /**
     * Дата просмотра
     */
    val viewedAt: LocalDateTime? = null,

    /**
     * Дата начала работы
     */
    val startedAt: LocalDateTime? = null,

    /**
     * Дата завершения
     */
    val completedAt: LocalDateTime? = null,

    /**
     * Место создания (приложение, сервер)
     */
    val creationPlace: CreationPlace,

    /**
     * Исполнитель задания
     */
    val executorId: String? = null,

    /**
     * Статус выполнения
     */
    val status: TaskStatus = TaskStatus.TO_DO,

    /**
     * Признак выгрузки на сервер
     */
    val uploaded: Boolean = false,

    /**
     * Дата выгрузки на сервер
     */
    val uploadedAt: LocalDateTime? = null,

    /**
     * Строки плана выполнения
     */
    val planLines: List<TaskPlanLine> = emptyList(),

    /**
     * Строки факта выполнения
     */
    val factLines: List<TaskFactLine> = emptyList()
) {
    /**
     * Проверка, можно ли начать выполнение задания
     */
    fun canStart(): Boolean = status == TaskStatus.TO_DO

    /**
     * Проверка, можно ли завершить задание
     */
    fun canComplete(): Boolean = status == TaskStatus.IN_PROGRESS

    /**
     * Проверка, можно ли выгрузить задание
     */
    fun canUpload(): Boolean = status == TaskStatus.COMPLETED && !uploaded

    /**
     * Получение общего количества товаров по плану
     */
    fun getTotalPlanQuantity(): Float = planLines.sumOf { it.quantity.toDouble() }.toFloat()

    /**
     * Получение общего количества товаров по факту
     */
    fun getTotalFactQuantity(): Float = factLines.sumOf { it.quantity.toDouble() }.toFloat()

    /**
     * Получение процента выполнения задания
     */
    fun getCompletionPercentage(): Float {
        val totalPlan = getTotalPlanQuantity()
        return if (totalPlan > 0) {
            (getTotalFactQuantity() / totalPlan) * 100f
        } else {
            0f
        }
    }

    /**
     * Получение строки факта по идентификатору товара
     */
    fun getFactLineByProductId(productId: String): TaskFactLine? =
        factLines.find { it.productId == productId }

    /**
     * Проверка, есть ли товар в плане
     */
    fun isProductInPlan(productId: String): Boolean =
        planLines.any { it.productId == productId }
}

/**
 * Строка плана задания
 */
data class TaskPlanLine(
    /**
     * Идентификатор строки
     */
    val id: String,

    /**
     * Идентификатор задания
     */
    val taskId: String,

    /**
     * Идентификатор товара
     */
    val productId: String,

    /**
     * Количество по плану
     */
    val quantity: Float
)

/**
 * Строка факта задания
 */
data class TaskFactLine(
    /**
     * Идентификатор строки
     */
    val id: String,

    /**
     * Идентификатор задания
     */
    val taskId: String,

    /**
     * Идентификатор товара
     */
    val productId: String,

    /**
     * Количество по факту
     */
    val quantity: Float
)