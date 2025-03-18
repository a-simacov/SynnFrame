package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AuthResponseDto

/**
 * Интерфейс для работы с API аутентификации
 */
interface AuthApi {
    /**
     * Аутентификация пользователя
     *
     * @param password пароль пользователя
     * @param deviceInfo информация об устройстве
     * @return ответ с данными пользователя
     */
    suspend fun authenticate(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto>
}