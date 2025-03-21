package com.synngate.synnframe.util.network

import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Класс для реализации стратегии повторных попыток с экспоненциальной задержкой
 */
class RetryStrategy(
    private val maxAttempts: Int = 5,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 60000,
    private val factor: Double = 2.0,
    private val jitter: Double = 0.1
) {
    /**
     * Выполняет операцию с повторными попытками
     *
     * @param operation Операция, которую нужно выполнить
     * @param shouldRetry Функция, определяющая, нужно ли повторять попытку при данном исключении
     * @param onError Обработчик ошибок для каждой попытки
     * @param tag Тег для логирования
     * @return Результат операции или выбрасывает последнее исключение
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        shouldRetry: (Exception) -> Boolean = { true },
        onError: suspend (Exception, Int, Long) -> Unit = { _, _, _ -> },
        tag: String = "RetryStrategy"
    ): T {
        var currentDelay = initialDelayMs
        var attempt = 1
        var lastException: Exception? = null

        while (attempt <= maxAttempts) {
            try {
                // Выполнение операции
                return operation()
            } catch (e: Exception) {
                lastException = e

                // Проверяем, нужно ли повторять попытку
                if (!shouldRetry(e) || attempt == maxAttempts) {
                    Timber.w("$tag: Ошибка после $attempt попыток, больше не повторяем: ${e.message}")
                    throw e
                }

                // Вычисляем задержку с добавлением случайного отклонения (jitter)
                val jitterOffset = (Random.nextDouble(-jitter, jitter) * currentDelay)
                val delayWithJitter = (currentDelay + jitterOffset).toLong()

                // Логируем информацию о повторной попытке
                Timber.d("$tag: Попытка $attempt/$maxAttempts не удалась. Повтор через $delayWithJitter мс. Ошибка: ${e.message}")

                // Вызываем обработчик ошибок
                onError(e, attempt, delayWithJitter)

                // Задержка перед следующей попыткой
                delay(delayWithJitter)

                // Увеличиваем задержку для следующей попытки, но не более maxDelayMs
                currentDelay = min(currentDelay * factor.toLong(), maxDelayMs)
                attempt++
            }
        }

        // Этот код никогда не должен выполниться, так как в последней итерации
        // при неудаче мы выбрасываем исключение
        throw lastException ?: IllegalStateException("Неизвестная ошибка при выполнении операции с повторными попытками")
    }
}