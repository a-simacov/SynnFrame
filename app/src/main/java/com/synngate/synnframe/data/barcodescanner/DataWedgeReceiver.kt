package com.synngate.synnframe.data.barcodescanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import timber.log.Timber

class DataWedgeReceiver : BroadcastReceiver() {
    companion object {
        // Используем стандартный action DataWedge
        const val ACTION_DATAWEDGE_SCAN = "com.symbol.datawedge.api.RESULT_ACTION"

        // Ключи для извлечения данных
        const val DATAWEDGE_INTENT_KEY_DATA = "com.symbol.datawedge.data_string"
        const val DATAWEDGE_INTENT_KEY_LABEL_TYPE = "com.symbol.datawedge.label_type"
        const val DATAWEDGE_INTENT_KEY_RESULT = "com.symbol.datawedge.decode_data"
        const val DATAWEDGE_INTENT_KEY_SOURCE = "com.symbol.datawedge.source"

        private val scanListeners = mutableListOf<ScanListener>()

        fun addListener(listener: ScanListener) {
            if (!scanListeners.contains(listener)) {
                scanListeners.add(listener)
                Timber.d("DataWedge scan listener added, total: ${scanListeners.size}")
            }
        }

        fun removeListener(listener: ScanListener) {
            scanListeners.remove(listener)
            Timber.d("DataWedge scan listener removed, remaining: ${scanListeners.size}")
        }
    }

    interface ScanListener {
        fun onScan(barcode: String, labelType: String)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DATAWEDGE_SCAN) {
            Timber.d("DataWedge scan received with action: ${intent.action}")

            // Вывод всех extras для отладки
            intent.extras?.let { bundle ->
                bundle.keySet().forEach { key ->
                    Timber.d("Intent extra: $key = ${bundle.get(key)}")
                }
            }

            // Проверяем источник сканирования (должен быть "scanner")
            val source = intent.getStringExtra(DATAWEDGE_INTENT_KEY_SOURCE)
            if (source != "scanner") {
                Timber.d("DataWedge scan from non-scanner source: $source")
                return
            }

            // Получаем данные сканирования
            // DataWedge может вернуть результат двумя способами:
            // 1. Напрямую в data_string
            // 2. В массиве decode_data для более сложных данных
            var barcode = intent.getStringExtra(DATAWEDGE_INTENT_KEY_DATA)
            var labelType = intent.getStringExtra(DATAWEDGE_INTENT_KEY_LABEL_TYPE)

            // Если основной способ не сработал, используем резервный
            if (barcode == null) {
                val decodeData = intent.getParcelableArrayListExtra<Bundle>(DATAWEDGE_INTENT_KEY_RESULT)
                if (decodeData != null && decodeData.isNotEmpty()) {
                    barcode = decodeData[0].getString("data")
                    labelType = decodeData[0].getString("symbology")
                }
            }

            if (barcode != null) {
                Timber.d("DataWedge scan processed: $barcode, type: $labelType")

                // Уведомляем всех слушателей
                scanListeners.forEach { it.onScan(barcode, labelType ?: "UNKNOWN") }
            } else {
                Timber.w("DataWedge received null barcode data")
            }
        }
    }
}