package com.example.furniturefactory.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.furniturefactory.ui.theme.FfColors

enum class BottomTab(
    val route: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconDefault: ImageVector,
) {
    HOME("home", "Главная", Icons.Filled.Home, Icons.Outlined.Home),
    CATALOG("catalog", "Каталог", Icons.Filled.GridView, Icons.Outlined.GridView),
    CART("cart", "Корзина", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    PROFILE("profile", "Профиль", Icons.Filled.Person, Icons.Outlined.Person),
}

/** Нижняя панель навигации: активная вкладка — тёмная, остальные — серые. */
@Composable
fun BottomNavigationBar(
    selected: BottomTab?,
    cartCount: Int,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(FfColors.White)) {
        HorizontalDivider(color = FfColors.Divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val color by animateColorAsState(if (isSelected) FfColors.Ink else FfColors.GrayLight, label = "tabColor")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(tab) },
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 32.dp),
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Icon(
                            if (isSelected) tab.iconSelected else tab.iconDefault,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp),
                        )
                        if (tab == BottomTab.CART && cartCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-4).dp)
                                    .size(16.dp)
                                    .background(FfColors.Ink, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (cartCount > 9) "9+" else cartCount.toString(),
                                    color = FfColors.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
