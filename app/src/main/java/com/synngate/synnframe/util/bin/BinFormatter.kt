package com.synngate.synnframe.util.bin

import timber.log.Timber

class BinFormatter(private val pattern: String) {
    private val sectionsPattern = Regex("""\{([^:]+):@([^\}]+)\}""")

    /**
     * Форматирует код ячейки в читаемое имя.
     * Пример: A00131 -> A0-01-3-1
     * @param code Код ячейки
     * @return Отформатированное имя ячейки
     */
    fun formatBinName(code: String): String {
        try {
            val sections = sectionsPattern.findAll(pattern)
                .map { it.groupValues[1] to it.groupValues[2] }
                .toList()

            if (sections.isEmpty()) {
                return code // Если шаблон не содержит секций, возвращаем код как есть
            }

            // Пример: {Aisle:@[a-zA-Z][0-9]}{Rack:@[0-9]{2}}{Shelf:@[1-9]{1}}{Position:@[1-9]}
            // Для кода A00131:
            // - Aisle: A0 (индексы 0-1)
            // - Rack: 01 (индексы 2-3)
            // - Shelf: 3 (индекс 4)
            // - Position: 1 (индекс 5)

            var currentIndex = 0
            val parts = mutableListOf<String>()

            for (section in sections) {
                val name = section.first
                val pattern = section.second

                // Определяем длину секции на основе шаблона
                val sectionLength = extractSectionLength(pattern)

                if (currentIndex + sectionLength <= code.length) {
                    val sectionValue = code.substring(currentIndex, currentIndex + sectionLength)
                    parts.add(sectionValue)
                    currentIndex += sectionLength
                }
            }

            // Соединяем части через дефис
            return parts.joinToString("-")
        } catch (e: Exception) {
            Timber.e(e, "Error formatting bin name for code: $code")
            return code // В случае ошибки возвращаем исходный код
        }
    }

    /**
     * Извлекает длину секции из шаблона.
     * Например, для [a-zA-Z][0-9] длина будет 2,
     * для [0-9]{2} длина будет 2,
     * для [1-9] длина будет 1.
     */
    private fun extractSectionLength(pattern: String): Int {
        // Очень упрощенная реализация
        val quantifierPattern = Regex("""\{(\d+)\}""")
        val quantifier = quantifierPattern.find(pattern)

        return if (quantifier != null) {
            quantifier.groupValues[1].toInt()
        } else {
            // Если нет явного указания количества, считаем каждый [] за 1 символ
            pattern.count { it == '[' }
        }
    }
}