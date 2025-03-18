// Файл: com.synngate.synnframe.data.service.ClipboardServiceImpl.kt

package com.synngate.synnframe.data.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.synngate.synnframe.domain.service.ClipboardService
import timber.log.Timber

/**
 * Реализация сервиса для работы с буфером обмена
 */
class ClipboardServiceImpl(private val context: Context) : ClipboardService {

    override fun copyToClipboard(text: String, label: String): Boolean {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(label, text)
            clipboardManager.setPrimaryClip(clipData)
            true
        } catch (e: Exception) {
            Timber.e(e, "Error copying text to clipboard")
            false
        }
    }
}