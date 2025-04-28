package com.synngate.synnframe.presentation.ui.dynamicmenu.components

/**
 * Класс-инициализатор компонентов, регистрирующий все доступные компоненты в реестре.
 * Это место для добавления новых типов компонентов в будущем.
 */
object ComponentInitializer {

    /**
     * Регистрирует все компоненты в реестре
     */
    fun initializeRegistry(registry: ScreenComponentRegistry) {
        registry.apply {
            // Регистрация всех доступных компонентов
            registerSearchComponent()
            registerTaskListComponent()

            // В будущем здесь можно будет добавить регистрацию новых компонентов:
            // registerFilterComponent()
            // registerSortComponent()
            // registerCalendarComponent()
            // и т.д.
        }
    }
}

/**
 * Расширение для ScreenComponentRegistry, которое инициализирует все компоненты
 */
fun ScreenComponentRegistry.initialize() {
    ComponentInitializer.initializeRegistry(this)
}