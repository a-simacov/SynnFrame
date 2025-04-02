package com.synngate.synnframe.util.scanner

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
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

    //todo добавить приватное поле для продолжительного распознавание ШК.

    // Флаг, указывающий, обрабатывается ли в данный момент изображение
    private var isAnalyzing = false

    // Флаг, указывающий, что штрихкод уже был распознан
    private var isBarcodeDetected = false

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
        if (isAnalyzing || isBarcodeDetected) {
            imageProxy.close()
            return
        }

        isAnalyzing = true

        try {
            // Получаем изображение для анализа
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                // Получаем данные изображения
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val imageData = mediaImage.toByteArray()

                // Определяем размеры изображения
                val width = mediaImage.width
                val height = mediaImage.height

                // Создаем hints с поддерживаемыми форматами штрихкодов
                val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
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

                // Сначала пробуем распознать в исходной ориентации
                val result = tryDecodeWithOrientation(imageData, width, height, rotationDegrees, hints)
                if (result != null) {
                    Timber.d("Barcode detected: ${result.text}")
                    isBarcodeDetected = true
                    onBarcodeDetected(result.text)
                    return
                }

                // Если не удалось, пробуем другие ориентации
                for (rotation in listOf(90, 180, 270)) {
                    val adjustedRotation = (rotationDegrees + rotation) % 360
                    val rotatedResult = tryDecodeWithOrientation(imageData, width, height, adjustedRotation, hints)
                    if (rotatedResult != null) {
                        Timber.d("Barcode detected with rotation $rotation: ${rotatedResult.text}")
                        onBarcodeDetected(rotatedResult.text)
                        return
                    }
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

    fun reset() {
        isBarcodeDetected = false
    }

    // Функция для попытки декодирования с учетом ориентации
    private fun tryDecodeWithOrientation(
        imageData: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        hints: Map<DecodeHintType, *>
    ): com.google.zxing.Result? {  // Используем полное имя класса
        try {
            val rotatedData: ByteArray
            val finalWidth: Int
            val finalHeight: Int

            // Применяем поворот данных, если необходимо
            when (rotationDegrees) {
                90 -> {
                    rotatedData = rotateYUV90(imageData, width, height)
                    finalWidth = height
                    finalHeight = width
                }
                180 -> {
                    rotatedData = rotateYUV180(imageData, width, height)
                    finalWidth = width
                    finalHeight = height
                }
                270 -> {
                    rotatedData = rotateYUV270(imageData, width, height)
                    finalWidth = height
                    finalHeight = width
                }
                else -> {
                    rotatedData = imageData
                    finalWidth = width
                    finalHeight = height
                }
            }

            // Создаем источник изображения для ZXing
            val source = PlanarYUVLuminanceSource(
                rotatedData,
                finalWidth,
                finalHeight,
                0,
                0,
                finalWidth,
                finalHeight,
                false
            )

            // Создаем бинарное изображение и декодируем
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            return formatReader.decode(bitmap, hints)
        } catch (e: Exception) {
            // Игнорируем ошибку и возвращаем null
            return null
        }
    }

    // Функции поворота YUV данных остаются без изменений
    private fun rotateYUV90(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotatedData = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                rotatedData[x * height + (height - y - 1)] = data[y * width + x]
            }
        }
        return rotatedData
    }

    private fun rotateYUV180(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotatedData = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                rotatedData[(height - y - 1) * width + (width - x - 1)] = data[y * width + x]
            }
        }
        return rotatedData
    }

    private fun rotateYUV270(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotatedData = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                rotatedData[(width - x - 1) * height + y] = data[y * width + x]
            }
        }
        return rotatedData
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