package com.smartpantry.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val timeMinutes: Int,
    val difficulty: String,
    val emoji: String,
    val tags: List<String>,
    val ingredients: List<String>,
    val steps: List<String>,
    val accentColor: Long
)

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val brand: String = "",
    val quantity: String = "1",
    val expiryDateEpochDay: Long,
    val addedAtMillis: Long = System.currentTimeMillis(),
    val imageHint: String = "🛒"
) {
    fun expiryDate(): LocalDate = LocalDate.ofEpochDay(expiryDateEpochDay)

    fun daysUntilExpiry(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, expiryDate())

    fun status(today: LocalDate = LocalDate.now()): ExpiryStatus {
        val days = daysUntilExpiry(today)
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= 2 -> ExpiryStatus.SOON
            days <= 7 -> ExpiryStatus.WATCH
            else -> ExpiryStatus.FRESH
        }
    }
}

enum class ExpiryStatus {
    FRESH, WATCH, SOON, EXPIRED
}

data class RecipeMatch(
    val recipe: Recipe,
    val matchedIngredients: List<String>,
    val missingIngredients: List<String>,
    val matchPercent: Int
)

data class ScannedProduct(
    val barcode: String,
    val name: String,
    val brand: String,
    val imageHint: String = "🛒",
    val source: String = "",
    val suggestedExpiry: LocalDate? = null,
    val statusLabel: String = "",
    val markingCode: String = ""
)
