package com.synngate.synnframe.domain.service

// FileService.kt
interface FileService {
    /**
     * Сохраняет данные в файл
     * @param filename Имя файла (или путь с именем файла)
     * @param data Массив байтов для сохранения
     * @return Путь к сохраненному файлу или null в случае ошибки
     */
    suspend fun saveFile(filename: String, data: ByteArray): String?

    /**
     * Проверяет наличие свободного места на диске
     * @param requiredBytes Требуемое количество байт
     * @return true, если места достаточно, иначе false
     */
    fun hasEnoughStorage(requiredBytes: Long): Boolean

    /**
     * Создает директорию при необходимости
     * @param dirPath Путь к директории
     * @return true, если директория создана или уже существовала
     */
    fun ensureDirectoryExists(dirPath: String): Boolean
}