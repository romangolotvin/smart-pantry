package com.smartpantry.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartpantry.app.data.RecipeCatalog
import com.smartpantry.app.ui.AppViewModel
import com.smartpantry.app.ui.components.UpdateDialog
import com.smartpantry.app.ui.navigation.Routes
import com.smartpantry.app.ui.screens.CookableScreen
import com.smartpantry.app.ui.screens.PantryScreen
import com.smartpantry.app.ui.screens.RecipeDetailScreen
import com.smartpantry.app.ui.screens.RecipesScreen
import com.smartpantry.app.ui.screens.ScannerScreen
import com.smartpantry.app.ui.theme.SmartPantryTheme
import com.smartpantry.app.update.UpdateConfig
import com.smartpantry.app.update.UpdateService
import com.smartpantry.app.update.UpdateState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SmartPantryApplication
        setContent {
            SmartPantryTheme {
                val vm: AppViewModel = viewModel(
                    factory = AppViewModel.Factory(app.pantryRepository, app.productLookupService)
                )
                SmartPantryRoot(vm = vm, updateService = app.updateService)
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun SmartPantryRoot(
    vm: AppViewModel,
    updateService: UpdateService
) {
    val navController = rememberNavController()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val updateState by updateService.state.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as ComponentActivity
    var manualCheck by remember { mutableStateOf(false) }

    val tabs = listOf(
        Tab(Routes.RECIPES, "Рецепты", Icons.Default.List),
        Tab(Routes.PANTRY, "Холодильник", Icons.Default.Home),
        Tab(Routes.COOKABLE, "Готовим", Icons.Default.Favorite)
    )
    val showBottomBar = currentRoute in tabs.map { it.route }

    LaunchedEffect(Unit) {
        updateService.checkForUpdates(silent = true)
    }

    val dialogState: UpdateState? = when (val s = updateState) {
        is UpdateState.Available,
        is UpdateState.Downloading,
        is UpdateState.ReadyToInstall -> s
        is UpdateState.Error,
        is UpdateState.UpToDate -> if (manualCheck) s else null
        else -> null
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.RECIPES,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.RECIPES) {
                RecipesScreen(
                    recipes = state.recipes,
                    appVersion = UpdateConfig.APP_VERSION,
                    onCheckUpdates = {
                        manualCheck = true
                        scope.launch { updateService.checkForUpdates(silent = false) }
                    },
                    onRecipeClick = { recipe ->
                        navController.navigate(Routes.recipeDetail(recipe.id))
                    }
                )
            }
            composable(Routes.PANTRY) {
                PantryScreen(
                    items = state.pantry,
                    onScanClick = { navController.navigate(Routes.SCANNER) },
                    onDelete = vm::deleteItem,
                    onAddManual = { name, expiry, quantity, imageHint ->
                        vm.addManualProduct(name, expiry, quantity, imageHint)
                    }
                )
            }
            composable(Routes.COOKABLE) {
                CookableScreen(
                    matches = state.matches,
                    pantryCount = state.pantry.size,
                    onRecipeClick = { recipe ->
                        navController.navigate(Routes.recipeDetail(recipe.id))
                    }
                )
            }
            composable(Routes.SCANNER) {
                ScannerScreen(
                    busy = state.scanBusy,
                    error = state.scanError,
                    pendingProduct = state.pendingProduct,
                    onBack = { navController.popBackStack() },
                    onBarcode = vm::onBarcodeScanned,
                    onConfirm = { date, qty, name, imageHint ->
                        vm.confirmPendingProduct(date, qty, name, imageHint)
                        navController.popBackStack()
                    },
                    onClearPending = vm::clearPendingProduct
                )
            }
            composable(Routes.RECIPE_DETAIL) { entry ->
                val id = entry.arguments?.getString("recipeId").orEmpty()
                val recipe = RecipeCatalog.byId(id)
                if (recipe != null) {
                    RecipeDetailScreen(
                        recipe = recipe,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    dialogState?.let { dialog ->
        UpdateDialog(
            state = dialog,
            onUpdate = {
                scope.launch { updateService.downloadAndInstall(activity) }
            },
            onDismiss = {
                manualCheck = false
                updateService.dismissStatus()
            }
        )
    }
}
