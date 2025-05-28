package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber
import java.util.UUID

/**
 * Сервис для поиска объектов по штрихкодам
 */
class ObjectSearchService(
    private val productUseCases: ProductUseCases,
    private val validator: WizardValidator
) {
    // Переменные для отслеживания времени последнего сканирования
    private val MIN_SCAN_INTERVAL = 500L // Минимальный интервал между сканированиями (миллисекунды)
    private var lastScanTime = 0L        // Время последнего сканирования
    private var lastScannedBarcode = ""  // Последний отсканированный штрихкод

    /**
     * Обрабатывает штрих-код в контексте текущего шага
     * @return Тройка (успех операции, найденный объект или null, сообщение об ошибке или null)
     */
    suspend fun handleBarcode(state: ActionWizardState, barcode: String): Triple<Boolean, Any?, String?> {
        val currentTime = System.currentTimeMillis()

        // Проверяем, прошло ли достаточно времени с момента последнего сканирования
        // и не совпадает ли текущий штрихкод с предыдущим
        if (currentTime - lastScanTime < MIN_SCAN_INTERVAL && lastScannedBarcode == barcode) {
            Timber.d("Игнорирование повторного сканирования штрихкода: $barcode (слишком быстро)")
            return Triple(false, null, "Игнорирование повторного сканирования")
        }

        // Обновляем информацию о последнем сканировании
        lastScanTime = currentTime
        lastScannedBarcode = barcode

        val currentStep = state.getCurrentStep()
        val fieldType = currentStep?.factActionField

        if (barcode.isBlank()) {
            return Triple(false, null, "Пустой штрихкод")
        }

        if (fieldType == null) {
            Timber.w("Тип поля не определен для штрих-кода: $barcode")
            return Triple(false, null, "Не удалось определить тип поля для поиска")
        }

        Timber.d("Обработка штрих-кода: $barcode для поля типа: $fieldType")

        try {
            val result = when (fieldType) {
                FactActionField.STORAGE_BIN -> searchBin(state, barcode, true)
                FactActionField.ALLOCATION_BIN -> searchBin(state, barcode, false)
                FactActionField.STORAGE_PALLET -> searchPallet(state, barcode, true)
                FactActionField.ALLOCATION_PALLET -> searchPallet(state, barcode, false)
                FactActionField.STORAGE_PRODUCT -> searchTaskProduct(state, barcode)
                FactActionField.STORAGE_PRODUCT_CLASSIFIER -> searchProductClassifier(state, barcode)
                FactActionField.QUANTITY -> {
                    val parsedValue = barcode.toFloatOrNull()
                    if (parsedValue != null) {
                        Pair(parsedValue, null)
                    } else {
                        Pair(null, "Недопустимое значение количества: $barcode")
                    }
                }
                else -> Pair(null, "Неподдерживаемый тип поля: $fieldType")
            }

            return when {
                result.first != null -> Triple(true, result.first, null)
                result.second != null -> Triple(false, null, result.second)
                else -> Triple(false, null, "Не удалось найти объект по штрих-коду: $barcode")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")
            return Triple(false, null, "Ошибка при обработке штрих-кода: ${e.message}")
        }
    }

    /**
     * Ищет товар классификатора по штрих-коду
     * @return Пара (найденный объект или null, сообщение об ошибке или null)
     */
    private suspend fun searchProductClassifier(state: ActionWizardState, barcode: String): Pair<Any?, String?> {
        try {
            val currentStep = state.getCurrentStep() ?: return Pair(null, "Шаг не найден")
            val plannedProduct = state.plannedAction?.storageProductClassifier

            if (plannedProduct != null) {
                if (plannedProduct.id == barcode) {
                    Timber.d("Найден запланированный товар по ID: ${plannedProduct.id}")
                    return Pair(plannedProduct, null)
                }

                if (plannedProduct.articleNumber == barcode) {
                    Timber.d("Найден запланированный товар по артикулу: ${plannedProduct.articleNumber}")
                    return Pair(plannedProduct, null)
                }

                val foundByBarcode = plannedProduct.units.any { unit ->
                    unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                }

                if (foundByBarcode) {
                    Timber.d("Найден запланированный товар по штрих-коду: $barcode")
                    return Pair(plannedProduct, null)
                }
            }

            val product = productUseCases.findProductByBarcode(barcode)

            if (product != null) {
                val (isValid, errorMessage) = validator.validateFoundObject(state, product, currentStep)
                if (isValid) {
                    Timber.d("Найден товар по штрих-коду: $barcode")
                    return Pair(product, null)
                } else {
                    return Pair(null, errorMessage ?: "Найденный товар не соответствует плану")
                }
            }

            val productById = productUseCases.getProductById(barcode)

            if (productById != null) {
                val (isValid, errorMessage) = validator.validateFoundObject(state, productById, currentStep)
                if (isValid) {
                    Timber.d("Найден товар по ID: $barcode")
                    return Pair(productById, null)
                } else {
                    return Pair(null, errorMessage ?: "Найденный товар не соответствует плану")
                }
            }

            return Pair(null, "Товар не найден по штрихкоду или ID: $barcode")

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара классификатора: $barcode")
            return Pair(null, "Ошибка поиска: ${e.message}")
        }
    }

    /**
     * Ищет товар задания по штрих-коду
     * @return Пара (найденный объект или null, сообщение об ошибке или null)
     */
    private suspend fun searchTaskProduct(state: ActionWizardState, barcode: String): Pair<Any?, String?> {
        try {
            val currentStep = state.getCurrentStep() ?: return Pair(null, "Шаг не найден")
            val plannedTaskProduct = state.plannedAction?.storageProduct
            val plannedClassifierProduct = state.plannedAction?.storageProductClassifier

            if (plannedTaskProduct != null) {
                if (plannedTaskProduct.id == barcode) {
                    Timber.d("Найден запланированный товар задания по ID: ${plannedTaskProduct.id}")
                    return Pair(plannedTaskProduct, null)
                }

                if (plannedTaskProduct.product.id == barcode) {
                    Timber.d("Найден запланированный товар задания по ID товара: ${plannedTaskProduct.product.id}")
                    return Pair(plannedTaskProduct, null)
                }

                if (plannedTaskProduct.product.articleNumber == barcode) {
                    Timber.d("Найден запланированный товар задания по артикулу: ${plannedTaskProduct.product.articleNumber}")
                    return Pair(plannedTaskProduct, null)
                }

                val foundByBarcode = plannedTaskProduct.product.units.any { unit ->
                    unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                }

                if (foundByBarcode) {
                    Timber.d("Найден запланированный товар задания по штрих-коду: $barcode")
                    return Pair(plannedTaskProduct, null)
                }
            }

            // Особый случай: если нет запланированного товара задания, но есть товар классификатора
            // и текущий шаг поддерживает дополнительные свойства (срок годности, статус и т.д.)
            if (plannedTaskProduct == null && plannedClassifierProduct != null &&
                state.shouldShowAdditionalProps(currentStep)) {

                if (plannedClassifierProduct.id == barcode ||
                    plannedClassifierProduct.articleNumber == barcode ||
                    plannedClassifierProduct.units.any { unit ->
                        unit.barcodes.contains(barcode) || unit.mainBarcode == barcode
                    }) {

                    val taskProduct = state.getTaskProductFromClassifier(currentStep.id)

                    val (isValid, errorMessage) = validator.validateFoundObject(state, taskProduct, currentStep)
                    if (isValid) {
                        // Для товара с дополнительными свойствами не делаем автопереход,
                        // так как нужно заполнить дополнительные поля
                        Timber.d("Создан товар задания на основе товара классификатора: $barcode")
                        return Pair(taskProduct, null)
                    } else {
                        return Pair(null, errorMessage ?: "Найденный товар не соответствует плану")
                    }
                }
            }

            val product = productUseCases.findProductByBarcode(barcode)

            if (product != null) {
                val taskProduct = TaskProduct(
                    id = UUID.randomUUID().toString(),
                    product = product,
                    status = ProductStatus.STANDARD
                )

                val (isValid, errorMessage) = validator.validateFoundObject(state, taskProduct, currentStep)
                if (isValid) {
                    Timber.d("Найден товар по штрих-коду и создан товар задания: $barcode")
                    return Pair(taskProduct, null)
                } else {
                    return Pair(null, errorMessage ?: "Найденный товар не соответствует плану")
                }
            }

            return Pair(null, "Товар не найден по штрихкоду или ID: $barcode")

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске товара задания: $barcode")
            return Pair(null, "Ошибка поиска: ${e.message}")
        }
    }

    /**
     * Ищет ячейку по штрих-коду
     * @return Пара (найденный объект или null, сообщение об ошибке или null)
     */
    private suspend fun searchBin(state: ActionWizardState, barcode: String, isStorage: Boolean): Pair<Any?, String?> {
        try {
            val currentStep = state.getCurrentStep() ?: return Pair(null, "Шаг не найден")

            val plannedBin = if (isStorage) {
                state.plannedAction?.storageBin
            } else {
                state.plannedAction?.placementBin
            }

            if (plannedBin != null && plannedBin.code == barcode) {
                Timber.d("Найдена запланированная ячейка: ${plannedBin.code}")
                return Pair(plannedBin, null)
            }

            val bin = BinX(code = barcode, zone = "")

            val (isValid, errorMessage) = validator.validateFoundObject(state, bin, currentStep)
            if (isValid) {
                Timber.d("Ячейка $barcode прошла валидацию, устанавливаем объект")
                return Pair(bin, null)
            } else {
                Timber.d("Ячейка $barcode НЕ прошла валидацию")
                return Pair(null, errorMessage ?: "Введенная ячейка не соответствует плану")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске ячейки: $barcode")
            return Pair(null, "Ошибка поиска: ${e.message}")
        }
    }

    /**
     * Ищет паллету по штрих-коду
     * @return Пара (найденный объект или null, сообщение об ошибке или null)
     */
    private suspend fun searchPallet(state: ActionWizardState, barcode: String, isStorage: Boolean): Pair<Any?, String?> {
        try {
            val currentStep = state.getCurrentStep() ?: return Pair(null, "Шаг не найден")

            val plannedPallet = if (isStorage) {
                state.plannedAction?.storagePallet
            } else {
                state.plannedAction?.placementPallet
            }

            if (plannedPallet != null && plannedPallet.code == barcode) {
                Timber.d("Найдена запланированная паллета: ${plannedPallet.code}")
                return Pair(plannedPallet, null)
            }

            val pallet = Pallet(code = barcode)

            val (isValid, errorMessage) = validator.validateFoundObject(state, pallet, currentStep)
            if (isValid) {
                Timber.d("Паллета $barcode прошла валидацию, устанавливаем объект")
                return Pair(pallet, null)
            } else {
                Timber.d("Паллета $barcode НЕ прошла валидацию")
                return Pair(null, errorMessage ?: "Введенная паллета не соответствует плану")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске паллеты: $barcode")
            return Pair(null, "Ошибка поиска: ${e.message}")
        }
    }
}