package com.example.furniturefactory

import com.example.furniturefactory.data.local.CartCodec
import com.example.furniturefactory.data.local.DemoCatalog
import com.example.furniturefactory.data.local.IdListCodec
import com.example.furniturefactory.data.model.CartItem
import com.example.furniturefactory.data.model.CartSummary
import com.example.furniturefactory.data.model.FurnitureCategory
import com.example.furniturefactory.domain.ProductFilter
import com.example.furniturefactory.domain.ProductQuery
import com.example.furniturefactory.domain.SortOrder
import com.example.furniturefactory.presentation.screens.checkout.CheckoutField
import com.example.furniturefactory.presentation.screens.checkout.CheckoutForm
import com.example.furniturefactory.presentation.screens.checkout.CheckoutValidator
import com.example.furniturefactory.presentation.util.formatPrice
import com.example.furniturefactory.presentation.util.itemsCountText
import com.example.furniturefactory.presentation.viewer.OrbitCameraController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    private val products = DemoCatalog.products

    @Test
    fun priceIsFormattedWithSpaces() {
        assertEquals("89 990 ₽", formatPrice(89_990))
        assertEquals("139 980 ₽", formatPrice(139_980))
        assertEquals("990 ₽", formatPrice(990))
    }

    @Test
    fun pluralForms() {
        assertEquals("1 товар", itemsCountText(1))
        assertEquals("3 товара", itemsCountText(3))
        assertEquals("11 товаров", itemsCountText(11))
        assertEquals("22 товара", itemsCountText(22))
    }

    @Test
    fun cartTotalMatchesReference() {
        val sofa = products.first { it.id == "sofa_komfort" }
        val table = products.first { it.id == "table_elegant" }
        val summary = CartSummary(listOf(CartItem(sofa, 1), CartItem(table, 1)))
        assertEquals(139_980L, summary.totalPrice)
        assertEquals(2, summary.totalQuantity)
    }

    @Test
    fun searchByNameArticleAndCategory() {
        assertEquals(listOf("sofa_komfort"), ProductQuery.apply(products, "комфорт").map { it.id })
        assertEquals(listOf("table_elegant"), ProductQuery.apply(products, "FF-TB-2051").map { it.id })
        assertTrue(ProductQuery.apply(products, "диваны").all { it.category == FurnitureCategory.SOFAS })
        assertTrue(ProductQuery.apply(products, "Диван-кровать").any { it.id == "sofa_komfort" })
    }

    @Test
    fun filtersAndSorting() {
        val cheap = ProductQuery.apply(products, filter = ProductFilter(priceTo = 40_000), sort = SortOrder.PRICE_ASC)
        assertTrue(cheap.isNotEmpty())
        assertTrue(cheap.all { it.price <= 40_000 })
        assertEquals(cheap.sortedBy { it.price }, cheap)

        val inStock = ProductQuery.apply(products, filter = ProductFilter(inStockOnly = true))
        assertTrue(inStock.all { it.inStock })

        val desc = ProductQuery.apply(products, sort = SortOrder.PRICE_DESC)
        assertEquals(products.maxOf { it.price }, desc.first().price)

        val sofas = ProductQuery.apply(products, category = FurnitureCategory.SOFAS)
        assertEquals(2, sofas.size)
    }

    @Test
    fun cartCodecRoundTrip() {
        val map = linkedMapOf("sofa_komfort" to 2, "chair_nord" to 4)
        assertEquals(map, CartCodec.decode(CartCodec.encode(map)))
        assertEquals(emptyMap<String, Int>(), CartCodec.decode("broken;;x:"))
        assertEquals(listOf("a", "b"), IdListCodec.decode(IdListCodec.encode(listOf("a", "b", "a"))))
    }

    @Test
    fun checkoutValidation() {
        val bad = CheckoutValidator.validate(CheckoutForm())
        assertTrue(CheckoutField.NAME in bad && CheckoutField.PHONE in bad && CheckoutField.EMAIL in bad)
        val good = CheckoutValidator.validate(
            CheckoutForm("Петр Петров", "+7 900 123-45-67", "petr@mail.ru", "Москва", "ул. Ленина, 1, кв. 5"),
        )
        assertTrue(good.isEmpty())
    }

    @Test
    fun orbitCameraIsClampedAndResets() {
        val cam = OrbitCameraController()
        cam.setModelBounds(0f, 0.5f, 0f, 1f)
        repeat(50) { cam.onPinch(2f) }       // сильное приближение
        repeat(200) { cam.update(0.016f) }
        assertEquals(OrbitCameraController.MIN_ZOOM, cam.currentZoom, 0.01f)
        cam.onDrag(0f, 100_000f, 1000f)      // попытка уйти «под пол»/«выше зенита»
        repeat(200) { cam.update(0.016f) }
        assertTrue(cam.currentPitchDegrees <= OrbitCameraController.MAX_PITCH + 0.01f)
        // камера всегда вне сферы модели
        val eye = cam.eye(30f, 0.5f)
        val dist = kotlin.math.sqrt(eye[0] * eye[0] + (eye[1] - 0.5f) * (eye[1] - 0.5f) + eye[2] * eye[2])
        assertTrue(dist > 1f)
        cam.reset()
        repeat(300) { cam.update(0.016f) }
        assertEquals(OrbitCameraController.DEFAULT_YAW, cam.currentYawDegrees, 0.1f)
        assertEquals(1f, cam.currentZoom, 0.01f)
    }

    @Test
    fun everyCatalogProductHasModelAndImage() {
        products.forEach {
            assertTrue(it.model3DPath.endsWith(".glb"))
            assertTrue(it.imageUrl.isNotBlank())
        }
        assertEquals(products.size, products.map { it.id }.toSet().size)
    }
}
