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
            .map { IngredientNames.baseName(it.name) }
            .filter { it.isNotBlank() }
            .distinct()

        if (usableNames.isEmpty()) return emptyList()

        return recipes.mapNotNull { recipe ->
            val matched = mutableListOf<String>()
            val missing = mutableListOf<String>()

            recipe.ingredients.forEach { ingredient ->
                if (PantryStaples.isAlwaysAvailable(ingredient)) {
                    matched += ingredient
                    return@forEach
                }
                val needle = IngredientNames.baseName(ingredient)
                val found = usableNames.any { pantryName ->
                    pantryName.contains(needle) || needle.contains(pantryName) ||
                        tokensOverlap(pantryName, needle)
                }
                if (found) matched += ingredient else missing += ingredient
            }

            // Нужен хотя бы один «настоящий» продукт из холодильника, не только масло/соль/сахар
            val nonStapleMatched = matched.any { !PantryStaples.isAlwaysAvailable(it) }
            if (!nonStapleMatched) return@mapNotNull null

            val required = recipe.ingredients.filterNot { PantryStaples.isAlwaysAvailable(it) }
            val requiredMatched = required.count { it in matched }
            val percent = if (required.isEmpty()) {
                100
            } else {
                ((requiredMatched.toFloat() / required.size) * 100f).toInt()
            }

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

    private fun tokensOverlap(a: String, b: String): Boolean {
        val ta = a.split(" ").filter { it.length >= 3 }.toSet()
        val tb = b.split(" ").filter { it.length >= 3 }.toSet()
        return ta.intersect(tb).isNotEmpty()
    }
}
