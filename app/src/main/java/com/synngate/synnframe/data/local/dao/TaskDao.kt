package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.synngate.synnframe.data.local.entity.TaskEntity
import com.synngate.synnframe.data.local.entity.TaskFactLineEntity
import com.synngate.synnframe.data.local.entity.TaskPlanLineEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskDao {
    // Методы для работы с основной таблицей заданий
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE name LIKE '%' || :nameFilter || '%' OR barcode LIKE '%' || :nameFilter || '%' ORDER BY createdAt DESC")
    fun getTasksByNameFilter(nameFilter: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun getTasksByStatuses(statuses: List<String>): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE type = :type ORDER BY createdAt DESC")
    fun getTasksByType(type: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE createdAt BETWEEN :dateFrom AND :dateTo ORDER BY createdAt DESC")
    fun getTasksByDateRange(dateFrom: LocalDateTime, dateTo: LocalDateTime): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE executorId = :executorId ORDER BY createdAt DESC")
    fun getTasksByExecutor(executorId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE " +
            "(name LIKE '%' || :nameFilter || '%' OR barcode LIKE '%' || :nameFilter || '%') " +
            "AND (:hasStatusFilter = 0 OR status IN (:statuses)) " +
            "AND (:hasTypeFilter = 0 OR type = :type) " +
            "AND (:hasDateFilter = 0 OR createdAt BETWEEN :dateFrom AND :dateTo) " +
            "AND (:hasExecutorFilter = 0 OR executorId = :executorId) " +
            "ORDER BY createdAt DESC")
    fun getFilteredTasks(
        nameFilter: String,
        hasStatusFilter: Boolean,
        statuses: List<String>,
        hasTypeFilter: Boolean,
        type: String,
        hasDateFilter: Boolean,
        dateFrom: LocalDateTime,
        dateTo: LocalDateTime,
        hasExecutorFilter: Boolean,
        executorId: String
    ): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE barcode = :barcode")
    suspend fun getTaskByBarcode(barcode: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' AND uploaded = 0")
    suspend fun getCompletedNotUploadedTasks(): List<TaskEntity>

    // Методы для работы со строками плана и факта
    @Query("SELECT * FROM task_plan_lines WHERE taskId = :taskId")
    suspend fun getPlanLinesForTask(taskId: String): List<TaskPlanLineEntity>

    @Query("SELECT * FROM task_fact_lines WHERE taskId = :taskId")
    suspend fun getFactLinesForTask(taskId: String): List<TaskFactLineEntity>

    // Методы для массовой загрузки связанных данных
    @Query("SELECT * FROM task_plan_lines WHERE taskId IN (:taskIds)")
    suspend fun getPlanLinesForTasks(taskIds: List<String>): List<TaskPlanLineEntity>

    @Query("SELECT * FROM task_fact_lines WHERE taskId IN (:taskIds)")
    suspend fun getFactLinesForTasks(taskIds: List<String>): List<TaskFactLineEntity>

    // Существующие методы для вставки, обновления и удаления
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskPlanLine(planLine: TaskPlanLineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskFactLine(factLine: TaskFactLineEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Update
    suspend fun updateTaskPlanLine(planLine: TaskPlanLineEntity)

    @Update
    suspend fun updateTaskFactLine(factLine: TaskFactLineEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Delete
    suspend fun deleteTaskPlanLine(planLine: TaskPlanLineEntity)

    @Delete
    suspend fun deleteTaskFactLine(factLine: TaskFactLineEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM task_plan_lines WHERE taskId = :taskId")
    suspend fun deleteTaskPlanLinesForTask(taskId: String)

    @Query("DELETE FROM task_fact_lines WHERE taskId = :taskId")
    suspend fun deleteTaskFactLinesForTask(taskId: String)

    @Query("SELECT * FROM task_fact_lines WHERE taskId = :taskId AND productId = :productId")
    suspend fun getTaskFactLineByTaskAndProduct(taskId: String, productId: String): TaskFactLineEntity?

    @Query("SELECT * FROM task_plan_lines WHERE taskId = :taskId AND productId = :productId")
    suspend fun getTaskPlanLineByTaskAndProduct(taskId: String, productId: String): TaskPlanLineEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE executorId = :executorId")
    fun getTasksCountForExecutor(executorId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status IN (:statuses)")
    fun getTasksCountByStatuses(statuses: List<String>): Flow<Int>
}