package com.synngate.synnframe.presentation.common.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.BuildConfig
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scanner.ScannerStatusIndicator
import com.synngate.synnframe.presentation.theme.SynnFrameTheme

/**
 * Компонент для отображения нижней панели приложения с информацией
 * о пользователе, версии и состоянии сканера.
 *
 * @param userName имя текущего пользователя или null, если пользователь не авторизован
 * @param isSyncing флаг, указывающий на активный процесс синхронизации
 * @param scannerService сервис сканера или null, если сканер не используется на экране
 * @param modifier модификатор для настройки внешнего вида
 */
@Composable
fun BottomInfoPanel(
    userName: String? = null,
    isSyncing: Boolean = false,
    scannerService: ScannerService? = null,
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .navigationBarsPadding()
        ) {
            bottomBar()

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Блок информации о пользователе (слева)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (!userName.isNullOrEmpty()) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Версия приложения (центр)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        modifier = Modifier.alpha(0.8f)
                    )
                }

                // Блок со сканером и прогресс-баром (справа)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.wrapContentWidth(Alignment.End)
                    ) {
                        // Если сканер используется на экране, отображаем его статус
                        scannerService?.let { scanner ->
                            // Разделитель (вертикальная линия)
                            SectionDivider()

                            // Тип сканера в виде аббревиатуры
                            val scannerType = getScannerTypeAbbreviation(scanner)
                            Text(
                                text = scannerType,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )

                            // Индикатор статуса сканера
                            ScannerStatusIndicator(
                                scannerService = scanner,
                                showText = false, // Не показываем текст для компактности
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }

                        // Прогресс-бар синхронизации
                        if (isSyncing) {
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.width(60.dp) // Ограничиваем ширину прогресс-бара
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Вертикальный разделитель для секций в нижней панели
 */
@Composable
private fun SectionDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(width = 1.dp, height = 16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.width(1.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

/**
 * Возвращает аббревиатуру типа сканера
 */
@Composable
private fun getScannerTypeAbbreviation(scannerService: ScannerService): String {
    // Используем текущий тип сканера из scannerService
    return if (scannerService.isCameraScanner()) {
        "CAM" // Камера
    } else if (scannerService.hasRealScanner()) {
        // Для простоты определяем тип на основе проверки API Zebra DataWedge
        // Для полной реализации может потребоваться дополнительная логика
        "ZBR" // Предполагаем, что если есть реальный сканер и это не камера, то это Zebra
    } else {
        "STD" // Стандартный режим
    }
}

@Preview(apiLevel = 34, showBackground = true)
@Composable
private fun D() {
    SynnFrameTheme {
        BottomInfoPanel(
            userName = "admin",
            scannerService = LocalScannerService.current
        )
    }
}