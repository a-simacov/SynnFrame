package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.operation.ScreenElementType

class GenericScreenComponentRegistry<S : ScreenElementsContainer> : GenericScreenComponentFactory<S> {

    private val componentFactories = mutableMapOf<ScreenElementType, @Composable (S) -> ScreenComponent>()

    fun registerComponent(
        type: ScreenElementType,
        factory: @Composable (S) -> ScreenComponent
    ) {
        componentFactories[type] = factory
    }

    @Composable
    override fun createComponent(type: ScreenElementType, state: S): ScreenComponent? {
        return componentFactories[type]?.invoke(state)
    }
}

@Composable
fun <S : ScreenElementsContainer> rememberGenericScreenComponentRegistry(): GenericScreenComponentRegistry<S> {
    return remember { GenericScreenComponentRegistry<S>() }
}