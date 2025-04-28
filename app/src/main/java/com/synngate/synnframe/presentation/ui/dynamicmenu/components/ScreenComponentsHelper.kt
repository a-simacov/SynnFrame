package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.entity.operation.ScreenSettings

/**
 * Интерфейс для состояний динамических экранов,
 * которые поддерживают элементы экрана
 */
interface ScreenElementsContainer {
    /**
     * Возвращает настройки экрана с определением элементов
     */
    val screenSettings: ScreenSettings
}

/**
 * Универсальный интерфейс фабрики компонентов
 * @param S тип состояния экрана
 */
interface GenericScreenComponentFactory<S> {
    /**
     * Создание компонента по типу элемента и состоянию
     */
    @Composable
    fun createComponent(type: ScreenElementType, state: S): ScreenComponent?
}

/**
 * Класс-обертка, содержащий созданные компоненты, разделенные на категории
 */
class ComponentGroups(
    val fixedComponents: List<ScreenComponent>,
    val weightedComponents: List<ScreenComponent>
)

/**
 * Функция для создания и группировки компонентов экрана
 *
 * @param state Состояние динамического экрана
 * @param registry Реестр компонентов
 * @return Группы компонентов, разделенные по использованию веса
 */
@Composable
fun <S : ScreenElementsContainer> createComponentGroups(
    state: S,
    registry: GenericScreenComponentFactory<S>
): ComponentGroups {
    // Создаем все компоненты один раз
    val allComponents = state.screenSettings.screenElements.mapNotNull { elementType ->
        registry.createComponent(elementType, state)
    }

    // Группируем компоненты по принципу использования веса
    val fixedComponents = allComponents.filter { !it.usesWeight() }
    val weightedComponents = allComponents.filter { it.usesWeight() }

    return remember(fixedComponents, weightedComponents) {
        ComponentGroups(fixedComponents, weightedComponents)
    }
}