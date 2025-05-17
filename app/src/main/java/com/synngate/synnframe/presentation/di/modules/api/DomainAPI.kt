package com.synngate.synnframe.presentation.di.modules.api

import com.synngate.synnframe.domain.service.ActionExecutionService
import com.synngate.synnframe.domain.service.ActionSearchService
import com.synngate.synnframe.domain.service.FinalActionsValidator
import com.synngate.synnframe.domain.service.ServerCoordinator
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.domain.service.UpdateInstaller
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.domain.service.WebServerManager
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases

/**
 * Интерфейс для предоставления компонентов бизнес-логики.
 * Включает use cases, бизнес-сервисы, валидаторы и т.д.
 */
interface DomainAPI {
    /**
     * Менеджер контекста задач
     */
    val taskContextManager: TaskContextManager

    /**
     * Use cases для работы с серверами
     */
    val serverUseCases: ServerUseCases

    /**
     * Use cases для работы с пользователями
     */
    val userUseCases: UserUseCases

    /**
     * Use cases для работы с продуктами
     */
    val productUseCases: ProductUseCases

    /**
     * Use cases для работы с логами
     */
    val logUseCases: LogUseCases

    /**
     * Use cases для работы с настройками
     */
    val settingsUseCases: SettingsUseCases

    /**
     * Use cases для работы с динамическим меню
     */
    val dynamicMenuUseCases: DynamicMenuUseCases

    /**
     * Use cases для работы с заданиями X
     */
    val taskXUseCases: TaskXUseCases

    /**
     * Сервис для выполнения действий
     */
    val actionExecutionService: ActionExecutionService

    /**
     * Сервис для валидации данных
     */
    val validationService: ValidationService

    /**
     * Валидатор финальных действий
     */
    val finalActionsValidator: FinalActionsValidator

    /**
     * Сервис для поиска действий
     */
    val actionSearchService: ActionSearchService

    /**
     * Менеджер веб-сервера
     */
    val webServerManager: WebServerManager

    /**
     * Установщик обновлений
     */
    val updateInstaller: UpdateInstaller

    /**
     * Контроллер синхронизации
     */
    val synchronizationController: SynchronizationController

    /**
     * Координатор серверов
     */
    val serverCoordinator: ServerCoordinator
}