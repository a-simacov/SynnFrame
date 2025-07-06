package com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.CustomListAlignment
import com.synngate.synnframe.domain.entity.operation.CustomListItem
import com.synngate.synnframe.domain.entity.operation.CustomListLayoutType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenComponent
import com.synngate.synnframe.util.html.HtmlUtils

class CustomListComponent<S>(
    private val state: S,
    private val items: List<CustomListItem>,
    private val isLoading: Boolean,
    private val error: String?,
    private val onItemClick: (CustomListItem) -> Unit
) : ScreenComponent {

    @Composable
    override fun Render(modifier: Modifier) {
        Box(modifier = modifier) {
            if (items.isEmpty() && !isLoading) {
                Text(
                    text = if (error == null) {
                        stringResource(id = R.string.no_tasks_available)
                    } else {
                        formatErrorMessage(error)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                CustomItemsList(
                    items = items,
                    onItemClick = onItemClick
                )
            }
        }
    }

    override fun usesWeight(): Boolean = true

    override fun getWeight(): Float = 1f

    @Composable
    private fun CustomItemsList(
        items: List<CustomListItem>,
        onItemClick: (CustomListItem) -> Unit,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(items) { item ->
                CustomListItemCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }

    @Composable
    private fun CustomListItemCard(
        item: CustomListItem,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            when (item.getLayoutType()) {
                CustomListLayoutType.SINGLE_DESCRIPTION -> {
                    RenderSingleDescription(item)
                }
                CustomListLayoutType.START_END -> {
                    RenderStartEnd(item)
                }
                CustomListLayoutType.TOP_BOTTOM -> {
                    RenderTopBottom(item)
                }
                CustomListLayoutType.START_WITH_SPLIT_END -> {
                    RenderStartWithSplitEnd(item)
                }
                CustomListLayoutType.SPLIT_START_WITH_END -> {
                    RenderSplitStartWithEnd(item)
                }
                CustomListLayoutType.TOP_SPLIT_WITH_BOTTOM -> {
                    RenderTopSplitWithBottom(item)
                }
                CustomListLayoutType.TOP_WITH_SPLIT_BOTTOM -> {
                    RenderTopWithSplitBottom(item)
                }
                CustomListLayoutType.GRID_2x2 -> {
                    RenderGrid2x2(item)
                }
            }
        }
    }

    @Composable
    private fun RenderSingleDescription(item: CustomListItem) {
        Text(
            text = HtmlUtils.htmlToAnnotatedString(item.description),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(4.dp)
        )
    }

    @Composable
    private fun RenderStartEnd(item: CustomListItem) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            item.start?.let { start ->
                val (text, alignment, weight) = item.parseTextWithAlignment(start)
                AlignedText(
                    text = text,
                    alignment = alignment,
                    modifier = Modifier.weight(weight)
                )
            }
            
            if (!item.start.isNullOrEmpty() && !item.end.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            item.end?.let { end ->
                val (text, alignment, weight) = item.parseTextWithAlignment(end)
                AlignedText(
                    text = text,
                    alignment = alignment,
                    modifier = Modifier.weight(weight)
                )
            }
        }
    }

    @Composable
    private fun RenderTopBottom(item: CustomListItem) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            item.top?.let { top ->
                val (text, alignment, weight) = item.parseTextWithAlignment(top)
                AlignedText(
                    text = text,
                    alignment = alignment,
                    modifier = Modifier.fillMaxWidth().weight(weight).weight(weight)
                )
            }
            
            if (!item.top.isNullOrEmpty() && !item.bottom.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item.bottom?.let { bottom ->
                val (text, alignment, weight) = item.parseTextWithAlignment(bottom)
                AlignedText(
                    text = text,
                    alignment = alignment,
                    modifier = Modifier.fillMaxWidth().weight(weight).weight(weight)
                )
            }
        }
    }

    @Composable
    private fun RenderStartWithSplitEnd(item: CustomListItem) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Левая часть
            item.start?.let { start ->
                val (text, alignment, weight) = item.parseTextWithAlignment(start)
                AlignedText(
                    text = text,
                    alignment = alignment,
                    modifier = Modifier.weight(weight)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Правая часть разделена  
            val rightWeight = maxOf(
                item.topEnd?.let { item.parseTextWithAlignment(it).third } ?: 1f,
                item.bottomEnd?.let { item.parseTextWithAlignment(it).third } ?: 1f
            )
            
            Column(modifier = Modifier.weight(rightWeight)) {
                item.topEnd?.let { topEnd ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(topEnd)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.fillMaxWidth().weight(weight).weight(weight)
                    )
                }
                
                if (!item.topEnd.isNullOrEmpty() && !item.bottomEnd.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item.bottomEnd?.let { bottomEnd ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(bottomEnd)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.fillMaxWidth().weight(weight).weight(weight)
                    )
                }
            }
        }
    }

    @Composable
    private fun RenderSplitStartWithEnd(item: CustomListItem) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Левая часть разделена
            Column(modifier = Modifier.weight(1f)) {
                item.topStart?.let { topStart ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(topStart)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.fillMaxWidth().weight(weight).weight(weight)
                    )
                }
                
                if (!item.topStart.isNullOrEmpty() && !item.bottomStart.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item.bottomStart?.let { bottomStart ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(bottomStart)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.fillMaxWidth().weight(weight).weight(weight)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Правая часть
            item.end?.let { end ->
                val (text, alignment, weight) = item.parseTextWithAlignment(end)
                AlignedText(
                    text = text,
                    alignment = alignment,
                    modifier = Modifier.weight(weight)
                )
            }
        }
    }

    @Composable
    private fun RenderTopSplitWithBottom(item: CustomListItem) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Верхняя часть разделена
            Row(modifier = Modifier.fillMaxWidth()) {
                item.topStart?.let { topStart ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(topStart)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.weight(weight)
                    )
                }
                
                if (!item.topStart.isNullOrEmpty() && !item.topEnd.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                item.topEnd?.let { topEnd ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(topEnd)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.weight(weight)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Нижняя часть
            item.bottom?.let { bottom ->
                val (text, alignment, weight) = item.parseTextWithAlignment(bottom)
                AlignedText(
                    text = text,
                    alignment = alignment,
                    modifier = Modifier.fillMaxWidth().weight(weight)
                )
            }
        }
    }

    @Composable
    private fun RenderTopWithSplitBottom(item: CustomListItem) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Верхняя часть
            item.top?.let { top ->
                val (text, alignment, weight) = item.parseTextWithAlignment(top)
                AlignedText(
                    text = text,
                    alignment = alignment,
                    modifier = Modifier.fillMaxWidth().weight(weight)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Нижняя часть разделена
            Row(modifier = Modifier.fillMaxWidth()) {
                item.bottomStart?.let { bottomStart ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(bottomStart)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.weight(weight)
                    )
                }
                
                if (!item.bottomStart.isNullOrEmpty() && !item.bottomEnd.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                item.bottomEnd?.let { bottomEnd ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(bottomEnd)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.weight(weight)
                    )
                }
            }
        }
    }

    @Composable
    private fun RenderGrid2x2(item: CustomListItem) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Верхняя строка
            Row(modifier = Modifier.fillMaxWidth()) {
                item.topStart?.let { topStart ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(topStart)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.weight(weight)
                    )
                }
                
                if (!item.topStart.isNullOrEmpty() && !item.topEnd.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                item.topEnd?.let { topEnd ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(topEnd)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.weight(weight)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Нижняя строка
            Row(modifier = Modifier.fillMaxWidth()) {
                item.bottomStart?.let { bottomStart ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(bottomStart)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.weight(weight)
                    )
                }
                
                if (!item.bottomStart.isNullOrEmpty() && !item.bottomEnd.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                item.bottomEnd?.let { bottomEnd ->
                    val (text, alignment, weight) = item.parseTextWithAlignment(bottomEnd)
                    AlignedText(
                        text = text,
                        alignment = alignment,
                        modifier = Modifier.weight(weight)
                    )
                }
            }
        }
    }

    @Composable
    private fun AlignedText(
        text: String,
        alignment: CustomListAlignment,
        modifier: Modifier = Modifier
    ) {
        val textAlign = when (alignment.horizontal) {
            "Start" -> TextAlign.Start
            "Center" -> TextAlign.Center
            "End" -> TextAlign.End
            else -> TextAlign.Start
        }
        
        val boxAlignment = when ("${alignment.horizontal}_${alignment.vertical}") {
            "Start_Top" -> Alignment.TopStart
            "Center_Top" -> Alignment.TopCenter
            "End_Top" -> Alignment.TopEnd
            "Start_Center" -> Alignment.CenterStart
            "Center_Center" -> Alignment.Center
            "End_Center" -> Alignment.CenterEnd
            "Start_Bottom" -> Alignment.BottomStart
            "Center_Bottom" -> Alignment.BottomCenter
            "End_Bottom" -> Alignment.BottomEnd
            else -> Alignment.TopStart
        }

        Box(
            modifier = modifier,
            contentAlignment = boxAlignment
        ) {
            Text(
                text = HtmlUtils.htmlToAnnotatedString(text),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = textAlign
            )
        }
    }

    private fun formatErrorMessage(errorMessage: String?): String {
        if (errorMessage == null) return ""

        return errorMessage
            .replace("\n", ". ")
            .replace("..", ".")
            .replace(". .", ".")
    }
}