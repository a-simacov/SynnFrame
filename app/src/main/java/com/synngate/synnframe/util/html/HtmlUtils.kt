package com.synngate.synnframe.util.html

import android.text.Html
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import timber.log.Timber

/**
 * Утилитный класс для работы с HTML-форматированием
 */
object HtmlUtils {

    /**
     * Преобразует HTML-строку в AnnotatedString с обработкой ошибок
     * @param html HTML-строка для преобразования
     * @return AnnotatedString с примененным форматированием или исходную строку в случае ошибки
     */
    @Composable
    fun htmlToAnnotatedString(html: String): AnnotatedString {
        return remember(html) {
            try {
                if (containsHtmlTags(html)) {
                    AnnotatedString.fromHtml(html)
                } else {
                    // Если нет HTML-тегов, возвращаем обычную строку
                    AnnotatedString(html)
                }
            } catch (e: Exception) {
                // В случае ошибки логируем и возвращаем простую строку
                Timber.e(e, "Error converting HTML to AnnotatedString: $html")
                AnnotatedString(html)
            }
        }
    }

    /**
     * Проверяет, содержит ли строка HTML-теги
     * @param text Строка для проверки
     * @return true, если строка содержит HTML-теги
     */
    fun containsHtmlTags(text: String): Boolean {
        // Простая проверка наличия открывающего и закрывающего HTML-тега
        return text.contains(Regex("<[^>]+>.*?</[^>]+>")) ||
                text.contains(Regex("<[^>]+/>")) ||
                text.contains(Regex("<br>|<p>|<div>"))
    }

    /**
     * Удаляет HTML-теги из строки, оставляя только текст
     * @param html HTML-строка для очистки
     * @return Текст без HTML-тегов
     */
    fun stripHtml(html: String): String {
        return try {
            // Используем стандартный метод Android для очистки HTML
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } catch (e: Exception) {
            Timber.e(e, "Error stripping HTML from: $html")
            html.replace(Regex("<[^>]*>"), "")
        }
    }

    /**
     * Преобразует простой текст в HTML с базовым форматированием
     * @param text Текст для форматирования
     * @param isHighlighted Нужно ли выделить текст
     * @param color Цвет текста в формате HEX или именованный цвет CSS
     * @param isBold Нужно ли сделать текст жирным
     * @param isItalic Нужно ли сделать текст курсивным
     * @return Отформатированный HTML-текст
     */
    fun formatTextAsHtml(
        text: String,
        isHighlighted: Boolean = false,
        color: String? = null,
        isBold: Boolean = false,
        isItalic: Boolean = false
    ): String {
        var result = text

        if (isBold) {
            result = "<b>$result</b>"
        }

        if (isItalic) {
            result = "<i>$result</i>"
        }

        if (color != null) {
            result = "<font color='$color'>$result</font>"
        }

        if (isHighlighted) {
            result = "<span style='background-color:yellow;'>$result</span>"
        }

        return result
    }
}