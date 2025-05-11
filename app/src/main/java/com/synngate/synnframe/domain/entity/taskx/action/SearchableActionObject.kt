package com.synngate.synnframe.domain.entity.taskx.action

import kotlinx.serialization.Serializable

@Serializable
data class SearchableActionObject(
    val objectType: ActionObjectType,
    val isRemoteSearch: Boolean = false,
    val endpoint: String? = null
)