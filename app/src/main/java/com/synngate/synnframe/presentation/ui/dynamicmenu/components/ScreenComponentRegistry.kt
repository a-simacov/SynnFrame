package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.common.inputs.SearchTextField

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

/**
 * Функция для инициализации реестра компонентов для заданий
 */
fun <S : ScreenElementsContainer> GenericScreenComponentRegistry<S>.initializeTaskComponents(
    tasksProvider: (S) -> List<DynamicTask>,
    isLoadingProvider: (S) -> Boolean,
    errorProvider: (S) -> String?,
    onTaskClickProvider: (S) -> ((DynamicTask) -> Unit),
    searchValueProvider: (S) -> String,
    onSearchValueChangedProvider: (S) -> ((String) -> Unit),
    onSearchProvider: (S) -> (() -> Unit)
) {
    // Регистрация компонента списка заданий
    registerComponent(ScreenElementType.SHOW_LIST) { state ->
        TaskListComponent(
            state = state,
            tasks = tasksProvider(state),
            isLoading = isLoadingProvider(state),
            error = errorProvider(state),
            onTaskClick = onTaskClickProvider(state)
        )
    }

    // Регистрация компонента поиска
    registerComponent(ScreenElementType.SEARCH) { state ->
        SearchComponent(
            searchValue = searchValueProvider(state),
            onSearchValueChanged = onSearchValueChangedProvider(state),
            onSearch = onSearchProvider(state)
        )
    }
}

/**
 * Универсальный компонент поиска
 */
class SearchComponent(
    private val searchValue: String,
    private val onSearchValueChanged: (String) -> Unit,
    private val onSearch: () -> Unit
) : ScreenComponent {

    @Composable
    override fun Render(modifier: Modifier) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(8.dp))

            SearchTextField(
                value = searchValue,
                onValueChange = onSearchValueChanged,
                label = stringResource(id = R.string.search_value),
                onSearch = onSearch,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.search))
            }
        }
    }

    override fun usesWeight(): Boolean = false
}