package com.synngate.synnframe.presentation.ui.wizard.action.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.SavableObject

@Composable
fun SavableObjectsPanel(
    savableObjects: List<SavableObject>,
    onRemoveObject: (String) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    if (!visible || savableObjects.isEmpty()) return

    val scrollState = rememberScrollState()

    AnimatedVisibility(
        visible = visible && savableObjects.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(4.dp)
            ) {
                savableObjects.forEach { savableObject ->
                    SavableObjectChip(
                        savableObject = savableObject,
                        onRemove = onRemoveObject,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}