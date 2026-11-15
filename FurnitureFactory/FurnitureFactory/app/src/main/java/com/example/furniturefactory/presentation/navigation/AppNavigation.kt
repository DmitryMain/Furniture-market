package com.example.furniturefactory.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.furniturefactory.data.model.FurnitureCategory
import com.example.furniturefactory.data.repository.CartRepository
import com.example.furniturefactory.presentation.components.BottomNavigationBar
import com.example.furniturefactory.presentation.components.BottomTab
import com.example.furniturefactory.presentation.screens.cart.CartScreen
import com.example.furniturefactory.presentation.screens.catalog.CatalogScreen
import com.example.furniturefactory.presentation.screens.checkout.CheckoutScreen
import com.example.furniturefactory.presentation.screens.checkout.OrderSuccessScreen
import com.example.furniturefactory.presentation.screens.compare.CompareScreen
import com.example.furniturefactory.presentation.screens.favorites.FavoritesScreen
import com.example.furniturefactory.presentation.screens.home.HomeScreen
import com.example.furniturefactory.presentation.screens.product.ProductScreen
import com.example.furniturefactory.presentation.screens.profile.AboutScreen
import com.example.furniturefactory.presentation.screens.profile.NotificationsScreen
import com.example.furniturefactory.presentation.screens.profile.OrdersScreen
import com.example.furniturefactory.presentation.screens.profile.ProfileScreen
import com.example.furniturefactory.presentation.screens.profile.SettingsScreen
import com.example.furniturefactory.presentation.screens.profile.SupportScreen
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.ui.theme.FfColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Счётчик товаров для значка на вкладке «Корзина». */
class CartBadgeViewModel(cart: CartRepository) : ViewModel() {
    val count: StateFlow<Int> = cart.cart.map { it.totalQuantity }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@Composable
fun FurnitureAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = BottomTab.entries.firstOrNull { it.route == currentRoute?.substringBefore("?") }
    val badgeVm = appViewModel { CartBadgeViewModel(it.cartRepository) }
    val cartCount by badgeVm.count.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FfColors.White,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = selectedTab != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                BottomNavigationBar(
                    selected = selectedTab,
                    cartCount = cartCount,
                    onSelect = { tab -> navController.navigateToTab(tab.route) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            appGraph(navController)
        }
    }
}

fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun androidx.navigation.NavGraphBuilder.appGraph(nav: NavHostController) {
    val openProduct: (String) -> Unit = { id -> nav.navigate(Routes.product(id)) }
    val openProduct3D: (String) -> Unit = { id -> nav.navigate(Routes.product(id, fullscreen = true)) }
    val back: () -> Unit = { nav.popBackStack() }

    composable(Routes.HOME) {
        HomeScreen(
            onProductClick = openProduct,
            onOpen3D = openProduct3D,
            onSeeAll = { nav.navigateToTab(Routes.CATALOG) },
        )
    }

    composable(
        Routes.CATALOG_PATTERN,
        arguments = listOf(navArgument("category") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }),
    ) { entry ->
        val category = entry.arguments?.getString("category")
            ?.let { name -> FurnitureCategory.entries.firstOrNull { it.name == name } }
        CatalogScreen(initialCategory = category, onProductClick = openProduct)
    }

    composable(Routes.CART) {
        CartScreen(
            onCheckout = { nav.navigate(Routes.CHECKOUT) },
            onGoToCatalog = { nav.navigateToTab(Routes.CATALOG) },
            onProductClick = openProduct,
        )
    }

    composable(Routes.PROFILE) {
        ProfileScreen(
            onOrders = { nav.navigate(Routes.ORDERS) },
            onFavorites = { nav.navigate(Routes.FAVORITES) },
            onCompare = { nav.navigate(Routes.COMPARE) },
            onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
            onAbout = { nav.navigate(Routes.ABOUT) },
            onSupport = { nav.navigate(Routes.SUPPORT) },
            onSettings = { nav.navigate(Routes.SETTINGS) },
            onBanner = { nav.navigateToTab(Routes.CATALOG) },
        )
    }

    composable(
        Routes.PRODUCT_PATTERN,
        arguments = listOf(
            navArgument("id") { type = NavType.StringType },
            navArgument("fullscreen") { type = NavType.BoolType; defaultValue = false },
        ),
        enterTransition = { slideInHorizontally(tween(260)) { it / 3 } + fadeIn(tween(260)) },
        popExitTransition = { slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(220)) },
    ) { entry ->
        val id = entry.arguments?.getString("id").orEmpty()
        val fullscreen = entry.arguments?.getBoolean("fullscreen") ?: false
        ProductScreen(
            productId = id,
            startFullscreen = fullscreen,
            onBack = back,
            onOpenCart = { nav.navigateToTab(Routes.CART) },
        )
    }

    composable(Routes.CHECKOUT) {
        CheckoutScreen(
            onBack = back,
            onOrderPlaced = { number ->
                nav.navigate(Routes.orderSuccess(number)) {
                    popUpTo(Routes.CART) { inclusive = false }
                }
            },
        )
    }

    composable(
        Routes.ORDER_SUCCESS_PATTERN,
        arguments = listOf(navArgument("number") { type = NavType.StringType }),
    ) { entry ->
        OrderSuccessScreen(
            orderNumber = entry.arguments?.getString("number").orEmpty(),
            onOrders = {
                nav.navigate(Routes.ORDERS) { popUpTo(Routes.CART) { inclusive = false } }
            },
            onHome = {
                // убираем экран успеха из стека вкладки «Корзина», затем переходим на главную
                nav.popBackStack(Routes.CART, inclusive = false)
                nav.navigateToTab(Routes.HOME)
            },
        )
    }

    composable(Routes.FAVORITES) { FavoritesScreen(onBack = back, onProductClick = openProduct, onGoToCatalog = { nav.navigateToTab(Routes.CATALOG) }) }
    composable(Routes.COMPARE) { CompareScreen(onBack = back, onProductClick = openProduct, onGoToCatalog = { nav.navigateToTab(Routes.CATALOG) }) }
    composable(Routes.ORDERS) { OrdersScreen(onBack = back, onGoToCatalog = { nav.navigateToTab(Routes.CATALOG) }) }
    composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = back) }
    composable(Routes.ABOUT) { AboutScreen(onBack = back) }
    composable(Routes.SUPPORT) { SupportScreen(onBack = back) }
    composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
}
