package com.smartpantry.app.data

import com.smartpantry.app.data.model.ExpiryStatus
import com.smartpantry.app.data.model.PantryItem
import com.smartpantry.app.data.model.Recipe
import com.smartpantry.app.data.model.RecipeMatch
import java.time.LocalDate

object RecipeMatcher {
    fun match(
        recipes: List<Recipe>,
        pantry: List<PantryItem>,
        today: LocalDate = LocalDate.now()
    ): List<RecipeMatch> {
        val usableNames = pantry
            .filter { it.status(today) != ExpiryStatus.EXPIRED }
            .map { normalize(it.name) }
            .distinct()

        if (usableNames.isEmpty()) return emptyList()

        return recipes.mapNotNull { recipe ->
            val matched = mutableListOf<String>()
            val missing = mutableListOf<String>()

            recipe.ingredients.forEach { ingredient ->
                val needle = normalize(ingredient)
                val found = usableNames.any { pantryName ->
                    pantryName.contains(needle) || needle.contains(pantryName) ||
                        tokensOverlap(pantryName, needle)
                }
                if (found) matched += ingredient else missing += ingredient
            }

            if (matched.isEmpty()) return@mapNotNull null

            val percent = ((matched.size.toFloat() / recipe.ingredients.size) * 100f).toInt()
            RecipeMatch(
                recipe = recipe,
                matchedIngredients = matched,
                missingIngredients = missing,
                matchPercent = percent
            )
        }.sortedWith(
            compareByDescending<RecipeMatch> { it.matchPercent }
                .thenBy { it.missingIngredients.size }
                .thenBy { it.recipe.timeMinutes }
        )
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^a-zа-я0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun tokensOverlap(a: String, b: String): Boolean {
        val ta = a.split(" ").filter { it.length >= 3 }.toSet()
        val tb = b.split(" ").filter { it.length >= 3 }.toSet()
        return ta.intersect(tb).isNotEmpty()
    }
}
