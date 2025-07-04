package com.synngate.synnframe.presentation.common.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.data.barcodescanner.ScannerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerStatusIndicator(
    scannerService: ScannerService,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    showText: Boolean = true
) {
    val scannerState by scannerService.scannerState.collectAsState()
    val tooltipState = rememberTooltipState()

    val hasRealScanner = scannerService.hasRealScanner()

    val (statusColor, statusText, showError) = when {
        !hasRealScanner -> Triple(
            Color.Gray,
            "Scanner is unavailable",
            false
        )
        scannerState == ScannerState.Uninitialized -> Triple(
            Color.Gray,
            stringResource(R.string.scanner_uninitialized),
            false
        )
        scannerState == ScannerState.Initializing -> Triple(
            Color.Yellow,
            stringResource(R.string.scanner_initializing),
            false
        )
        scannerState == ScannerState.Initialized -> Triple(
            Color.Blue,
            stringResource(R.string.scanner_initialized),
            false
        )
        scannerState == ScannerState.Enabling -> Triple(
            Color.Yellow,
            stringResource(R.string.scanner_enabling),
            false
        )
        scannerState == ScannerState.Enabled -> Triple(
            Color.Green,
            stringResource(R.string.scanner_enabled),
            false
        )
        scannerState == ScannerState.Disabled -> Triple(
            Color.Red,
            stringResource(R.string.scanner_disabled),
            false
        )
        scannerState is ScannerState.Error -> Triple(
            Color.Red,
            (scannerState as ScannerState.Error).message,
            true
        )
        else -> Triple(
            Color.Gray,
            "Unlnown state",
            false
        )
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            if (scannerState is ScannerState.Error) {
                val errorMessage = (scannerState as ScannerState.Error).message
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(4.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(4.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        state = tooltipState,
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier.padding(2.dp)
            ) {
                if (showIcon) {
                    if (showError) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = statusText,
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = statusText,
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )

                if (showText) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    )
}