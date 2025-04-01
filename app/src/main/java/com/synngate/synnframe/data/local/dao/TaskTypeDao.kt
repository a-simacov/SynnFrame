package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.synngate.synnframe.data.local.entity.TaskTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTypeDao {
    @Query("SELECT * FROM task_types")
    fun getAllTaskTypes(): Flow<List<TaskTypeEntity>>

    @Query("SELECT * FROM task_types WHERE id = :id")
    suspend fun getTaskTypeById(id: String): TaskTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskType(taskType: TaskTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTypes(taskTypes: List<TaskTypeEntity>)

    @Query("DELETE FROM task_types")
    suspend fun deleteAllTaskTypes()
}