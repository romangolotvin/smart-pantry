package com.smartpantry.app.ui.navigation

object Routes {
    const val RECIPES = "recipes"
    const val PANTRY = "pantry"
    const val COOKABLE = "cookable"
    const val SCANNER = "scanner"
    const val RECIPE_DETAIL = "recipe/{recipeId}"

    fun recipeDetail(id: String) = "recipe/$id"
}
