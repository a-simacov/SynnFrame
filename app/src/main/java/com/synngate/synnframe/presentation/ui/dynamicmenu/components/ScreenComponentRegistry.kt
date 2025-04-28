package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.operation.ScreenElementType

/**
 * Универсальный реестр компонентов экрана.
 * Используется для создания и хранения компонентов экрана различных типов.
 *
 * @param S тип состояния экрана, которое реализует интерфейс ScreenElementsContainer
 */
class GenericScreenComponentRegistry<S : ScreenElementsContainer> : GenericScreenComponentFactory<S> {

    private val componentFactories = mutableMapOf<ScreenElementType, @Composable (S) -> ScreenComponent>()

    /**
     * Регистрация фабрики компонента определенного типа
     */
    fun registerComponent(
        type: ScreenElementType,
        factory: @Composable (S) -> ScreenComponent
    ) {
        componentFactories[type] = factory
    }

    /**
     * Создание компонента указанного типа на основе текущего состояния
     */
    @Composable
    override fun createComponent(type: ScreenElementType, state: S): ScreenComponent? {
        return componentFactories[type]?.invoke(state)
    }
}

/**
 * Composable функция для создания и инициализации реестра компонентов
 */
@Composable
fun <S : ScreenElementsContainer> rememberGenericScreenComponentRegistry(): GenericScreenComponentRegistry<S> {
    return remember { GenericScreenComponentRegistry<S>() }
}