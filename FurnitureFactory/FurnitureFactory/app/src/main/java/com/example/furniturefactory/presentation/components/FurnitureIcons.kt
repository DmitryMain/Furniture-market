package com.example.furniturefactory.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.furniturefactory.data.model.FurnitureCategory

/**
 * Минималистичные контурные иконки мебели (24×24, линия 1.6), в стиле референса.
 * Цвет задаётся параметром tint у Icon.
 */
object FurnitureIcons {

    private fun outline(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()

    val Sofa: ImageVector by lazy {
        outline("Sofa") {
            // спинка
            moveTo(5f, 11f); verticalLineTo(8f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            horizontalLineToRelative(10f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            verticalLineTo(11f)
            // корпус с подлокотниками
            moveTo(3f, 13f)
            arcToRelative(2f, 2f, 0f, false, true, 4f, 0f)
            verticalLineTo(14f); horizontalLineTo(17f); verticalLineTo(13f)
            arcToRelative(2f, 2f, 0f, false, true, 4f, 0f)
            verticalLineTo(17f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
            horizontalLineTo(4f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
            close()
            // ножки
            moveTo(5.5f, 18f); verticalLineTo(19.5f)
            moveTo(18.5f, 18f); verticalLineTo(19.5f)
        }
    }

    val Armchair: ImageVector by lazy {
        outline("Armchair") {
            moveTo(7f, 11f); verticalLineTo(7f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            horizontalLineToRelative(6f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            verticalLineTo(11f)
            moveTo(4f, 13f)
            arcToRelative(2f, 2f, 0f, false, true, 4f, 0f)
            verticalLineTo(14f); horizontalLineTo(16f); verticalLineTo(13f)
            arcToRelative(2f, 2f, 0f, false, true, 4f, 0f)
            verticalLineTo(17f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
            horizontalLineTo(5f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
            close()
            moveTo(6.5f, 18f); verticalLineTo(19.5f)
            moveTo(17.5f, 18f); verticalLineTo(19.5f)
        }
    }

    val Table: ImageVector by lazy {
        outline("Table") {
            moveTo(3f, 8f); horizontalLineTo(21f); verticalLineTo(10f); horizontalLineTo(3f); close()
            moveTo(6f, 10f); verticalLineTo(19f)
            moveTo(18f, 10f); verticalLineTo(19f)
            moveTo(6f, 12.5f); horizontalLineTo(18f)
        }
    }

    val Chair: ImageVector by lazy {
        outline("Chair") {
            // спинка
            moveTo(7f, 4f); horizontalLineTo(17f); verticalLineTo(9f); horizontalLineTo(7f); close()
            // сиденье
            moveTo(6f, 12f); horizontalLineTo(18f); verticalLineTo(14f); horizontalLineTo(6f); close()
            moveTo(8f, 9f); verticalLineTo(12f)
            moveTo(16f, 9f); verticalLineTo(12f)
            moveTo(7.5f, 14f); verticalLineTo(20f)
            moveTo(16.5f, 14f); verticalLineTo(20f)
        }
    }

    val Wardrobe: ImageVector by lazy {
        outline("Wardrobe") {
            moveTo(6f, 3f); horizontalLineTo(18f)
            arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
            verticalLineTo(19f); horizontalLineTo(5f); verticalLineTo(4f)
            arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
            close()
            moveTo(12f, 3f); verticalLineTo(19f)
            moveTo(10f, 10f); verticalLineTo(12f)
            moveTo(14f, 10f); verticalLineTo(12f)
            moveTo(6.5f, 19f); verticalLineTo(21f)
            moveTo(17.5f, 19f); verticalLineTo(21f)
        }
    }

    val Dresser: ImageVector by lazy {
        outline("Dresser") {
            moveTo(4f, 5f); horizontalLineTo(20f); verticalLineTo(18f); horizontalLineTo(4f); close()
            moveTo(4f, 9.5f); horizontalLineTo(20f)
            moveTo(4f, 14f); horizontalLineTo(20f)
            moveTo(10.5f, 7.25f); horizontalLineTo(13.5f)
            moveTo(10.5f, 11.75f); horizontalLineTo(13.5f)
            moveTo(10.5f, 16f); horizontalLineTo(13.5f)
            moveTo(5.5f, 18f); verticalLineTo(20f)
            moveTo(18.5f, 18f); verticalLineTo(20f)
        }
    }

    /** Весы — «Сравнение». */
    val Compare: ImageVector by lazy {
        outline("Compare") {
            moveTo(12f, 4f); verticalLineTo(20f)
            moveTo(8f, 20f); horizontalLineTo(16f)
            moveTo(5f, 7f); horizontalLineTo(19f)
            // левая чаша
            moveTo(5f, 7f); lineTo(2.5f, 13f)
            arcToRelative(2.5f, 2.5f, 0f, false, false, 5f, 0f)
            close()
            // правая чаша
            moveTo(19f, 7f); lineTo(16.5f, 13f)
            arcToRelative(2.5f, 2.5f, 0f, false, false, 5f, 0f)
            close()
        }
    }

    /** Планшет со списком — «Мои заказы». */
    val Orders: ImageVector by lazy {
        outline("Orders") {
            moveTo(9f, 4f); horizontalLineTo(7f)
            arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
            verticalLineTo(19f)
            arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
            horizontalLineTo(17f)
            arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
            verticalLineTo(6f)
            arcToRelative(2f, 2f, 0f, false, false, -2f, -2f)
            horizontalLineTo(15f)
            moveTo(9f, 3f); horizontalLineTo(15f); verticalLineTo(6f); horizontalLineTo(9f); close()
            moveTo(8.5f, 11f); horizontalLineTo(15.5f)
            moveTo(8.5f, 15f); horizontalLineTo(13.5f)
        }
    }

    fun forCategory(category: FurnitureCategory): ImageVector = when (category) {
        FurnitureCategory.SOFAS -> Sofa
        FurnitureCategory.ARMCHAIRS -> Armchair
        FurnitureCategory.TABLES -> Table
        FurnitureCategory.CHAIRS -> Chair
        FurnitureCategory.WARDROBES -> Wardrobe
        FurnitureCategory.DRESSERS -> Dresser
    }
}
