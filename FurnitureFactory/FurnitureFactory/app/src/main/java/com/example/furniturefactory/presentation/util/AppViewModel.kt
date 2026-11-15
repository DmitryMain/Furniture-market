package com.example.furniturefactory.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.furniturefactory.AppContainer
import com.example.furniturefactory.FurnitureApp

/**
 * Создаёт ViewModel с доступом к [AppContainer]. ViewModel привязывается к текущему
 * NavBackStackEntry (каждый экран получает свой экземпляр).
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as FurnitureApp).container
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
