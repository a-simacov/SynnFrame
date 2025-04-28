package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksState

/**
 * Интерфейс для компонентов экрана.
 * Каждый компонент отвечает за отображение определенного элемента экрана.
 */
interface ScreenComponent {
    /**
     * Отрисовка компонента
     */
    @Composable
    fun Render(modifier: Modifier)

    /**
     * Возвращает true, если компонент должен использовать модификатор веса (weight)
     */
    fun usesWeight(): Boolean = false

    /**
     * Возвращает значение веса компонента, если он его использует
     */
    fun getWeight(): Float = 1f
}

/**
 * Интерфейс для фабрики компонентов экрана
 */
interface ScreenComponentFactory {
    /**
     * Создание компонента указанного типа
     * @param state Текущее состояние экрана
     * @return Компонент или null, если компонент не поддерживается
     */
    @Composable
    fun createComponent(type: ScreenElementType, state: DynamicTasksState): ScreenComponent?
}

/**
 * Класс для хранения всех событий экрана, которые могут использоваться компонентами
 */
data class DynamicTasksScreenEvents(
    val onSearchValueChanged: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onTaskClick: (com.synngate.synnframe.domain.entity.operation.DynamicTask) -> Unit = {}
)