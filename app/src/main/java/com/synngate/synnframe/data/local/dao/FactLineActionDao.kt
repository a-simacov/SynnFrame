package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.synngate.synnframe.data.local.entity.FactLineActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FactLineActionDao {
    @Query("SELECT * FROM fact_line_actions WHERE taskTypeId = :taskTypeId ORDER BY `order`")
    fun getActionsForTaskType(taskTypeId: String): Flow<List<FactLineActionEntity>>

    @Query("SELECT * FROM fact_line_actions WHERE taskTypeId = :taskTypeId ORDER BY `order`")
    suspend fun getActionsForTaskTypeSync(taskTypeId: String): List<FactLineActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFactLineAction(action: FactLineActionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFactLineActions(actions: List<FactLineActionEntity>)

    @Query("DELETE FROM fact_line_actions WHERE taskTypeId = :taskTypeId")
    suspend fun deleteActionsForTaskType(taskTypeId: String)
}