package com.smartpantry.app.data.remote

import com.smartpantry.app.data.model.ScannedProduct

/**
 * Совместимость со старым вызовом: сначала world OFF, иначе null-friendly результат
 * через семейство баз (см. ProductLookupService).
 */
class OpenFoodFactsClient(
    private val delegate: OpenFactsFamilyClient = OpenFactsFamilyClient(
        host = "world.openfoodfacts.org",
        sourceLabel = "Open Food Facts"
    )
) {
    suspend fun lookup(barcode: String): ScannedProduct {
        return delegate.lookup(barcode) ?: ScannedProduct(
            barcode = barcode,
            name = "Продукт $barcode",
            brand = "",
            imageHint = "🛒",
            source = "Open Food Facts"
        )
    }

    suspend fun lookupOrNull(barcode: String): ScannedProduct? = delegate.lookup(barcode)
}
