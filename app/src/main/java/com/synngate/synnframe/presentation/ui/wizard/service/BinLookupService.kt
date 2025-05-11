package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.repository.WizardBinRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Сервис для поиска ячеек
 */
class BinLookupService(
    private val taskContextManager: TaskContextManager,
    private val wizardBinRepository: WizardBinRepository? = null
) : BaseBarcodeScanningService() {

    /**
     * Находит ячейку по штрих-коду
     * @param barcode штрих-код ячейки
     * @param onResult обработчик результата
     * @param onError обработчик ошибок
     */
    override suspend fun findItemByBarcode(
        barcode: String,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                // Сначала ищем в текущем задании
                val currentTask = taskContextManager.lastStartedTaskX.value
                if (currentTask != null) {
                    // Ищем среди ячеек в задании
                    val taskBins = currentTask.plannedActions.mapNotNull {
                        it.placementBin
                    }.distinctBy { it.code }

                    val matchingBin = taskBins.firstOrNull { it.code == barcode }
                    if (matchingBin != null) {
                        Timber.d("Ячейка найдена в задании: ${matchingBin.code}")
                        onResult(true, matchingBin)
                        return@withContext
                    }
                }

                // Затем пробуем найти через репозиторий, если он предоставлен
                if (wizardBinRepository != null) {
                    val bin = wizardBinRepository.getBinByCode(barcode)
                    if (bin != null) {
                        Timber.d("Ячейка найдена через репозиторий: ${bin.code}")
                        onResult(true, bin)
                        return@withContext
                    }
                }

                // Если не найдена, создаем временную
                val temporaryBin = createLocalBin(barcode)
                Timber.d("Создана временная ячейка: $barcode")
                onResult(true, temporaryBin)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске ячейки по коду: $barcode")
            onError("Ошибка при поиске ячейки: ${e.message}")
        }
    }

    /**
     * Поиск ячеек по текстовому запросу и фильтру зоны
     * @param query текстовый запрос для поиска
     * @param zoneFilter фильтр по зоне
     * @return список ячеек, соответствующих запросу
     */
    suspend fun searchBins(query: String, zoneFilter: String? = null): List<BinX> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<BinX>()

                // Сначала ищем в текущем задании
                val currentTask = taskContextManager.lastStartedTaskX.value
                if (currentTask != null) {
                    // Ищем среди ячеек в задании
                    val taskBins = currentTask.plannedActions.mapNotNull {
                        it.placementBin
                    }.distinctBy { it.code }

                    // Применяем фильтры, если они заданы
                    val filteredTaskBins = taskBins.filter { bin ->
                        (query.isEmpty() || bin.code.contains(query, ignoreCase = true)) &&
                                (zoneFilter == null || bin.zone == zoneFilter)
                    }

                    results.addAll(filteredTaskBins)
                }

                // Если репозиторий доступен и результатов нет или мало,
                // дополнительно используем репозиторий
                if (wizardBinRepository != null && results.size < 10) {
                    val repoResults = wizardBinRepository.getBins(query, zoneFilter)

                    // Добавляем только те ячейки, которых еще нет в результатах
                    val existingCodes = results.map { it.code }.toSet()
                    val newBins = repoResults.filter { it.code !in existingCodes }

                    results.addAll(newBins)
                }

                results
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске ячеек: query=$query, zone=$zoneFilter")
                emptyList()
            }
        }
    }

    /**
     * Создание локальной ячейки (без сохранения в репозиторий)
     */
    private fun createLocalBin(code: String): BinX {
        return BinX(
            code = code,
            zone = "Неизвестная зона"
        )
    }
}