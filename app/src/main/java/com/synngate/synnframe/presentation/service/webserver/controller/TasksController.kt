package com.synngate.synnframe.presentation.service.webserver.controller

import com.synngate.synnframe.data.remote.dto.TaskDto
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.presentation.service.webserver.WebServerConstants
import com.synngate.synnframe.presentation.service.webserver.WebServerSyncIntegrator
import com.synngate.synnframe.presentation.service.webserver.util.respondError
import com.synngate.synnframe.presentation.service.webserver.util.respondSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.serialization.Serializable
import timber.log.Timber

class TasksController(
    private val taskRepository: TaskRepository,
    private val syncIntegrator: WebServerSyncIntegrator,
    private val saveSyncHistoryRecord: suspend (Int, Int, Int, Long) -> Unit
) : WebServerController {

    @Serializable
    data class TasksResponse(
        val total: Int,
        val new: Int,
        val updated: Int,
        val processingTime: Long
    )

    suspend fun handleTasks(call: ApplicationCall) {
        val startTime = System.currentTimeMillis()

        try {
            // Получаем данные запроса
            val tasksData = call.receive<List<TaskDto>>()

            if (tasksData.isEmpty()) {
                call.respondError("No tasks received", code = 400, statusCode = HttpStatusCode.BadRequest)
                return
            }

            // Конвертируем DTO в доменные модели
            val tasks = tasksData.map { it.toDomainModel() }

            // Для каждого задания проверяем, есть ли оно уже в базе
            val newTasks = mutableListOf<Task>()
            val updatedTasks = mutableListOf<Task>()

            for (task in tasks) {
                val existingTask = taskRepository.getTaskById(task.id)
                if (existingTask == null) {
                    newTasks.add(task)
                } else if (existingTask.status == TaskStatus.TO_DO) {
                    // Обновляем только задания в статусе "К выполнению"
                    updatedTasks.add(task)
                }
            }

            // Добавляем новые задания
            if (newTasks.isNotEmpty()) {
                taskRepository.addTasks(newTasks)
            }

            // Обновляем существующие задания
            for (task in updatedTasks) {
                taskRepository.updateTask(task)
            }

            // Логируем операцию
            val duration = System.currentTimeMillis() - startTime
            Timber.i(
                String.format(
                    WebServerConstants.LOG_TASKS_RECEIVED,
                    tasks.size,
                    newTasks.size,
                    updatedTasks.size,
                    duration
                )
            )

            // Сохраняем запись в историю синхронизаций
            try {
                val syncId = "webserver-${System.currentTimeMillis()}"
                saveSyncHistoryRecord(
                    tasks.size,
                    0,
                    0,
                    duration
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to save sync history record")
            }

            // Обновляем UI через интегратор
            syncIntegrator.updateSyncProgress(
                tasksDownloaded = tasks.size,
                operation = WebServerConstants.OPERATION_TASKS_RECEIVED
            )

            // Отправляем ответ
            call.respondSuccess(
                TasksResponse(
                    total = tasks.size,
                    new = newTasks.size,
                    updated = updatedTasks.size,
                    processingTime = duration
                )
            )

        } catch (e: Exception) {
            handleError(call, e, "tasks endpoint")
            call.respondError("Failed to process tasks: ${e.message}")
        }
    }
}