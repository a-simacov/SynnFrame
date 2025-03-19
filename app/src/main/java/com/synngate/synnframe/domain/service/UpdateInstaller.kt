package com.synngate.synnframe.domain.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

/**
 * Интерфейс для установки обновлений приложения
 */
interface UpdateInstaller {
    /**
     * Инициирует установку обновления из файла
     * @param filePath Путь к файлу APK
     * @return Result с Uri для Intent установки или ошибкой
     */
    suspend fun initiateInstall(filePath: String): Result<Uri>
}

/**
 * Реализация установщика обновлений с использованием FileProvider
 */
class UpdateInstallerImpl(
    private val context: Context,
    private val loggingService: LoggingService
) : UpdateInstaller {

    override suspend fun initiateInstall(filePath: String): Result<Uri> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                loggingService.logError("Файл обновления не найден: $filePath")
                return Result.failure(Exception("Файл обновления не найден"))
            }

            // Получаем URI через FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            loggingService.logInfo("Подготовка к установке обновления: $filePath")
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Error preparing update installation")
            loggingService.logError("Ошибка подготовки установки: ${e.message}")
            Result.failure(e)
        }
    }
}