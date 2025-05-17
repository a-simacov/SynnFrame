package com.synngate.synnframe.presentation.di.modules.feature

import com.synngate.synnframe.presentation.di.AppContainer
import com.synngate.synnframe.presentation.di.modules.CoreContainer
import com.synngate.synnframe.presentation.di.modules.DataContainer
import com.synngate.synnframe.presentation.di.modules.DomainContainer
import com.synngate.synnframe.presentation.di.modules.FeatureContainer
import com.synngate.synnframe.presentation.ui.products.ProductDetailViewModel
import com.synngate.synnframe.presentation.ui.products.ProductListViewModel
import timber.log.Timber

/**
 * Контейнер для функциональности работы с продуктами.
 * Содержит зависимости и фабрики для ViewModel-ей, связанных с продуктами.
 */
class ProductsFeatureContainer(
    appContainer: AppContainer,
    coreContainer: CoreContainer,
    domainContainer: DomainContainer,
    private val dataContainer: DataContainer
) : FeatureContainer(appContainer, coreContainer, domainContainer) {

    override val moduleName: String = "Products"

    /**
     * Создание ViewModel для экрана списка продуктов
     *
     * @param isSelectionMode Флаг режима выбора продукта
     * @return ViewModel для списка продуктов
     */
    fun createProductListViewModel(isSelectionMode: Boolean): ProductListViewModel {
        return getViewModel("ProductListViewModel_${if (isSelectionMode) "selection" else "normal"}") {
            ProductListViewModel(
                productUseCases = domainContainer.productUseCases,
                soundService = coreContainer.soundService,
                synchronizationController = domainContainer.synchronizationController,
                productUiMapper = dataContainer.productUiMapper,
                resourceProvider = coreContainer.resourceProvider,
                isSelectionMode = isSelectionMode
            )
        }
    }

    /**
     * Создание ViewModel для экрана деталей продукта
     *
     * @param productId Идентификатор продукта
     * @return ViewModel для деталей продукта
     */
    fun createProductDetailViewModel(productId: String): ProductDetailViewModel {
        return getViewModel("ProductDetailViewModel_$productId") {
            ProductDetailViewModel(
                productId = productId,
                productUseCases = domainContainer.productUseCases,
                clipboardService = coreContainer.clipboardService,
                productUiMapper = dataContainer.productUiMapper,
                resourceProvider = coreContainer.resourceProvider
            )
        }
    }

    override fun initialize() {
        super.initialize()
        Timber.d("Products feature initialized")
    }
}