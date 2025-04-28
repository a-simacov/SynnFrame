package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicTasksState

/**
 * Компонент отображения поиска на экране заданий
 */
class SearchComponent(
    private val state: DynamicTasksState,
    private val events: DynamicTasksScreenEvents
) : ScreenComponent {

    @Composable
    override fun Render(modifier: Modifier) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(8.dp))

            SearchTextField(
                value = state.searchValue,
                onValueChange = events.onSearchValueChanged,
                label = stringResource(id = R.string.search_value),
                onSearch = events.onSearch,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = events.onSearch,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.search))
            }
        }
    }
}

/**
 * Расширение реестра компонентов для регистрации компонента поиска
 */
fun ScreenComponentRegistry.registerSearchComponent() {
    registerComponent(ScreenElementType.SEARCH) { state ->
        SearchComponent(state, events)
    }
}