package com.synngate.synnframe.presentation.ui.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.service.ActionDataCacheService
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

/**
 * ViewModel для доступа к данным, используемым в визарде действий
 */
class ActionDataViewModel(
    private val actionDataCacheService: ActionDataCacheService
) : BaseViewModel<Unit, Unit>(Unit) {

    // Проксируем потоки данных из сервиса кэша
    val products = actionDataCacheService.products
    val bins = actionDataCacheService.bins
    val pallets = actionDataCacheService.pallets

    // Методы для загрузки данных
    fun loadProducts(query: String? = null, planProductIds: Set<String>? = null) {
        launchIO {
            try {
                actionDataCacheService.loadProducts(query, planProductIds)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке продуктов")
            }
        }
    }

    fun loadBins(query: String? = null, zone: String? = null) {
        launchIO {
            try {
                actionDataCacheService.loadBins(query, zone)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке ячеек")
            }
        }
    }

    fun loadPallets(query: String? = null) {
        launchIO {
            try {
                actionDataCacheService.loadPallets(query)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке паллет")
            }
        }
    }

    // Методы для поиска данных
    fun findProductByBarcode(barcode: String, onResult: (Product?) -> Unit) {
        launchIO {
            try {
                val product = actionDataCacheService.findProductByBarcode(barcode)
                launchMain {
                    onResult(product)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске продукта по штрихкоду")
                launchMain {
                    onResult(null)
                }
            }
        }
    }

    fun findBinByCode(code: String, onResult: (BinX?) -> Unit) {
        launchIO {
            try {
                val bin = actionDataCacheService.findBinByCode(code)
                launchMain {
                    onResult(bin)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске ячейки по коду")
                launchMain {
                    onResult(null)
                }
            }
        }
    }

    fun findPalletByCode(code: String, onResult: (Pallet?) -> Unit) {
        launchIO {
            try {
                val pallet = actionDataCacheService.findPalletByCode(code)
                launchMain {
                    onResult(pallet)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске паллеты по коду")
                launchMain {
                    onResult(null)
                }
            }
        }
    }

    // Методы для работы с паллетами
    fun createPallet(onResult: (Result<Pallet>) -> Unit) {
        launchIO {
            try {
                val result = actionDataCacheService.createPallet()
                launchMain {
                    onResult(result)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при создании паллеты")
                launchMain {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    fun closePallet(code: String, onResult: (Result<Boolean>) -> Unit) {
        launchIO {
            try {
                val result = actionDataCacheService.closePallet(code)
                launchMain {
                    onResult(result)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при закрытии паллеты")
                launchMain {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    fun printPalletLabel(code: String, onResult: (Result<Boolean>) -> Unit) {
        launchIO {
            try {
                val result = actionDataCacheService.printPalletLabel(code)
                launchMain {
                    onResult(result)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при печати этикетки паллеты")
                launchMain {
                    onResult(Result.failure(e))
                }
            }
        }
    }

    // Методы для очистки кэша
    fun clearCache() {
        launchIO {
            actionDataCacheService.clearCache()
        }
    }
}