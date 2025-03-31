package com.synngate.synnframe.data.service

import android.content.Context
import com.synngate.synnframe.domain.service.FileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.os.StatFs
import timber.log.Timber

class FileServiceImpl(private val context: Context) : FileService {
    override suspend fun saveFile(filename: String, data: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "updates")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val file = File(dir, filename)

            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(data)
                    outputStream.flush()
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to save file: $filename")
            null
        }
    }

    override fun hasEnoughStorage(requiredBytes: Long): Boolean {
        try {
            val stat = StatFs(context.filesDir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            return availableBytes >= requiredBytes
        } catch (e: Exception) {
            Timber.e(e, "Failed to check storage")
            return false
        }
    }

    override fun ensureDirectoryExists(dirPath: String): Boolean {
        val dir = File(context.filesDir, dirPath)
        return dir.exists() || dir.mkdirs()
    }
}