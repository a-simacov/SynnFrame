package com.synngate.synnframe.presentation.common.scaffold

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Кнопка для включения/выключения режима поиска
 */
@Composable
fun SearchButton(
    isSearchActive: Boolean,
    onToggleSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Search
    val description = if (isSearchActive) "Выключить поиск" else "Включить поиск"

    IconButton(
        onClick = onToggleSearch,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}