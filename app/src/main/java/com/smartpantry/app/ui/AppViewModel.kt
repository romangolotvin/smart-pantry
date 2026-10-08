package com.smartpantry.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartpantry.app.data.PantryRepository
import com.smartpantry.app.data.ProductLookupService
import com.smartpantry.app.data.RecipeCatalog
import com.smartpantry.app.data.RecipeMatcher
import com.smartpantry.app.data.model.PantryItem
import com.smartpantry.app.data.model.Recipe
import com.smartpantry.app.data.model.RecipeMatch
import com.smartpantry.app.data.model.ScannedProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppUiState(
    val recipes: List<Recipe> = RecipeCatalog.recipes,
    val pantry: List<PantryItem> = emptyList(),
    val matches: List<RecipeMatch> = emptyList(),
    val selectedRecipe: Recipe? = null,
    val scanBusy: Boolean = false,
    val scanError: String? = null,
    val pendingProduct: ScannedProduct? = null
)

class AppViewModel(
    private val pantryRepository: PantryRepository,
    private val productLookupService: ProductLookupService
) : ViewModel() {

    private val selectedRecipe = MutableStateFlow<Recipe?>(null)
    private val scanBusy = MutableStateFlow(false)
    private val scanError = MutableStateFlow<String?>(null)
    private val pendingProduct = MutableStateFlow<ScannedProduct?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        pantryRepository.observeItems(),
        selectedRecipe,
        scanBusy,
        scanError,
        pendingProduct
    ) { pantry, selected, busy, error, pending ->
        AppUiState(
            recipes = RecipeCatalog.recipes,
            pantry = pantry,
            matches = RecipeMatcher.match(RecipeCatalog.recipes, pantry),
            selectedRecipe = selected,
            scanBusy = busy,
            scanError = error,
            pendingProduct = pending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun openRecipe(recipe: Recipe) {
        selectedRecipe.value = recipe
    }

    fun closeRecipe() {
        selectedRecipe.value = null
    }

    fun onBarcodeScanned(barcode: String) {
        if (scanBusy.value) return
        viewModelScope.launch {
            scanBusy.value = true
            scanError.value = null
            try {
                pendingProduct.value = productLookupService.lookup(barcode)
            } catch (e: Exception) {
                scanError.value = "Не удалось найти продукт. Можно добавить вручную."
                pendingProduct.value = ScannedProduct(barcode, "Продукт $barcode", "")
            } finally {
                scanBusy.value = false
            }
        }
    }

    fun clearPendingProduct() {
        pendingProduct.value = null
        scanError.value = null
    }

    fun confirmPendingProduct(
        expiry: LocalDate,
        quantity: String,
        customName: String?,
        imageHint: String? = null
    ) {
        val product = pendingProduct.value ?: return
        viewModelScope.launch {
            var updated = product
            if (!customName.isNullOrBlank()) {
                updated = updated.copy(name = customName.trim())
            }
            if (!imageHint.isNullOrBlank()) {
                updated = updated.copy(imageHint = imageHint)
            }
            pantryRepository.addScanned(updated, expiry, quantity.ifBlank { "1" })
            pendingProduct.value = null
        }
    }

    fun addManualProduct(
        name: String,
        expiry: LocalDate,
        quantity: String = "1",
        imageHint: String = "🛒"
    ) {
        viewModelScope.launch {
            pantryRepository.addManual(
                name = name,
                expiry = expiry,
                quantity = quantity.ifBlank { "1" },
                imageHint = imageHint
            )
        }
    }

    fun deleteItem(item: PantryItem) {
        viewModelScope.launch {
            pantryRepository.delete(item)
        }
    }

    class Factory(
        private val pantryRepository: PantryRepository,
        private val productLookupService: ProductLookupService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(pantryRepository, productLookupService) as T
        }
    }
}
