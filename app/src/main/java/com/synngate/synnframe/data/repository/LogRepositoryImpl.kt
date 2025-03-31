package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.local.dao.LogDao
import com.synngate.synnframe.data.local.entity.LogEntity
import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class LogRepositoryImpl(
    private val logDao: LogDao
) : LogRepository {

    override fun getLogs(): Flow<List<Log>> {
        return logDao.getAllLogs().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFilteredLogs(
        messageFilter: String?,
        typeFilter: List<LogType>?,
        dateFromFilter: LocalDateTime?,
        dateToFilter: LocalDateTime?
    ): Flow<List<Log>> {
        // Определяем параметры фильтрации
        val hasMessageFilter = !messageFilter.isNullOrEmpty()
        val hasTypeFilter = !typeFilter.isNullOrEmpty()
        val hasDateFilter = dateFromFilter != null && dateToFilter != null

        // Применяем соответствующий запрос в зависимости от комбинации фильтров
        return when {
            hasMessageFilter && hasTypeFilter && hasDateFilter -> {
                val types = typeFilter!!.map { it.name }
                val from = dateFromFilter!!
                val to = dateToFilter!!
                logDao.getFilteredLogs(types, messageFilter!!, from, to)
            }
            hasMessageFilter && hasTypeFilter -> {
                val types = typeFilter!!.map { it.name }
                logDao.getLogsByTypesAndMessageFilter(types, messageFilter!!)
            }
            hasMessageFilter && hasDateFilter -> {
                val from = dateFromFilter!!
                val to = dateToFilter!!
                logDao.getLogsByMessageFilter(messageFilter!!)
                    .map { entities ->
                        entities.filter { it.createdAt in from..to }
                    }
            }
            hasTypeFilter && hasDateFilter -> {
                val types = typeFilter!!.map { it.name }
                val from = dateFromFilter!!
                val to = dateToFilter!!
                logDao.getLogsByTypes(types)
                    .map { entities ->
                        entities.filter { it.createdAt in from..to }
                    }
            }
            hasMessageFilter -> {
                logDao.getLogsByMessageFilter(messageFilter!!)
            }
            hasTypeFilter -> {
                val types = typeFilter!!.map { it.name }
                logDao.getLogsByTypes(types)
            }
            hasDateFilter -> {
                val from = dateFromFilter!!
                val to = dateToFilter!!
                logDao.getLogsByDateRange(from, to)
            }
            else -> {
                logDao.getAllLogs()
            }
        }.map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getLogById(id: Int): Log? {
        return logDao.getLogById(id)?.toDomainModel()
    }

    override suspend fun addLog(log: Log): Long {
        val entity = LogEntity.fromDomainModel(log)
        return logDao.insertLog(entity)
    }

    override suspend fun deleteLog(id: Int) {
        logDao.deleteLogById(id)
    }

    override suspend fun deleteAllLogs() {
        logDao.deleteAllLogs()
    }

    override suspend fun deleteLogsOlderThan(date: LocalDateTime): Int {
        return logDao.deleteLogsOlderThan(date)
    }
}