package com.synngate.synnframe.util.scanner

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Менеджер сканирования штрихкодов, обеспечивающий централизованное управление
 * функциями сканирования и валидации в приложении
 */
class BarcodeScannerManager(
    private val context: Context
) {
    // Кэш для избежания дублирования сканирования одних и тех же штрихкодов
    private val scannedBarcodeCache = ConcurrentHashMap<String, Long>()

    // Тайм-аут для предотвращения повторного сканирования (мс)
    private val DEBOUNCE_TIMEOUT = 1500L

    /**
     * Обработка сканированного штрихкода с дебаунсингом
     */
    fun processScannedBarcode(
        barcode: String,
        onValidBarcode: (String) -> Unit
    ) {
        if (barcode.isEmpty()) {
            return
        }

        val currentTime = System.currentTimeMillis()
        val lastScannedTime = scannedBarcodeCache[barcode]

        // Проверяем, был ли штрихкод недавно отсканирован
        if (lastScannedTime == null || (currentTime - lastScannedTime > DEBOUNCE_TIMEOUT)) {
            // Обновляем время последнего сканирования
            scannedBarcodeCache[barcode] = currentTime

            // Очистка старых записей кэша
            clearOldCacheEntries()

            // Вызываем обработчик валидного штрихкода
            onValidBarcode(barcode)
        } else {
            Timber.d("Barcode $barcode debounced. Last scan was ${currentTime - lastScannedTime}ms ago")
        }
    }

    /**
     * Очистка старых записей из кэша
     */
    private fun clearOldCacheEntries() {
        val currentTime = System.currentTimeMillis()
        scannedBarcodeCache.entries.removeIf { (_, timestamp) ->
            currentTime - timestamp > 30000 // Удаляем записи старше 30 секунд
        }
    }

    /**
     * Проверка, принадлежит ли штрихкод продукту
     */
    fun isProductBarcode(product: Product, barcode: String): Boolean {
        // Проверка основного штрихкода у единиц измерения продукта
        if (product.units.any { it.mainBarcode == barcode }) {
            return true
        }

        // Проверка дополнительных штрихкодов
        return product.units.any { unit ->
            unit.barcodes.contains(barcode)
        }
    }

    /**
     * Получение единицы измерения по штрихкоду
     */
    fun getUnitByBarcode(product: Product, barcode: String): ProductUnit? {
        // Сначала ищем по основному штрихкоду
        product.units.find { it.mainBarcode == barcode }?.let { return it }

        // Затем по дополнительным штрихкодам
        return product.units.find { unit ->
            unit.barcodes.contains(barcode)
        }
    }

    /**
     * Генерация изображения штрихкода по значению
     */
    fun generateBarcodeImage(
        barcodeValue: String,
        width: Int = 400,
        height: Int = 100,
        format: BarcodeFormat = BarcodeFormat.CODE_128
    ): android.graphics.Bitmap? {
        return try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(
                barcodeValue,
                format,
                width,
                height
            )
        } catch (e: Exception) {
            Timber.e(e, "Error generating barcode image")
            null
        }
    }
}

/**
 * Composable функция для создания менеджера сканирования штрихкодов
 * с учетом жизненного цикла
 */
@Composable
fun rememberBarcodeScannerManager(): BarcodeScannerManager {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val barcodeScannerManager = remember { BarcodeScannerManager(context) }

    // Очищаем кэш при уничтожении компонента
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                barcodeScannerManager.processScannedBarcode("", {})
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return barcodeScannerManager
}