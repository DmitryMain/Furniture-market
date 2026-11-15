package com.example.furniturefactory.presentation.screens.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.furniturefactory.AppConfig
import com.example.furniturefactory.BuildConfig
import com.example.furniturefactory.data.model.Order
import com.example.furniturefactory.presentation.components.EmptyState
import com.example.furniturefactory.presentation.components.FurnitureIcons
import com.example.furniturefactory.presentation.components.GroupCard
import com.example.furniturefactory.presentation.components.PrimaryButton
import com.example.furniturefactory.presentation.components.ScreenTopBar
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.presentation.util.formatDate
import com.example.furniturefactory.presentation.util.formatPrice
import com.example.furniturefactory.presentation.util.itemsCountText
import com.example.furniturefactory.ui.theme.FfColors

// ============================================================================ Мои заказы

@Composable
fun OrdersScreen(onBack: () -> Unit, onGoToCatalog: () -> Unit) {
    val vm = appViewModel { OrdersViewModel(it.orderRepository) }
    val orders by vm.orders.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Мои заказы", onBack = onBack)
        val list = orders
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FfColors.Ink)
            }
            list.isEmpty() -> EmptyState(
                icon = FurnitureIcons.Orders,
                title = "Заказов пока нет",
                subtitle = "Оформленные заказы появятся здесь",
                actionText = "Перейти в каталог",
                onAction = onGoToCatalog,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                items(list, key = { it.number }) { OrderCard(it) }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    var expanded by rememberSaveable(order.number) { mutableStateOf(false) }
    GroupCard(Modifier.animateContentSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Заказ ${order.number}", style = MaterialTheme.typography.titleSmall)
                    Text(formatDate(order.createdAtMillis), style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
                }
                Text(
                    order.status.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = FfColors.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(FfColors.Success)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(itemsCountText(order.itemsCount), style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray, modifier = Modifier.weight(1f))
                Text(formatPrice(order.total), style = MaterialTheme.typography.titleMedium)
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Подробнее",
                    tint = FfColors.Gray,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = FfColors.Divider)
                order.lines.forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("${line.name} × ${line.quantity}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(formatPrice(line.total), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(color = FfColors.Divider)
                Spacer(Modifier.height(8.dp))
                Text("Доставка: ${order.contact.city}, ${order.contact.address}", style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
                Text("Получатель: ${order.contact.fullName}, ${order.contact.phone}", style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
                if (order.contact.comment.isNotBlank()) {
                    Text("Комментарий: ${order.contact.comment}", style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
                }
            }
        }
    }
}

// ============================================================================ Настройки профиля

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it.userRepository) }
    val profile by vm.profile.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        val p = profile ?: return@LaunchedEffect
        if (!initialized) {
            if (!p.isGuest) {
                name = p.name
                email = p.email
                phone = p.phone
                city = p.city
            }
            initialized = true
        }
    }

    val nameError = showErrors && name.isBlank()
    val emailError = showErrors && !email.contains('@')

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(if (profile?.isGuest == true) "Вход в профиль" else "Настройки профиля", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                "Демонстрационный профиль хранится только на этом устройстве.",
                style = MaterialTheme.typography.bodyMedium,
                color = FfColors.Gray,
            )
            Spacer(Modifier.height(16.dp))
            ProfileField("Имя", name, nameError, "Введите имя") { name = it }
            ProfileField("Электронная почта", email, emailError, "Введите адрес почты", KeyboardType.Email) { email = it }
            ProfileField("Телефон", phone, false, null, KeyboardType.Phone) { phone = it }
            ProfileField("Город", city, false, null) { city = it }
        }
        PrimaryButton(
            text = "Сохранить",
            onClick = {
                showErrors = true
                if (name.isNotBlank() && email.contains('@')) vm.save(name, email, phone, city, onBack)
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    isError: Boolean,
    errorText: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FfColors.Ink,
            unfocusedBorderColor = FfColors.Divider,
            focusedLabelColor = FfColors.Ink,
            cursorColor = FfColors.Ink,
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    )
}

