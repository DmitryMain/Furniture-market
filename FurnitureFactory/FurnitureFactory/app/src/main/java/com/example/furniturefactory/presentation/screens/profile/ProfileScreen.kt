package com.example.furniturefactory.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.furniturefactory.presentation.components.FurnitureIcons
import com.example.furniturefactory.presentation.components.GroupCard
import com.example.furniturefactory.presentation.components.productImageModel
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.ui.theme.FfColors

@Composable
fun ProfileScreen(
    onOrders: () -> Unit,
    onFavorites: () -> Unit,
    onCompare: () -> Unit,
    onNotifications: () -> Unit,
    onAbout: () -> Unit,
    onSupport: () -> Unit,
    onSettings: () -> Unit,
    onBanner: () -> Unit,
) {
    val vm = appViewModel { ProfileViewModel(it.userRepository, it.orderRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val profile = state.profile

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(FfColors.Beige),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = FfColors.GrayLight, modifier = Modifier.size(76.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Настройки профиля", tint = FfColors.Ink)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(profile?.name ?: "", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(2.dp))
        Text(profile?.email ?: "", style = MaterialTheme.typography.bodyLarge, color = FfColors.Gray)
        Spacer(Modifier.height(20.dp))

        GroupCard {
            MenuRow(FurnitureIcons.Orders, "Мои заказы", badge = state.ordersCount, onClick = onOrders)
            MenuDivider()
            MenuRow(Icons.Outlined.FavoriteBorder, "Избранное", badge = state.favoritesCount, onClick = onFavorites)
            MenuDivider()
            MenuRow(FurnitureIcons.Compare, "Сравнение", badge = state.compareCount, onClick = onCompare)
        }
        Spacer(Modifier.height(12.dp))
        GroupCard {
            MenuRow(Icons.Outlined.Notifications, "Уведомления", onClick = onNotifications)
        }
        Spacer(Modifier.height(12.dp))
        GroupCard {
            MenuRow(Icons.Outlined.Info, "О приложении", onClick = onAbout)
            MenuDivider()
            MenuRow(Icons.Outlined.HeadsetMic, "Поддержка", onClick = onSupport)
            MenuDivider()
            if (profile?.isGuest == true) {
                MenuRow(Icons.AutoMirrored.Outlined.Login, "Войти", onClick = onSettings)
            } else {
                MenuRow(Icons.AutoMirrored.Outlined.Logout, "Выйти", onClick = { confirmLogout = true })
            }
        }
        Spacer(Modifier.height(16.dp))
        InspirationBanner(onClick = onBanner)
        Spacer(Modifier.height(16.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Выйти из профиля?") },
            text = { Text("Корзина, избранное и заказы останутся на устройстве.") },
            confirmButton = {
                TextButton(onClick = { vm.logout(); confirmLogout = false }) { Text("Выйти", color = FfColors.Ink) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Отмена", color = FfColors.Gray) }
            },
            containerColor = FfColors.White,
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(color = FfColors.Divider, modifier = Modifier.padding(start = 56.dp))
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, badge: Int = 0, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = FfColors.Ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (badge > 0) {
            Text(
                badge.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = FfColors.Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(FfColors.Beige)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = FfColors.GrayLight)
    }
}

/** Баннер «Вдохновение для вашего дома» — ведёт в каталог. */
@Composable
private fun InspirationBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FfColors.Beige)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = productImageModel("images/banner_interior.jpg"),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to FfColors.Beige,
                        0.45f to FfColors.Beige.copy(alpha = 0.85f),
                        0.75f to FfColors.Beige.copy(alpha = 0f),
                    ),
                ),
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 18.dp, end = 120.dp)) {
            Text("Вдохновение\nдля вашего дома", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Откройте каталог\nновых коллекций", style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(36.dp)
                .background(FfColors.Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Открыть каталог", tint = FfColors.White, modifier = Modifier.size(18.dp))
        }
    }
}
