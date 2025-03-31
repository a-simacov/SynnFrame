package com.synngate.synnframe.domain.service

interface FileService {

    suspend fun saveFile(filename: String, data: ByteArray): String?

    fun hasEnoughStorage(requiredBytes: Long): Boolean

    fun ensureDirectoryExists(dirPath: String): Boolean
}