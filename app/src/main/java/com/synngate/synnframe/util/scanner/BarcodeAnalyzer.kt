package com.synngate.synnframe.util.scanner

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraManager
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.EnumMap
import java.util.EnumSet

/**
 * Анализатор изображений с камеры для распознавания штрихкодов
 * с использованием библиотеки ZXing
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // Флаг, указывающий, обрабатывается ли в данный момент изображение
    private var isAnalyzing = false

    // Настройка формата штрихкодов для распознавания
    private val formatReader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        // Устанавливаем список форматов для распознавания
        val decodeFormats = EnumSet.of(
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.ITF,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX
        )
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
        hints[DecodeHintType.TRY_HARDER] = true
        setHints(hints)
    }

    /**
     * Обработка изображения с камеры и распознавание штрихкодов
     */
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        // Если уже идет анализ, пропускаем
        if (isAnalyzing) {
            imageProxy.close()
            return
        }

        isAnalyzing = true

        try {
            // Получаем изображение для анализа
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                // Получаем данные изображения с учетом поворота
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val imageData = mediaImage.toByteArray()

                // Создаем источник изображения для ZXing с учетом поворота
                val source = PlanarYUVLuminanceSource(
                    imageData,
                    mediaImage.width,
                    mediaImage.height,
                    0,
                    0,
                    mediaImage.width,
                    mediaImage.height,
                    false
                )

                // Создаем бинарное изображение для декодирования
                val bitmap = BinaryBitmap(HybridBinarizer(source))

                try {
                    // Пытаемся распознать штрихкод
                    val result = formatReader.decode(bitmap)

                    // Если штрихкод распознан, вызываем обработчик
                    if (result != null) {
                        Timber.d("Barcode detected: ${result.text}")
                        onBarcodeDetected(result.text)
                    }
                } catch (e: Exception) {
                    // Ошибка распознавания - не критично, просто логируем
                    // Timber.v("No barcode found in this frame: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error analyzing image")
        } finally {
            // Снимаем флаг обработки и закрываем изображение
            isAnalyzing = false
            imageProxy.close()
        }
    }

    /**
     * Преобразование Image в ByteArray с учетом ориентации изображения
     */
    private fun Image.toByteArray(): ByteArray {
        // Получаем данные из буфера Y-плоскости (YUV формат)
        val yBuffer = planes[0].buffer
        val ySize = yBuffer.remaining()

        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val data = ByteArray(ySize + uSize + vSize)

        // Копируем данные из буферов
        yBuffer.get(data, 0, ySize)
        uBuffer.get(data, ySize, uSize)
        vBuffer.get(data, ySize + uSize, vSize)

        return data
    }

    /**
     * Преобразование ByteBuffer в ByteArray
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    /**
     * Альтернативный метод преобразования Image в ByteArray через YuvImage
     * Может быть полезен для отладки или если основной метод даёт некорректные результаты
     */
    private fun Image.toByteArrayAlternative(): ByteArray {
        val yuvImage = YuvImage(
            toByteArray(),
            ImageFormat.NV21,
            width,
            height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        return out.toByteArray()
    }
}