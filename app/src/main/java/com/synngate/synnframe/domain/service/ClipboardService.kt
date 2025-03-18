// Файл: com.synngate.synnframe.domain.service.ClipboardService.kt

package com.synngate.synnframe.domain.service

/**
 * Сервис для работы с буфером обмена
 */
interface ClipboardService {
    /**
     * Копирование текста в буфер обмена
     *
     * @param text Текст для копирования
     * @param label Метка для операции копирования
     * @return true если копирование успешно, false в противном случае
     */
    fun copyToClipboard(text: String, label: String = ""): Boolean
}