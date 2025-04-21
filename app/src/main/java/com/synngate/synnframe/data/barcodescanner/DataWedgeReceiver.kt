package com.synngate.synnframe.data.barcodescanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import timber.log.Timber

class DataWedgeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DATAWEDGE_SCAN = "com.symbol.datawedge.api.RESULT_ACTION"

        const val DATAWEDGE_INTENT_KEY_DATA = "com.symbol.datawedge.data_string"
        const val DATAWEDGE_INTENT_KEY_LABEL_TYPE = "com.symbol.datawedge.label_type"
        const val DATAWEDGE_INTENT_KEY_RESULT = "com.symbol.datawedge.decode_data"
        const val DATAWEDGE_INTENT_KEY_SOURCE = "com.symbol.datawedge.source"

        private val scanListeners = mutableListOf<ScanListener>()

        fun addListener(listener: ScanListener) {
            if (!scanListeners.contains(listener)) {
                scanListeners.add(listener)
            }
        }

        fun removeListener(listener: ScanListener) {
            scanListeners.remove(listener)
        }
    }

    interface ScanListener {
        fun onScan(barcode: String, labelType: String)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DATAWEDGE_SCAN) {
            val source = intent.getStringExtra(DATAWEDGE_INTENT_KEY_SOURCE)
            if (source != "scanner") {
                return
            }

            var barcode = intent.getStringExtra(DATAWEDGE_INTENT_KEY_DATA)
            var labelType = intent.getStringExtra(DATAWEDGE_INTENT_KEY_LABEL_TYPE)

            if (barcode == null) {
                val decodeData = intent.getParcelableArrayListExtra<Bundle>(DATAWEDGE_INTENT_KEY_RESULT)
                if (decodeData != null && decodeData.isNotEmpty()) {
                    barcode = decodeData[0].getString("data")
                    labelType = decodeData[0].getString("symbology")
                }
            }

            if (barcode != null) {
                scanListeners.forEach { it.onScan(barcode, labelType ?: "UNKNOWN") }
            } else {
                Timber.w("DataWedge received null barcode data")
            }
        }
    }
}