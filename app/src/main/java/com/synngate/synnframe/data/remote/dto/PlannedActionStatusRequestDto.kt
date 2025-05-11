package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class PlannedActionStatusRequestDto(
    val plannedActionId: String,
    val manuallyCompleted: Boolean,
    val manuallyCompletedAt: String? = null
) {
    companion object {
        fun fromPlannedAction(
            plannedActionId: String,
            manuallyCompleted: Boolean,
            manuallyCompletedAt: LocalDateTime?
        ): PlannedActionStatusRequestDto {
            return PlannedActionStatusRequestDto(
                plannedActionId = plannedActionId,
                manuallyCompleted = manuallyCompleted,
                manuallyCompletedAt = manuallyCompletedAt?.toString()
            )
        }
    }
}