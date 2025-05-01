package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.entity.operation.ScreenSettings

interface ScreenElementsContainer {

    val screenSettings: ScreenSettings
}

interface GenericScreenComponentFactory<S> {

    @Composable
    fun createComponent(type: ScreenElementType, state: S): ScreenComponent?
}

class ComponentGroups(
    val fixedComponents: List<ScreenComponent>,
    val weightedComponents: List<ScreenComponent>
)

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