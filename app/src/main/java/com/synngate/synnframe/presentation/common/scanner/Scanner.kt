package com.synngate.synnframe.presentation.common.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.inputs.QuantityTextField
import com.synngate.synnframe.util.scanner.BarcodeAnalyzer
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Компонент для сканирования штрихкодов с помощью ZXing
 */
@Composable
fun BarcodeScannerView(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { context ->
                    val previewView = PreviewView(context).apply {
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .apply {
                                setAnalyzer(
                                    cameraExecutor,
                                    BarcodeAnalyzer { barcode ->
                                        Timber.d("Barcode detected: $barcode")
                                        onBarcodeDetected(barcode)
                                    }
                                )
                            }

                        try {
                            // Очищаем привязки камеры перед новой привязкой
                            cameraProvider.unbindAll()

                            // Выбираем заднюю камеру
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            // Привязываем варианты использования к камере
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Use case binding failed")
                        }
                    }, ContextCompat.getMainExecutor(context))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Область наведения для штрихкода
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Необходимо разрешение на использование камеры для сканирования штрихкодов",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    productName: String? = null,
    modifier: Modifier = Modifier
) {
    // Добавляем состояние, которое предотвратит повторную обработку штрихкода
    var hasProcessedBarcode by remember { mutableStateOf(false) }

    // Используем DisposableEffect для закрытия диалога при размонтировании
    DisposableEffect(Unit) {
        onDispose {
            // Гарантируем закрытие диалога при размонтировании компонента
            if (!hasProcessedBarcode) {
                onClose()
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.scan_barcode),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                productName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                }

                // Важно! Область превью камеры должна занимать основное пространство
                Box(
                    modifier = Modifier
                        //.weight(0.7f)
                        .fillMaxWidth()
                ) {
                    // Используем BarcodeScannerView для отображения камеры и распознавания
                    BarcodeScannerView(
                        onBarcodeDetected = { barcode ->
                            if (!hasProcessedBarcode) {
                                hasProcessedBarcode = true
                                onBarcodeScanned(barcode)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Инструкция по сканированию
                Text(
                    text = stringResource(id = R.string.scan_barcode_instruction),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
//
//                Button(
//                    onClick = onClose,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(56.dp) // Фиксированная высота
//                        .padding(vertical = 4.dp), // Добавляем отступ
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.primary
//                    )
//                ) {
//                    Text(
//                        text = stringResource(id = R.string.close),
//                        style = MaterialTheme.typography.titleMedium
//                    )
//                }
            }
        }
    }
}

/**
 * Компонент для ввода количества товара
 */
@Composable
fun QuantityInputRow(
    currentQuantity: Float,
    onQuantityChanged: (Float) -> Unit
) {
    var additionalQuantity by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.task_scan_current_quantity, currentQuantity),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        QuantityTextField(
            value = additionalQuantity,
            onValueChange = { additionalQuantity = it },
            label = stringResource(id = R.string.task_scan_add_quantity),
            isError = isError,
            errorText = if (isError) "Неверное значение" else null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        val totalQuantity = try {
            currentQuantity + (additionalQuantity.toFloatOrNull() ?: 0f)
        } catch (e: Exception) {
            currentQuantity
        }

        Text(
            text = stringResource(id = R.string.task_scan_total_quantity, totalQuantity),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Button(
            onClick = {
                try {
                    val addValue = additionalQuantity.toFloatOrNull() ?: 0f
                    if (addValue != 0f) {
                        onQuantityChanged(currentQuantity + addValue)
                        additionalQuantity = "" // Сбрасываем поле после изменения
                        isError = false
                    } else {
                        isError = true
                    }
                } catch (e: Exception) {
                    isError = true
                }
            },
            enabled = additionalQuantity.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.task_scan_modify))
        }
    }
}