package com.synngate.synnframe.presentation.di

import com.synngate.synnframe.presentation.ui.login.LoginViewModel
import com.synngate.synnframe.presentation.ui.logs.LogDetailViewModel
import com.synngate.synnframe.presentation.ui.logs.LogListViewModel
import com.synngate.synnframe.presentation.ui.main.MainMenuViewModel
import com.synngate.synnframe.presentation.ui.product.ProductListViewModel
import com.synngate.synnframe.presentation.ui.products.ProductDetailViewModel
import com.synngate.synnframe.presentation.ui.server.ServerDetailViewModel
import com.synngate.synnframe.presentation.ui.server.ServerListViewModel
import com.synngate.synnframe.presentation.ui.settings.SettingsViewModel
import com.synngate.synnframe.presentation.ui.tasks.TaskDetailViewModel
import com.synngate.synnframe.presentation.ui.tasks.TaskListViewModel

interface Clearable {

    fun clear()
}

interface ClearableContainer : Clearable {

    val clearables: MutableList<Clearable>

    fun addClearable(clearable: Clearable) {
        clearables.add(clearable)
    }

    override fun clear() {
        clearables.forEach { it.clear() }
        clearables.clear()
    }
}

interface NavHostContainer : ClearableContainer {

    fun createServerListGraphContainer(): ServerListGraphContainer

    fun createTasksGraphContainer(): TasksGraphContainer

    fun createProductsGraphContainer(): ProductsGraphContainer

    fun createLogsGraphContainer(): LogsGraphContainer

    fun createSettingsScreenContainer(): SettingsScreenContainer

    fun createLoginScreenContainer(): LoginScreenContainer

    fun createMainMenuScreenContainer(): MainMenuScreenContainer
}

interface GraphContainer : ClearableContainer

interface ServerListGraphContainer : GraphContainer {

    fun createServerListViewModel(): ServerListViewModel

    fun createServerDetailViewModel(serverId: Int?): ServerDetailViewModel
}

interface TasksGraphContainer : GraphContainer {

    fun createTaskListViewModel(): TaskListViewModel

    fun createTaskDetailViewModel(taskId: String): TaskDetailViewModel
}

interface ProductsGraphContainer : GraphContainer {

    fun createProductListViewModel(): ProductListViewModel

    fun createProductDetailViewModel(productId: String): ProductDetailViewModel
}

interface LogsGraphContainer : GraphContainer {

    fun createLogListViewModel(): LogListViewModel

    fun createLogDetailViewModel(logId: Int): LogDetailViewModel
}

interface SettingsScreenContainer : GraphContainer {

    fun createSettingsViewModel(): SettingsViewModel
}

interface LoginScreenContainer : GraphContainer {

    fun createLoginViewModel(): LoginViewModel
}

interface MainMenuScreenContainer : GraphContainer {

    fun createMainMenuViewModel(): MainMenuViewModel
}