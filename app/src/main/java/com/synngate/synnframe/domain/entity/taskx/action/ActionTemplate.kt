package com.synngate.synnframe.domain.entity.taskx.action

import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.ActionCompletionCondition
import com.synngate.synnframe.presentation.ui.taskx.enums.ActionCompletionMethod
import kotlinx.serialization.Serializable

@Serializable
data class ActionTemplate(
    val id: String,
    val name: String,
    val wmsAction: WmsAction,
    val allowMultipleFactActions: Boolean = false,
    val allowManualActionCompletion: Boolean = false,
    val syncWithServer: Boolean = true,
    val actionCompletionMethod: ActionCompletionMethod = ActionCompletionMethod.AFTER_LAST_STEP,
    val actionCompletionCondition: ActionCompletionCondition = ActionCompletionCondition.ON_COMPLETION,
    val actionSteps: List<ActionStepTemplate> = emptyList()
) {

    fun isAutoCompleteEnabled(): Boolean {
        return actionCompletionMethod == ActionCompletionMethod.AFTER_LAST_STEP
    }
}