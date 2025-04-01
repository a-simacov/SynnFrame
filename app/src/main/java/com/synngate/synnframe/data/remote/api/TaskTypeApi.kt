package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.domain.entity.TaskType

interface TaskTypeApi {

    suspend fun getTaskTypes(): ApiResult<List<TaskType>>
}