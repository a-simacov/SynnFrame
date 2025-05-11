package com.synngate.synnframe.util.logging

import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LogLevelProvider(
    private val appSettingsDataStore: AppSettingsDataStore
) : AppTree.LogLevelProvider {

    private var currentLogLevel = LogLevel.FULL

    init {
        // Изначально блокирующе получаем текущий уровень
        runBlocking {
            currentLogLevel = appSettingsDataStore.logLevel.first()
        }

        // Подписываемся на изменения
        CoroutineScope(Dispatchers.IO).launch {
            appSettingsDataStore.logLevel.collect { level ->
                currentLogLevel = level
            }
        }
    }

    override fun getCurrentLogLevel(): LogLevel {
        return currentLogLevel
    }
}