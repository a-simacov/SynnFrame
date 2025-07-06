package com.synngate.synnframe.presentation.ui.dynamicmenu.customlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.search.SearchResultIndicator
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.di.ScreenContainer
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.createComponentGroups
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.rememberGenericScreenComponentRegistry
import com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.component.initializeCustomListComponents
import com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.model.DynamicCustomListEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.model.DynamicCustomListState
import com.synngate.synnframe.util.html.HtmlUtils
import kotlinx.coroutines.launch

@Composable
fun DynamicCustomListScreen(
    viewModel: DynamicCustomListViewModel,
    navigateBack: () -> Unit,
    screenContainer: ScreenContainer,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DynamicCustomListEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                is DynamicCustomListEvent.ShowItemDetails -> {
                    coroutineScope.launch {
                        // Show item description in a snackbar with HTML formatting stripped
                        val plainText = HtmlUtils.stripHtml(event.item.description)
                        snackbarHostState.showSnackbar(
                            message = plainText,
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }
        }
    }

    // Create and setup component registry
    val componentRegistry = rememberGenericScreenComponentRegistry<DynamicCustomListState>()

    // Initialize components for custom list
    componentRegistry.initializeCustomListComponents(
        itemsProvider = { it.items },
        isLoadingProvider = { it.isLoading },
        errorProvider = { it.error },
        onItemClickProvider = { { item -> viewModel.onItemClick(item) } },
        searchValueProvider = { it.searchKey },
        onSearchValueChangedProvider = { { value -> viewModel.onSearchValueChanged(value) } },
        onSearchProvider = { { viewModel.onSearch() } },
        searchResultTypeProvider = { it.searchResultType },
        lastSearchQueryProvider = { it.lastSearchQuery }
    )

    // Create component groups based on screen settings
    val componentGroups = createComponentGroups(state, componentRegistry)

    AppScaffold(
        title = state.menuItemName,
        onNavigateBack = navigateBack,
        actions = {
            IconButton(onClick = { viewModel.onRefresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.refresh)
                )
            }
        },
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        onDismissNotification = { viewModel.clearError() },
        isLoading = state.isLoading
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!state.hasElement(ScreenElementType.SHOW_LIST) && !state.isLoading && state.items.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_tasks_available),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                ) {
                    // Display fixed components (search, filters, etc.)
                    componentGroups.fixedComponents.forEach { component ->
                        component.Render(Modifier.fillMaxWidth())
                    }

                    // Show search result indicator if there are search results
                    if (state.shouldShowSearchIndicator()) {
                        state.searchResultType?.let { resultType ->
                            SearchResultIndicator(
                                resultType = resultType,
                                count = state.items.size,
                                query = state.lastSearchQuery,
                                itemType = "item"
                            )
                        }
                    }

                    // Display weighted components (lists that should take remaining space)
                    componentGroups.weightedComponents.forEach { component ->
                        component.Render(
                            Modifier
                                .fillMaxWidth()
                                .weight(component.getWeight())
                        )
                    }
                }
            }
        }
    }
}