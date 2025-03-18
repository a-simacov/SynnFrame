package com.synngate.synnframe.util.logging

import android.util.Log
import timber.log.Timber

/**
 * Timber Tree для релизной версии приложения.
 * Фильтрует логи низких уровней и сохраняет важные сообщения в базу данных.
 */
class ReleaseTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // Позволяем логировать только INFO, WARNING и ERROR сообщения
        return priority >= Log.INFO
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (isLoggable(tag, priority)) {
            // Вывод в логкат и сохранение в локальной базе данных
            // В будущем здесь будет реализация для сохранения в базу данных Room
            when (priority) {
                Log.INFO -> Log.i(tag, message, t)
                Log.WARN -> Log.w(tag, message, t)
                Log.ERROR -> Log.e(tag, message, t)
            }

            // TODO: Сохранение логов в базу данных через репозиторий
            // Будет реализовано позже, когда будет готова база данных
            // logRepository.saveLog(priority, message, t?.message ?: "")
        }
    }
}