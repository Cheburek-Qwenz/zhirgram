package com.qwenz.zhirgram.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qwenz.zhirgram.data.UiStyle
import com.qwenz.zhirgram.ui.theme.LocalUiStyle

/**
 * Масштаб при нажатии на пружине. Пружина берётся из MotionScheme темы,
 * поэтому в EXPRESSIVE есть отскок, а в MATERIAL3 нет.
 * В MATERIAL3 масштаб не меняется вообще.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.springPress(source: InteractionSource, pressedScale: Float = 0.97f): Modifier {
    if (LocalUiStyle.current != UiStyle.EXPRESSIVE) return this
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressScale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Радиус угла, который сжимается при нажатии (Expressive меняет форму на press). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun pressCorner(source: InteractionSource, rest: Dp, pressed: Dp): Dp {
    if (LocalUiStyle.current != UiStyle.EXPRESSIVE) return pressed // в MATERIAL3 форма постоянная
    val isPressed by source.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) pressed else rest,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressCorner",
    )
    return corner
}

/**
 * Главная кнопка. EXPRESSIVE: настоящая Button с формами по состоянию (ButtonDefaults.shapes()).
 * MATERIAL3: обычная Button. Минимальная высота 56dp (размер M по спецификации).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZhirButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tonal: Boolean = false,
) {
    val m = modifier.heightIn(min = 56.dp)
    val expressive = LocalUiStyle.current == UiStyle.EXPRESSIVE
    when {
        expressive && tonal -> FilledTonalButton(
            onClick = onClick, modifier = m, enabled = enabled,
            shapes = ButtonDefaults.shapes(),
        ) { Text(text) }
        expressive -> Button(
            onClick = onClick, modifier = m, enabled = enabled,
            shapes = ButtonDefaults.shapes(),
        ) { Text(text) }
        tonal -> FilledTonalButton(onClick = onClick, modifier = m, enabled = enabled) { Text(text) }
        else -> Button(onClick = onClick, modifier = m, enabled = enabled) { Text(text) }
    }
}

/**
 * Аватар с буквой. EXPRESSIVE: форма из MaterialShapes (своя для каждого чата).
 * MATERIAL3: круг.
 * TODO: настоящие фото чатов (UpdateChatPhoto + DownloadFile), морф формы при нажатии.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatAvatar(id: Long, title: String, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val expressive = LocalUiStyle.current == UiStyle.EXPRESSIVE
    val polygons = remember {
        listOf(
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.SoftBurst,
        )
    }
    val shape: Shape =
        if (expressive) polygons[Math.floorMod(id, polygons.size.toLong()).toInt()].toShape()
        else CircleShape

    val scheme = MaterialTheme.colorScheme
    val (bg: Color, fg: Color) = when (Math.floorMod(id, 3L).toInt()) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    Box(
        modifier = modifier.size(size).clip(shape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            color = fg,
        )
    }
}
