package com.synngate.synnframe.presentation.service.webserver.controller

import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.presentation.service.webserver.WebServerSyncIntegrator

class WebServerControllerFactory(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val syncIntegrator: WebServerSyncIntegrator,
    private val saveSyncHistoryRecord: suspend (Int, Long) -> Unit
) {

    fun createEchoController(): EchoController {
        return EchoController(userRepository)
    }

    fun createProductsController(): ProductsController {
        return ProductsController(productRepository, syncIntegrator, saveSyncHistoryRecord)
    }
}