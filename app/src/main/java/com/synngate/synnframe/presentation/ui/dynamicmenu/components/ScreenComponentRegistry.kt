package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksState

/**
 * Реестр компонентов экрана.
 * Используется для создания и хранения компонентов экрана различных типов.
 */
class ScreenComponentRegistry(
    val events: DynamicTasksScreenEvents
) : ScreenComponentFactory {

    private val componentFactories = mutableMapOf<ScreenElementType, @Composable (DynamicTasksState) -> ScreenComponent>()

    /**
     * Регистрация фабрики компонента определенного типа
     */
    fun registerComponent(
        type: ScreenElementType,
        factory: @Composable (DynamicTasksState) -> ScreenComponent
    ) {
        componentFactories[type] = factory
    }

    /**
     * Создание компонента указанного типа на основе текущего состояния
     */
    @Composable
    override fun createComponent(type: ScreenElementType, state: DynamicTasksState): ScreenComponent? {
        return componentFactories[type]?.invoke(state)
    }
}

/**
 * Composable функция для создания и инициализации реестра компонентов
 */
@Composable
fun rememberScreenComponentRegistry(events: DynamicTasksScreenEvents): ScreenComponentRegistry {
    return remember(events) {
        createScreenComponentRegistry(events)
    }
}

/**
 * Создание и инициализация реестра компонентов с регистрацией всех доступных типов
 */
private fun createScreenComponentRegistry(events: DynamicTasksScreenEvents): ScreenComponentRegistry {
    return ScreenComponentRegistry(events).apply {
        // Здесь регистрируем фабрики для всех типов компонентов
        // Регистрация будет происходить в конкретных реализациях
    }
}