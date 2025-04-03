package com.synngate.synnframe.presentation.service.webserver.controller

import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
import com.synngate.synnframe.presentation.service.webserver.WebServerSyncIntegrator

class WebServerControllerFactory(
    private val loggingService: LoggingService,
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository,
    private val productRepository: ProductRepository,
    private val taskTypeUseCases: TaskTypeUseCases,
    private val syncIntegrator: WebServerSyncIntegrator,
    private val saveSyncHistoryRecord: suspend (Int, Int, Int, Long) -> Unit
) {

    fun createEchoController(): EchoController {
        return EchoController(loggingService, userRepository)
    }

    fun createProductsController(): ProductsController {
        return ProductsController(loggingService, productRepository, syncIntegrator, saveSyncHistoryRecord)
    }

    fun createTasksController(): TasksController {
        return TasksController(loggingService, taskRepository, syncIntegrator, saveSyncHistoryRecord)
    }

    fun createTaskTypesController(): TaskTypesController {
        return TaskTypesController(loggingService, taskTypeUseCases, syncIntegrator, saveSyncHistoryRecord)
    }
}