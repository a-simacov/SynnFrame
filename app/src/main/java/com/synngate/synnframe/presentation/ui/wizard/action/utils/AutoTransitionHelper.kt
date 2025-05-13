import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory

/**
 * Утилита для реализации автоперехода между шагами
 */
object AutoTransitionHelper {
    /**
     * Проверяет, нужен ли автопереход для шага при изменении поля
     * @param factory Фабрика шагов
     * @param step Текущий шаг
     * @param fieldName Название измененного поля
     * @return true, если нужен автопереход, иначе false
     */
    fun shouldAutoTransition(factory: ActionStepFactory?, step: ActionStep, fieldName: String): Boolean {
        return factory is AutoCompleteCapableFactory &&
                factory.isAutoCompleteEnabled(step) &&
                factory.getAutoCompleteFieldName(step) == fieldName
    }

    /**
     * Проверяет, нужно ли подтверждение перед автопереходом
     * @param factory Фабрика шагов
     * @param step Текущий шаг
     * @param fieldName Название поля
     * @return true, если нужно подтверждение, иначе false
     */
    fun requiresConfirmation(factory: ActionStepFactory?, step: ActionStep, fieldName: String): Boolean {
        return (factory as? AutoCompleteCapableFactory)?.requiresConfirmation(step, fieldName) ?: false
    }
}

/**
 * Пример использования в конкретной ViewModel:
 *
 * class PalletSelectionViewModel(
 *     step: ActionStep,
 *     action: PlannedAction,
 *     context: ActionContext,
 *     private val palletLookupService: PalletLookupService,
 *     validationService: ValidationService,
 *     private val actionStepFactoryRegistry: ActionStepFactoryRegistry
 * ) : BaseStepViewModel<Pallet>(step, action, context, validationService) {
 *
 *     fun selectPallet(pallet: Pallet) {
 *         selectedPallet = pallet
 *         setData(pallet)
 *
 *         // Проверяем, нужен ли автопереход
 *         val factory = actionStepFactoryRegistry.getFactory(step.objectType)
 *         if (AutoTransitionHelper.shouldAutoTransition(factory, step, "selectedPallet")) {
 *             if (AutoTransitionHelper.requiresConfirmation(factory, step, "selectedPallet")) {
 *                 // Показываем диалог подтверждения
 *                 showConfirmationDialog(pallet)
 *             } else {
 *                 // Автоматически завершаем шаг
 *                 completeStep(pallet)
 *             }
 *         }
 *     }
 *
 *     private fun showConfirmationDialog(pallet: Pallet) {
 *         // Реализация диалога подтверждения
 *     }
 * }
 */