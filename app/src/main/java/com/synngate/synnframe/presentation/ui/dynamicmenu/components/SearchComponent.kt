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
import com.synngate.synnframe.presentation.common.inputs.SearchTextField

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