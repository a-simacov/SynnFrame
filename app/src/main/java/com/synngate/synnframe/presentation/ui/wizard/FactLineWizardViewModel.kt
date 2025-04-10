package com.synngate.synnframe.presentation.ui.wizard

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.usecase.wizard.FactLineWizardUseCases
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class FactLineWizardViewModel(
    private val factLineWizardUseCases: FactLineWizardUseCases
) : BaseViewModel<Unit, Unit>(Unit) {

    // Проксируем потоки данных из UseCase
    val products = factLineWizardUseCases.getProductsFlow()
    val bins = factLineWizardUseCases.getBinsFlow()
    val pallets = factLineWizardUseCases.getPalletsFlow()

    // Методы для загрузки данных
    fun loadProducts(query: String? = null, planProductIds: Set<String>? = null) {
        launchIO {
            try {
                factLineWizardUseCases.loadProducts(query, planProductIds)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке продуктов")
            }
        }
    }

    fun loadBins(query: String? = null, zone: String? = null) {
        launchIO {
            try {
                factLineWizardUseCases.loadBins(query, zone)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке ячеек")
            }
        }
    }

    fun loadPallets(query: String? = null) {
        launchIO {
            try {
                factLineWizardUseCases.loadPallets(query)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке паллет")
            }
        }
    }

    // Методы для поиска данных
    fun findProductByBarcode(barcode: String, onResult: (Product?) -> Unit) {
        launchIO {
            try {
                val product = factLineWizardUseCases.findProductByBarcode(barcode)
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
                val bin = factLineWizardUseCases.findBinByCode(code)
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
                val pallet = factLineWizardUseCases.findPalletByCode(code)
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
                val result = factLineWizardUseCases.createPallet()
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
                val result = factLineWizardUseCases.closePallet(code)
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
                val result = factLineWizardUseCases.printPalletLabel(code)
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
            factLineWizardUseCases.clearCache()
        }
    }
}