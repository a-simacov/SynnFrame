package com.synngate.synnframe.domain.entity.operation

import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import kotlinx.serialization.Serializable

@Serializable
data class DynamicTask(
    val id: String,
    val name: String,
    val status: String = "",
    val executorId: String? = null,
    val searchCode: String? = null,
    val canDelete: Boolean = false
) {
    fun getTaskStatus(): TaskXStatus {
        return when (status.uppercase()) {
            "TO_DO" -> TaskXStatus.TO_DO
            "IN_PROGRESS" -> TaskXStatus.IN_PROGRESS
            "PAUSED" -> TaskXStatus.PAUSED
            "COMPLETED" -> TaskXStatus.COMPLETED
            "CANCELLED" -> TaskXStatus.CANCELLED
            else -> TaskXStatus.TO_DO
        }
    }

    fun matchesSearchQuery(query: String): Boolean {
        if (query.isBlank()) return true

        val normalizedQuery = query.trim().lowercase()

        return when {
            // Проверка по searchCode (основной способ локального поиска)
            searchCode?.lowercase()?.contains(normalizedQuery) == true -> true

            // Проверка по id задания
            id.lowercase().contains(normalizedQuery) -> true

            // Проверка по имени задания (без HTML тегов)
            name.replace(Regex("<[^>]*>"), "").lowercase().contains(normalizedQuery) -> true

            else -> false
        }
    }

    fun isDeletable(): Boolean {
        return canDelete && getTaskStatus() in arrayOf(
            TaskXStatus.TO_DO,
            TaskXStatus.PAUSED,
            TaskXStatus.IN_PROGRESS
        )
    }
}