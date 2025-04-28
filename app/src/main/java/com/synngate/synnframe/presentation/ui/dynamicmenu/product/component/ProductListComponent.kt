package com.synngate.synnframe.presentation.ui.dynamicmenu.product.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenComponent

class ProductListComponent<S>(
    private val state: S,
    private val products: List<DynamicProduct>,
    private val isLoading: Boolean,
    private val error: String?,
    private val onProductClick: (DynamicProduct) -> Unit
) : ScreenComponent {

    @Composable
    override fun Render(modifier: Modifier) {
        Box(modifier = modifier) {
            if (products.isEmpty() && !isLoading) {
                Text(
                    text = if (error == null) {
                        stringResource(id = R.string.no_products)
                    } else {
                        formatErrorMessage(error)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                ProductsList(
                    products = products,
                    onProductClick = onProductClick
                )
            }
        }
    }

    override fun usesWeight(): Boolean = true

    override fun getWeight(): Float = 1f

    @Composable
    private fun ProductsList(
        products: List<DynamicProduct>,
        onProductClick: (DynamicProduct) -> Unit,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(
                items = products,
                key = { it.id }
            ) { product ->
                ProductItem(
                    product = product,
                    onClick = { onProductClick(product) }
                )
            }
        }
    }

    @Composable
    private fun ProductItem(
        product: DynamicProduct,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.product_article, product.articleNumber),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    // Основная единица измерения
                    product.getMainUnit()?.let { unit ->
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = stringResource(id = R.string.product_main_unit, unit.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    private fun formatErrorMessage(errorMessage: String?): String {
        if (errorMessage == null) return ""

        return errorMessage
            .replace("\n", ". ")
            .replace("..", ".")
            .replace(". .", ".")
    }
}