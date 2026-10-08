package com.smartpantry.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartpantry.app.data.model.Recipe
import com.smartpantry.app.data.model.RecipeMatch
import com.smartpantry.app.ui.components.RecipeCard
import com.smartpantry.app.ui.components.ScreenHeader

@Composable
fun CookableScreen(
    matches: List<RecipeMatch>,
    pantryCount: Int,
    onRecipeClick: (Recipe) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Что приготовить",
            subtitle = if (pantryCount == 0) {
                "Добавьте продукты со штрихкодом — подберём рецепты под ваш холодильник."
            } else {
                "Рецепты из ваших продуктов. Сначала те, где хватает больше ингредиентов."
            }
        )

        if (matches.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Пока нечего готовить", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Отсканируйте молоко, яйца, овощи и другие продукты — приложение найдёт подходящие блюда.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(matches, key = { it.recipe.id }) { match ->
                    Column {
                        RecipeCard(
                            recipe = match.recipe,
                            onClick = { onRecipeClick(match.recipe) },
                            trailing = "${match.matchPercent}%"
                        )
                        if (match.missingIngredients.isNotEmpty()) {
                            Text(
                                text = "Не хватает: ${match.missingIngredients.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 8.dp)
                            )
                        } else {
                            Text(
                                text = "Все ингредиенты есть!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
