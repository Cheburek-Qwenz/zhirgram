package com.qwenz.zhirgram.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.qwenz.zhirgram.data.AppSettings
import com.qwenz.zhirgram.data.UiStyle

/** Какой стиль выбран сейчас. Компоненты читают его, чтобы выбрать Expressive- или обычный вариант. */
val LocalUiStyle = staticCompositionLocalOf { UiStyle.EXPRESSIVE }

/**
 * EXPRESSIVE: MotionScheme.expressive() (пружины с отскоком), формы меняются при нажатии.
 * MATERIAL3: MotionScheme.standard() (спокойные пружины без отскока), обычные формы.
 *
 * TODO: статические схемы ниже по умолчанию фиолетовые. Сгенерировать настоящие схемы
 * от seed-цвета (синий 0xFF1E5BD8) через Material Theme Builder и подставить сюда.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZhirTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val motion = when (settings.style) {
        UiStyle.EXPRESSIVE -> MotionScheme.expressive()
        UiStyle.MATERIAL3 -> MotionScheme.standard()
    }
    CompositionLocalProvider(LocalUiStyle provides settings.style) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = motion,
            content = content,
        )
    }
}
