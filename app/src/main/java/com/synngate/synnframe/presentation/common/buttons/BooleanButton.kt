package com.synngate.synnframe.presentation.common.buttons

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R

@Composable
fun BooleanButton(
    currentValue: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    valueToString: @Composable (Boolean) -> String = { stringResource(id = if (it) R.string.yes else R.string.no) },
    labelText: String = ""
) {
    if (labelText.isNotBlank())
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

    CarouselValueButton(
        values = listOf(false, true),
        currentValue = currentValue,
        onValueChange = onValueChange,
        valueToString = valueToString,
        modifier = modifier
    )
}