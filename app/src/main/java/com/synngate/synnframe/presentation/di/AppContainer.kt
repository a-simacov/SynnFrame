package com.synngate.synnframe.presentation.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import timber.log.Timber

/**
 * Интерфейс для контейнера зависимостей приложения
 */
//interface AppContainer {
//    // Здесь будут объявлены все зависимости, которые доступны на уровне приложения
//    val appSettingsDataStore: AppSettingsDataStore
//}

///**
// * Реализация контейнера зависимостей для всего приложения.
// * Используется для ручного DI согласно требованиям.
// */
//class AppContainerImpl(private val applicationContext: Context) : AppContainer {
//
//    // Единый экземпляр DataStore для настроек приложения
//    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
//
//    // DataStore для хранения настроек приложения
//    override val appSettingsDataStore by lazy {
//        Timber.d("Creating AppSettingsDataStore")
//        AppSettingsDataStore(applicationContext.dataStore)
//    }
//
//    // Остальные зависимости будут добавлены по мере разработки
//    // - База данных Room
//    // - Репозитории
//    // - UseCase
//    // - API клиенты
//    // - Сервисы
//}