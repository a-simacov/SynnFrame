package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.synngate.synnframe.data.local.entity.TaskEntity
import com.synngate.synnframe.data.local.entity.TaskFactLineEntity
import com.synngate.synnframe.data.local.entity.TaskPlanLineEntity
import com.synngate.synnframe.data.local.entity.TaskWithDetails
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO для работы с заданиями в базе данных
 */
@Dao
interface TaskDao {
    /**
     * Получение всех заданий с деталями
     */
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksWithDetails(): Flow<List<TaskWithDetails>>

    /**
     * Получение заданий с фильтрацией по имени или штрихкоду
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE name LIKE '%' || :nameFilter || '%' OR barcode LIKE '%' || :nameFilter || '%' ORDER BY createdAt DESC")
    fun getTasksByNameFilter(nameFilter: String): Flow<List<TaskWithDetails>>

    /**
     * Получение заданий с фильтрацией по статусу
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun getTasksByStatuses(statuses: List<String>): Flow<List<TaskWithDetails>>

    /**
     * Получение заданий с фильтрацией по типу
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE type = :type ORDER BY createdAt DESC")
    fun getTasksByType(type: String): Flow<List<TaskWithDetails>>

    /**
     * Получение заданий с фильтрацией по дате создания
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE createdAt BETWEEN :dateFrom AND :dateTo ORDER BY createdAt DESC")
    fun getTasksByDateRange(dateFrom: LocalDateTime, dateTo: LocalDateTime): Flow<List<TaskWithDetails>>

    /**
     * Получение заданий с фильтрацией по исполнителю
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE executorId = :executorId ORDER BY createdAt DESC")
    fun getTasksByExecutor(executorId: String): Flow<List<TaskWithDetails>>

    /**
     * Получение заданий с комплексной фильтрацией
     */
    @Transaction
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
    ): Flow<List<TaskWithDetails>>

    /**
     * Получение задания по идентификатору
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskWithDetailsById(id: String): TaskWithDetails?

    /**
     * Получение задания по штрихкоду
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE barcode = :barcode")
    suspend fun getTaskByBarcode(barcode: String): TaskWithDetails?

    /**
     * Получение количества заданий для исполнителя
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE executorId = :executorId")
    fun getTasksCountForExecutor(executorId: String): Flow<Int>

    /**
     * Получение количества заданий по статусам
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE status IN (:statuses)")
    fun getTasksCountByStatuses(statuses: List<String>): Flow<Int>

    /**
     * Вставка задания
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    /**
     * Вставка строки плана задания
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskPlanLine(planLine: TaskPlanLineEntity)

    /**
     * Вставка строки факта задания
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskFactLine(factLine: TaskFactLineEntity)

    /**
     * Обновление задания
     */
    @Update
    suspend fun updateTask(task: TaskEntity)

    /**
     * Обновление строки плана задания
     */
    @Update
    suspend fun updateTaskPlanLine(planLine: TaskPlanLineEntity)

    /**
     * Обновление строки факта задания
     */
    @Update
    suspend fun updateTaskFactLine(factLine: TaskFactLineEntity)

    /**
     * Удаление задания
     */
    @Delete
    suspend fun deleteTask(task: TaskEntity)

    /**
     * Удаление строки плана задания
     */
    @Delete
    suspend fun deleteTaskPlanLine(planLine: TaskPlanLineEntity)

    /**
     * Удаление строки факта задания
     */
    @Delete
    suspend fun deleteTaskFactLine(factLine: TaskFactLineEntity)

    /**
     * Удаление задания по идентификатору
     */
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    /**
     * Удаление всех строк плана задания
     */
    @Query("DELETE FROM task_plan_lines WHERE taskId = :taskId")
    suspend fun deleteTaskPlanLinesForTask(taskId: String)

    /**
     * Удаление всех строк факта задания
     */
    @Query("DELETE FROM task_fact_lines WHERE taskId = :taskId")
    suspend fun deleteTaskFactLinesForTask(taskId: String)

    /**
     * Получение строки факта задания по идентификатору задания и товара
     */
    @Query("SELECT * FROM task_fact_lines WHERE taskId = :taskId AND productId = :productId")
    suspend fun getTaskFactLineByTaskAndProduct(taskId: String, productId: String): TaskFactLineEntity?

    /**
     * Получение строки плана задания по идентификатору задания и товара
     */
    @Query("SELECT * FROM task_plan_lines WHERE taskId = :taskId AND productId = :productId")
    suspend fun getTaskPlanLineByTaskAndProduct(taskId: String, productId: String): TaskPlanLineEntity?

    /**
     * Получение всех завершенных, но не выгруженных заданий
     */
    @Transaction
    @Query("SELECT * FROM tasks WHERE status = 'COMPLETED' AND uploaded = 0")
    suspend fun getCompletedNotUploadedTasks(): List<TaskWithDetails>
}