package com.synngate.synnframe.presentation.di.modules.feature

import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.CoreContainer
import com.synngate.synnframe.presentation.di.modules.DomainContainer
import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.ui.taskx.TaskXDetailViewModel
import com.synngate.synnframe.presentation.ui.taskx.TaskXListViewModel
import com.synngate.synnframe.presentation.ui.wizard.ActionWizardViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactoryRegistry
import com.synngate.synnframe.presentation.ui.wizard.action.bin.BinSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.pallet.PalletSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.product.ProductSelectionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.quantity.ProductQuantityStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.taskproduct.TaskProductSelectionStepFactory
import timber.log.Timber

/**
 * Контейнер для функциональности работы с задачами.
 * Содержит зависимости и фабрики для ViewModel-ей, связанных с задачами.
 */
class TasksFeatureContainer(
    appContainer: AppContainer,
    coreContainer: CoreContainer,
    domainContainer: DomainContainer
) : FeatureContainer(appContainer, coreContainer, domainContainer) {

    override val moduleName: String = "Tasks"

    /**
     * Создание ViewModel для экрана списка задач
     *
     * @return ViewModel для списка задач
     */
    fun createTaskXListViewModel(): TaskXListViewModel {
        return getViewModel("TaskXListViewModel") {
            TaskXListViewModel(
                taskXUseCases = domainContainer.taskXUseCases,
                userUseCases = domainContainer.userUseCases
            )
        }
    }

    /**
     * Создание ViewModel для экрана деталей задачи
     *
     * @param taskId Идентификатор задачи
     * @return ViewModel для деталей задачи
     */
    fun createTaskXDetailViewModel(taskId: String): TaskXDetailViewModel {
        return getViewModel("TaskXDetailViewModel_$taskId") {
            // Проверяем, есть ли данные в TaskContextManager
            val contextTask = domainContainer.taskContextManager.lastStartedTaskX.value
            val contextTaskType = domainContainer.taskContextManager.lastTaskTypeX.value

            // Если задача в контексте и совпадает по ID, используем её вместе с типом
            if (contextTask != null && contextTask.id == taskId && contextTaskType != null) {
                TaskXDetailViewModel(
                    taskId = taskId,
                    taskXUseCases = domainContainer.taskXUseCases,
                    userUseCases = domainContainer.userUseCases,
                    finalActionsValidator = domainContainer.finalActionsValidator,
                    actionExecutionService = domainContainer.actionExecutionService,
                    actionSearchService = domainContainer.actionSearchService,
                    preloadedTask = contextTask,
                    preloadedTaskType = contextTaskType
                )
            } else {
                // Иначе создаём обычный ViewModel, который загрузит данные
                TaskXDetailViewModel(
                    taskId = taskId,
                    taskXUseCases = domainContainer.taskXUseCases,
                    userUseCases = domainContainer.userUseCases,
                    finalActionsValidator = domainContainer.finalActionsValidator,
                    actionExecutionService = domainContainer.actionExecutionService,
                    actionSearchService = domainContainer.actionSearchService
                )
            }
        }
    }

    /**
     * Создание ViewModel для экрана мастера действий
     *
     * @param taskId Идентификатор задачи
     * @param actionId Идентификатор действия
     * @return ViewModel для мастера действий
     */
    fun createActionWizardViewModel(taskId: String, actionId: String): ActionWizardViewModel {
        return getViewModel("ActionWizardViewModel_${taskId}_${actionId}") {
            ActionWizardViewModel(
                taskId = taskId,
                actionId = actionId,
                wizardStateMachine = domainContainer.wizardStateMachine,
                actionStepFactoryRegistry = createActionStepFactoryRegistry()
            )
        }
    }

    /**
     * Создание реестра фабрик для шагов мастера действий
     */
    private fun createActionStepFactoryRegistry(): ActionStepFactoryRegistry {
        // Создаем реестр
        val registry = ActionStepFactoryRegistry()

        // Регистрируем фабрики для различных типов объектов
        registry.registerFactory(
            ActionObjectType.CLASSIFIER_PRODUCT,
            ProductSelectionStepFactory(
                productLookupService = domainContainer.productLookupService,
                validationService = domainContainer.validationService
            )
        )

        registry.registerFactory(
            ActionObjectType.TASK_PRODUCT,
            TaskProductSelectionStepFactory(
                productLookupService = domainContainer.productLookupService,
                validationService = domainContainer.validationService
            )
        )

        registry.registerFactory(
            ActionObjectType.PRODUCT_QUANTITY,
            ProductQuantityStepFactory(
                validationService = domainContainer.validationService
            )
        )

        registry.registerFactory(
            ActionObjectType.PALLET,
            PalletSelectionStepFactory(
                palletLookupService = domainContainer.palletLookupService,
                validationService = domainContainer.validationService
            )
        )

        registry.registerFactory(
            ActionObjectType.BIN,
            BinSelectionStepFactory(
                binLookupService = domainContainer.binLookupService,
                validationService = domainContainer.validationService
            )
        )

        return registry
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Tasks feature initialized")
    }
}