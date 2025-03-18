package com.synngate.synnframe.presentation.di

/**
 * Интерфейс для очистки ресурсов контейнера зависимостей
 */
interface Clearable {
    /**
     * Метод для очистки ресурсов
     */
    fun clear()
}

/**
 * Интерфейс для контейнера зависимостей, который можно очистить
 */
interface ClearableContainer : Clearable {
    /**
     * Список очищаемых ресурсов
     */
    val clearables: MutableList<Clearable>

    /**
     * Добавление ресурса для последующей очистки
     */
    fun addClearable(clearable: Clearable) {
        clearables.add(clearable)
    }

    /**
     * Очистка всех добавленных ресурсов
     */
    override fun clear() {
        clearables.forEach { it.clear() }
        clearables.clear()
    }
}

/**
 * Интерфейс для контейнера навигационного хоста
 */
interface NavHostContainer : ClearableContainer {
    /**
     * Создание контейнера для подграфа серверов
     */
    fun createServerListGraphContainer(): ServerListGraphContainer

    /**
     * Создание контейнера для подграфа заданий
     */
    fun createTasksGraphContainer(): TasksGraphContainer

    /**
     * Создание контейнера для подграфа товаров
     */
    fun createProductsGraphContainer(): ProductsGraphContainer

    /**
     * Создание контейнера для подграфа логов
     */
    fun createLogsGraphContainer(): LogsGraphContainer

    /**
     * Создание контейнера для экрана настроек
     */
    fun createSettingsScreenContainer(): SettingsScreenContainer
}

/**
 * Интерфейс для базового контейнера подграфа навигации
 */
interface GraphContainer : ClearableContainer

/**
 * Интерфейс для контейнера подграфа серверов
 */
interface ServerListGraphContainer : GraphContainer {
    /**
     * Создание ViewModel для экрана списка серверов
     */
    fun createServerListViewModel(): ServerListViewModel

    /**
     * Создание ViewModel для экрана деталей сервера
     */
    fun createServerDetailViewModel(serverId: Int?): ServerDetailViewModel
}

/**
 * Интерфейс для контейнера подграфа заданий
 */
interface TasksGraphContainer : GraphContainer {
    /**
     * Создание ViewModel для экрана списка заданий
     */
    fun createTaskListViewModel(): TaskListViewModel

    /**
     * Создание ViewModel для экрана деталей задания
     */
    fun createTaskDetailViewModel(taskId: String): TaskDetailViewModel
}

/**
 * Интерфейс для контейнера подграфа товаров
 */
interface ProductsGraphContainer : GraphContainer {
    /**
     * Создание ViewModel для экрана списка товаров
     */
    fun createProductListViewModel(): ProductListViewModel

    /**
     * Создание ViewModel для экрана деталей товара
     */
    fun createProductDetailViewModel(productId: String): ProductDetailViewModel
}

/**
 * Интерфейс для контейнера подграфа логов
 */
interface LogsGraphContainer : GraphContainer {
    /**
     * Создание ViewModel для экрана списка логов
     */
    fun createLogListViewModel(): LogListViewModel

    /**
     * Создание ViewModel для экрана деталей лога
     */
    fun createLogDetailViewModel(logId: Int): LogDetailViewModel
}

/**
 * Интерфейс для контейнера экрана настроек
 */
interface SettingsScreenContainer : GraphContainer {
    /**
     * Создание ViewModel для экрана настроек
     */
    fun createSettingsViewModel(): SettingsViewModel
}

// Интерфейсы для ViewModels (заглушки, будут реализованы позже)
interface ServerListViewModel
interface ServerDetailViewModel
interface TaskListViewModel
interface TaskDetailViewModel
interface ProductListViewModel
interface ProductDetailViewModel
interface LogListViewModel
interface LogDetailViewModel
interface SettingsViewModel