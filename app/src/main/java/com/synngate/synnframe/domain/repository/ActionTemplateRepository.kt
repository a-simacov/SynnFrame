package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.taskx.action.ActionTemplate
import kotlinx.coroutines.flow.Flow

interface ActionTemplateRepository {
    // Получение списка шаблонов действий
    fun getActionTemplates(): Flow<List<ActionTemplate>>

    // Получение шаблона действия по ID
    suspend fun getActionTemplateById(id: String): ActionTemplate?

    // Добавление шаблона действия
    suspend fun addActionTemplate(template: ActionTemplate)

    // Обновление шаблона действия
    suspend fun updateActionTemplate(template: ActionTemplate)

    // Удаление шаблона действия
    suspend fun deleteActionTemplate(id: String)
}