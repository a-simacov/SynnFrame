package com.synngate.synnframe.presentation.common.dialog

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.synngate.synnframe.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Диалог для отображения QR-кода
 */
@Composable
fun QrCodeDialog(
    content: String,
    title: String = stringResource(id = R.string.qr_code_title),
    description: String = stringResource(id = R.string.qr_code_description),
    onDismiss: () -> Unit
) {
    // Состояние для хранения созданного битмапа QR-кода
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Определяем размер QR-кода в зависимости от плотности экрана
    val density = LocalDensity.current
    val qrCodeSize = with(density) { 250.dp.toPx().toInt() }

    // Генерируем QR-код в корутине
    LaunchedEffect(content) {
        bitmap = withContext(Dispatchers.Default) {
            generateQrCode(content, qrCodeSize)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Отображаем QR-код или индикатор загрузки
                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.generating_qr_code),
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.close))
                }
            }
        }
    }
}

/**
 * Генерирует Bitmap с QR-кодом из строки
 */
private fun generateQrCode(content: String, size: Int): Bitmap {
    val hints = mutableMapOf<EncodeHintType, Any>().apply {
        put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
        put(EncodeHintType.MARGIN, 2)
        put(EncodeHintType.CHARACTER_SET, "UTF-8")
    }

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.Black.hashCode() else Color.White.hashCode())
        }
    }

    return bitmap
}