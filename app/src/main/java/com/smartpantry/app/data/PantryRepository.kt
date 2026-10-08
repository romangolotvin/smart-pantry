package com.smartpantry.app.data

import com.smartpantry.app.data.db.PantryDao
import com.smartpantry.app.data.model.PantryItem
import com.smartpantry.app.data.model.ScannedProduct
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PantryRepository(private val dao: PantryDao) {
    fun observeItems(): Flow<List<PantryItem>> = dao.observeAll()

    suspend fun addScanned(product: ScannedProduct, expiry: LocalDate, quantity: String = "1") {
        dao.insert(
            PantryItem(
                barcode = product.barcode,
                name = product.name,
                brand = product.brand,
                quantity = quantity,
                expiryDateEpochDay = expiry.toEpochDay(),
                imageHint = product.imageHint
            )
        )
    }

    suspend fun addManual(
        name: String,
        expiry: LocalDate,
        barcode: String = "",
        quantity: String = "1",
        imageHint: String = "🛒"
    ) {
        dao.insert(
            PantryItem(
                barcode = barcode.ifBlank { "manual-${System.currentTimeMillis()}" },
                name = name.trim(),
                quantity = quantity,
                expiryDateEpochDay = expiry.toEpochDay(),
                imageHint = imageHint
            )
        )
    }

    suspend fun delete(item: PantryItem) = dao.delete(item)
}
