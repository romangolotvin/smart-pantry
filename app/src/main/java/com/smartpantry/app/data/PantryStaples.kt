package com.smartpantry.app.data

/**
 * Базовые продукты, которые считаются всегда доступными для подбора рецептов.
 * В тексте рецепта они всё равно указываются.
 */
object PantryStaples {
    fun isAlwaysAvailable(ingredient: String): Boolean {
        val n = IngredientNames.baseName(ingredient)
        return when {
            n == "соль" || n.startsWith("соль ") -> true
            n == "сахар" || n.startsWith("сахар ") -> true
            "растительное масло" in n -> true
            "оливковое масло" in n -> true
            "сливочное масло" in n || "масло сливочное" in n -> true
            else -> false
        }
    }
}

object IngredientNames {
    fun normalize(value: String): String =
        value.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^a-zа-я0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Название без граммовки: «спагетти 200 г» → «спагетти». */
    fun baseName(value: String): String =
        normalize(value)
            .replace(
                Regex(
                    """\d+([.,]\d+)?\s*(г|кг|мл|л|шт|ст\s*л|ч\s*л|ст\.?\s*л\.?|ч\.?\s*л\.?)?"""
                ),
                " "
            )
            .replace(Regex("\\s+"), " ")
            .trim()
}
