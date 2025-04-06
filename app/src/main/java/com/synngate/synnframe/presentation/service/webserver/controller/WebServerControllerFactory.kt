package com.synngate.synnframe.presentation.service.webserver.controller

import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
import com.synngate.synnframe.presentation.service.webserver.WebServerSyncIntegrator

class WebServerControllerFactory(
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository,
    private val productRepository: ProductRepository,
    private val taskTypeUseCases: TaskTypeUseCases,
    private val syncIntegrator: WebServerSyncIntegrator,
    private val saveSyncHistoryRecord: suspend (Int, Int, Int, Long) -> Unit
) {

    fun createEchoController(): EchoController {
        return EchoController(userRepository)
    }

    fun createProductsController(): ProductsController {
        return ProductsController(productRepository, syncIntegrator, saveSyncHistoryRecord)
    }

    fun createTasksController(): TasksController {
        return TasksController(taskRepository, syncIntegrator, saveSyncHistoryRecord)
    }

    fun createTaskTypesController(): TaskTypesController {
        return TaskTypesController(taskTypeUseCases, syncIntegrator, saveSyncHistoryRecord)
    }
}