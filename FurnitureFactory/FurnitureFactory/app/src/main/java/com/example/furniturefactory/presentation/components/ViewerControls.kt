package com.example.furniturefactory.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ThreeSixty
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.furniturefactory.presentation.util.formatSeconds
import com.example.furniturefactory.presentation.viewer.Furniture3DViewerState
import com.example.furniturefactory.ui.theme.FfColors

/** Плавающие кнопки поверх 3D: «Вращение 360°» и сброс камеры. */
@Composable
fun ViewerQuickControls(state: Furniture3DViewerState, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillToggle(
            icon = Icons.Outlined.ThreeSixty,
            text = "Вращение 360°",
            selected = state.autoRotate,
            onClick = { state.toggleAutoRotate() },
        )
        PillToggle(
            icon = Icons.Outlined.CenterFocusWeak,
            text = "Сброс",
            selected = false,
            onClick = { state.resetCamera() },
        )
    }
}

@Composable
fun PillToggle(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .background(if (selected) FfColors.Ink else FfColors.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) FfColors.White else FfColors.Ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) FfColors.White else FfColors.Ink)
    }
}

/** Заметная кнопка «Показать трансформацию». */
@Composable
fun TransformationButton(state: Furniture3DViewerState, modifier: Modifier = Modifier) {
    PrimaryButton(
        text = "Показать трансформацию",
        onClick = { state.playTransformation() },
        icon = Icons.Outlined.ViewInAr,
        enabled = state.hasAnimations,
        modifier = modifier,
    )
}

/**
 * Панель управления анимацией: выбор клипа, «Воспроизвести»/«Пауза», «С начала»,
 * положения «Начало»/«Конец» и ползунок времени.
 */
@Composable
fun AnimationPanel(
    state: Furniture3DViewerState,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val clip = state.currentClip ?: return
    val textColor = if (onDark) FfColors.White else FfColors.Ink
    val secondary = if (onDark) Color.White.copy(alpha = 0.7f) else FfColors.Gray
    Column(modifier = modifier.fillMaxWidth()) {
        if (state.clips.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.clips.forEachIndexed { position, c ->
                    FilterChipItem(text = c.name, selected = position == state.selectedClipIndex) {
                        state.selectClip(position, autoPlay = true)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundAction(
                icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                description = if (state.isPlaying) "Пауза" else "Воспроизвести",
                filled = true,
                onClick = { state.togglePlay() },
            )
            Spacer(Modifier.width(8.dp))
            RoundAction(icon = Icons.Outlined.Replay, description = "С начала", filled = false, onClick = { state.restart() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.isPlaying) "Воспроизводится" else "Остановлено",
                    style = MaterialTheme.typography.labelMedium,
                    color = secondary,
                )
                Text(clip.name, style = MaterialTheme.typography.titleSmall, color = textColor, maxLines = 1)
            }
            SmallTextAction("Начало", textColor) { state.showStartPose() }
            SmallTextAction("Конец", textColor) { state.showEndPose() }
        }
        Slider(
            value = state.time.coerceIn(0f, clip.durationSec),
            onValueChange = { v ->
                state.pause()
                state.seekTo(v)
            },
            valueRange = 0f..clip.durationSec.coerceAtLeast(0.01f),
            colors = SliderDefaults.colors(
                thumbColor = textColor,
                activeTrackColor = textColor,
                inactiveTrackColor = if (onDark) Color.White.copy(alpha = 0.3f) else FfColors.BeigeDeep,
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatSeconds(state.time), style = MaterialTheme.typography.labelSmall, color = secondary)
            Spacer(Modifier.weight(1f))
            Text(formatSeconds(clip.durationSec), style = MaterialTheme.typography.labelSmall, color = secondary)
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, description: String, filled: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (filled) FfColors.Ink else FfColors.Beige)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (filled) FfColors.White else FfColors.Ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SmallTextAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