// ============================================================================ Уведомления

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val vm = appViewModel { NotificationsViewModel(it.userRepository) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Уведомления", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            GroupCard {
                SwitchRow("Статус заказов", "Подтверждение, производство и доставка", settings.orderStatus) { v ->
                    vm.update { it.copy(orderStatus = v) }
                }
                HorizontalDivider(color = FfColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                SwitchRow("Новые коллекции", "Премьеры моделей фабрики", settings.newCollections) { v ->
                    vm.update { it.copy(newCollections = v) }
                }
                HorizontalDivider(color = FfColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
                SwitchRow("Акции и скидки", "Специальные предложения", settings.promotions) { v ->
                    vm.update { it.copy(promotions = v) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Настройки сохраняются на устройстве. Push-уведомления заработают после подключения сервера фабрики.",
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.Gray,
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = FfColors.Ink, checkedThumbColor = FfColors.White),
        )
    }
}

// ============================================================================ О приложении

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("О приложении", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(FfColors.Beige),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FurnitureIcons.Sofa, contentDescription = null, tint = FfColors.Ink, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(AppConfig.APP_NAME, style = MaterialTheme.typography.headlineSmall)
            Text("Версия ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray)
            Spacer(Modifier.height(16.dp))
            Text(
                "Цифровая витрина мебельной фабрики. Рассматривайте мебель в интерактивном 3D, " +
                    "смотрите, как она трансформируется, сравнивайте модели и оформляйте заказ прямо в приложении.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(20.dp))
            GroupCard {
                AboutRow(Icons.Outlined.ViewInAr, "Интерактивные 3D-модели", "Вращение, масштаб и полноэкранный просмотр")
                HorizontalDivider(color = FfColors.Divider, modifier = Modifier.padding(start = 56.dp))
                AboutRow(FurnitureIcons.Sofa, "Анимация трансформации", "Настоящие glTF-анимации из Blender")
                HorizontalDivider(color = FfColors.Divider, modifier = Modifier.padding(start = 56.dp))
                AboutRow(Icons.Outlined.LocalShipping, "Доставка от фабрики", "Без посредников")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "3D-движок: Google Filament (gltfio). Интерфейс: Jetpack Compose, Material 3.",
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.Gray,
            )
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = FfColors.Ink, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
        }
    }
}

// ============================================================================ Поддержка

private val faq = listOf(
    "Как долго ждать доставку?" to "Товары в наличии доставляются за 3–7 дней. Модели под заказ изготавливаются за 2–3 недели.",
    "Можно ли выбрать другую ткань?" to "Да, менеджер предложит образцы тканей после оформления заказа.",
    "Как работает 3D-просмотр?" to "Откройте товар: проведите пальцем, чтобы повернуть модель, сведите или разведите два пальца для масштаба. Кнопка «Показать трансформацию» запускает анимацию механизма.",
    "Есть ли гарантия?" to "На всю мебель фабрики действует гарантия от 18 месяцев, на механизмы трансформации — 3 года.",
)

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun launch(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Нет приложения для этого действия", Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Поддержка", onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            GroupCard {
                ContactRow(Icons.Outlined.Phone, "Позвонить", AppConfig.SUPPORT_PHONE) {
                    launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + AppConfig.SUPPORT_PHONE.filter { it.isDigit() || it == '+' })))
                }
                HorizontalDivider(color = FfColors.Divider, modifier = Modifier.padding(start = 56.dp))
                ContactRow(Icons.Outlined.Email, "Написать", AppConfig.SUPPORT_EMAIL) {
                    launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + AppConfig.SUPPORT_EMAIL)))
                }
                HorizontalDivider(color = FfColors.Divider, modifier = Modifier.padding(start = 56.dp))
                ContactRow(Icons.Outlined.Schedule, "Часы работы", "Ежедневно, 9:00–21:00 (МСК)", onClick = null)
            }
            Spacer(Modifier.height(22.dp))
            Text("Частые вопросы", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            faq.forEach { (q, a) ->
                FaqItem(q, a)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, title: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = FfColors.Ink, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(18.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray)
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var open by rememberSaveable(question) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FfColors.Beige)
            .clickable { open = !open }
            .padding(16.dp)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(question, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Box(Modifier.size(24.dp).background(FfColors.White, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = FfColors.Ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(answer, style = MaterialTheme.typography.bodyMedium, color = FfColors.Gray)
        }
    }
}
