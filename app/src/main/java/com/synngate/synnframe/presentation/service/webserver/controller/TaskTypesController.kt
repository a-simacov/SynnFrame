package com.synngate.synnframe.presentation.service.webserver.controller

import com.synngate.synnframe.data.remote.dto.TaskTypeDto
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
import com.synngate.synnframe.presentation.service.webserver.WebServerConstants
import com.synngate.synnframe.presentation.service.webserver.WebServerSyncIntegrator
import com.synngate.synnframe.presentation.service.webserver.util.respondError
import com.synngate.synnframe.presentation.service.webserver.util.respondSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.serialization.Serializable
import timber.log.Timber

class TaskTypesController(
    override val logger: LoggingService,
    private val taskTypeUseCases: TaskTypeUseCases,
    private val syncIntegrator: WebServerSyncIntegrator,
    private val saveSyncHistoryRecord: suspend (Int, Int, Int, Long) -> Unit
) : WebServerController {

    @Serializable
    data class TaskTypesResponse(
        val count: Int,
        val processingTime: Long,
        val success: Boolean = true
    )

    suspend fun handleTaskTypes(call: ApplicationCall) {
        val startTime = System.currentTimeMillis()

        try {
            // Получаем данные запроса
            val taskTypesData = call.receive<List<TaskTypeDto>>()

            if (taskTypesData.isEmpty()) {
                call.respondError(
                    "No task types received",
                    code = 400,
                    statusCode = HttpStatusCode.BadRequest
                )
                return
            }

            // Используем встроенный метод для синхронизации типов заданий
            val result = taskTypeUseCases.syncTaskTypes()

            // Вычисляем время выполнения
            val duration = System.currentTimeMillis() - startTime

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0

                // Логируем операцию
                logger.logInfo(
                    String.format(WebServerConstants.LOG_TASK_TYPES_RECEIVED, count, duration)
                )

                // Сохраняем запись в историю синхронизаций
                try {
                    val syncId = "webserver-${System.currentTimeMillis()}"
                    saveSyncHistoryRecord(
                        0,
                        0,
                        count,
                        duration
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save sync history record")
                }

                // Обновляем UI через интегратор
                syncIntegrator.updateSyncProgress(
                    taskTypesDownloaded = count,
                    operation = WebServerConstants.OPERATION_TASK_TYPES_RECEIVED
                )

                // Отправляем ответ
                call.respondSuccess(
                    TaskTypesResponse(
                        count = count,
                        processingTime = duration
                    )
                )
            } else {
                val error = result.exceptionOrNull()
                logger.logError(String.format(WebServerConstants.LOG_ERROR_TASK_TYPES, error?.message))

                call.respondError(
                    "Failed to sync task types: ${error?.message}",
                    code = 500,
                    statusCode = HttpStatusCode.InternalServerError
                )
            }

        } catch (e: Exception) {
            handleError(call, e, "task types endpoint")
            call.respondError("Failed to process task types: ${e.message}")
        }
    }
}