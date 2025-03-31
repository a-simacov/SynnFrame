package com.synngate.synnframe.domain.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

interface UpdateInstaller {

    suspend fun initiateInstall(filePath: String): Result<Uri>
}

class UpdateInstallerImpl(
    private val context: Context,
    private val loggingService: LoggingService
) : UpdateInstaller {

    // Метод для проверки возможности установки пакета
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true // До Android 8.0 разрешение не требуется (только настройка "Неизвестные источники")
        }
    }

    // Метод для получения Intent для разрешения установки
    fun getInstallPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
    }

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

            // Проверка разрешения на установку из неизвестных источников
            if (!canInstallPackages()) {
                loggingService.logWarning("Необходимо разрешение на установку приложений")
                return Result.failure(Exception("INSTALL_PACKAGES permission required"))
            }

            loggingService.logInfo("Подготовка к установке обновления: $filePath")
            Result.success(uri)
        } catch (e: Exception) {
            Timber.e(e, "Error preparing update installation")
            loggingService.logError("Ошибка подготовки установки: ${e.message}")
            Result.failure(e)
        }
    }
}