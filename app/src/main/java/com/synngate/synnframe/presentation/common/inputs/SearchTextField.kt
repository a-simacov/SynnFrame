package com.synngate.synnframe.presentation.common.inputs

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.synngate.synnframe.R

@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSearch: () -> Unit = {},
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit) = {}
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
        onImeAction = onSearch,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(id = R.string.search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onSearch.invoke() }
            )
        },
        trailingIcon = trailingIcon,
        placeholder = placeholder?.let {
            { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        }
    )
}