package com.example.furniturefactory.presentation.screens.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.furniturefactory.presentation.components.EmptyState
import com.example.furniturefactory.presentation.components.GroupCard
import com.example.furniturefactory.presentation.components.PrimaryButton
import com.example.furniturefactory.presentation.components.ProductImage
import com.example.furniturefactory.presentation.components.ScreenTopBar
import com.example.furniturefactory.presentation.components.SecondaryButton
import com.example.furniturefactory.presentation.util.appViewModel
import com.example.furniturefactory.presentation.util.formatPrice
import com.example.furniturefactory.presentation.util.itemsCountText
import com.example.furniturefactory.ui.theme.FfColors

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit,
) {
    val vm = appViewModel { CheckoutViewModel(it.cartRepository, it.orderRepository, it.userRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val loadedSummary by vm.cartSummary.collectAsStateWithLifecycle()

    LaunchedEffect(vm) { vm.placed.collect { number -> onOrderPlaced(number) } }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Оформление заказа", onBack = onBack)
        val summary = loadedSummary
        if (summary == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FfColors.Ink)
            }
            return@Column
        }
        if (summary.isEmpty && !state.isSubmitting) {
            EmptyState(
                icon = Icons.Filled.Check,
                title = "Нет товаров для оформления",
                subtitle = "Корзина пуста",
                actionText = "Назад",
                onAction = onBack,
            )
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Text("Контактные данные", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            val f = state.form
            FormField("Имя и фамилия", f.fullName, state.errors[CheckoutField.NAME], KeyboardType.Text, KeyboardCapitalization.Words) { v -> vm.update { it.copy(fullName = v) } }
            FormField("Телефон", f.phone, state.errors[CheckoutField.PHONE], KeyboardType.Phone) { v -> vm.update { it.copy(phone = v) } }
            FormField("Электронная почта", f.email, state.errors[CheckoutField.EMAIL], KeyboardType.Email) { v -> vm.update { it.copy(email = v) } }
            Spacer(Modifier.height(8.dp))
            Text("Доставка", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            FormField("Город", f.city, state.errors[CheckoutField.CITY], KeyboardType.Text, KeyboardCapitalization.Words) { v -> vm.update { it.copy(city = v) } }
            FormField("Адрес доставки", f.address, state.errors[CheckoutField.ADDRESS], KeyboardType.Text, KeyboardCapitalization.Sentences) { v -> vm.update { it.copy(address = v) } }
            FormField("Комментарий к заказу", f.comment, null, KeyboardType.Text, KeyboardCapitalization.Sentences, singleLine = false) { v -> vm.update { it.copy(comment = v) } }

            Spacer(Modifier.height(8.dp))
            Text("Ваш заказ", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            GroupCard {
                summary.items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(FfColors.Beige)) {
                            ProductImage(item.product, Modifier.fillMaxSize().padding(4.dp), compactPlaceholder = true)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.product.name, style = MaterialTheme.typography.titleSmall)
                            Text("${item.quantity} шт. × ${formatPrice(item.product.price)}", style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
                        }
                        Text(formatPrice(item.total), style = MaterialTheme.typography.titleSmall)
                    }
                    if (index < summary.items.lastIndex) HorizontalDivider(color = FfColors.Divider)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Демонстрационный режим: оплата не списывается, заказ сохраняется на устройстве.",
                style = MaterialTheme.typography.bodySmall,
                color = FfColors.Gray,
            )
            Spacer(Modifier.height(20.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            state.submitError?.let {
                Text(it, color = FfColors.Error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Итого:", style = MaterialTheme.typography.titleMedium)
                    Text(itemsCountText(summary.totalQuantity), style = MaterialTheme.typography.bodySmall, color = FfColors.Gray)
                }
                Text(formatPrice(summary.totalPrice), style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center) {
                PrimaryButton(
                    text = if (state.isSubmitting) "" else "Подтвердить заказ",
                    onClick = vm::submit,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.isSubmitting) CircularProgressIndicator(color = FfColors.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    error: String?,
    keyboardType: KeyboardType,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FfColors.Ink,
            unfocusedBorderColor = FfColors.Divider,
            focusedLabelColor = FfColors.Ink,
            cursorColor = FfColors.Ink,
            unfocusedContainerColor = FfColors.White,
            focusedContainerColor = FfColors.White,
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    )
}

/** Экран подтверждения: «Спасибо за заказ!» и уникальный номер. */
@Composable
fun OrderSuccessScreen(
    orderNumber: String,
    onOrders: () -> Unit,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(120.dp).background(FfColors.Beige, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(72.dp).background(FfColors.Ink, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = FfColors.White, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Спасибо за заказ!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Ваш заказ успешно оформлен", style = MaterialTheme.typography.bodyLarge, color = FfColors.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(FfColors.Beige)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Номер заказа", style = MaterialTheme.typography.labelMedium, color = FfColors.Gray)
            Text(orderNumber, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Менеджер фабрики свяжется с вами, чтобы согласовать доставку.",
            style = MaterialTheme.typography.bodyMedium,
            color = FfColors.Gray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Мои заказы", onClick = onOrders, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton("На главную", onClick = onHome, modifier = Modifier.fillMaxWidth())
    }
}
