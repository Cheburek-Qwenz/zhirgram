package com.qwenz.zhirgram.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qwenz.zhirgram.data.AppSettings
import com.qwenz.zhirgram.data.UiStyle
import com.qwenz.zhirgram.ui.components.ZhirButton

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onStyle: (UiStyle) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Стиль интерфейса", style = MaterialTheme.typography.titleMedium)

        // ToggleButton: у выбранной кнопки форма меняется сама (ToggleButtonDefaults.shapes()).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToggleButton(
                checked = settings.style == UiStyle.EXPRESSIVE,
                onCheckedChange = { if (it) onStyle(UiStyle.EXPRESSIVE) },
                shapes = ToggleButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
            ) { Text("Expressive") }
            ToggleButton(
                checked = settings.style == UiStyle.MATERIAL3,
                onCheckedChange = { if (it) onStyle(UiStyle.MATERIAL3) },
                shapes = ToggleButtonDefaults.shapes(),
                modifier = Modifier.weight(1f),
            ) { Text("Material 3") }
        }
        Text(
            when (settings.style) {
                UiStyle.EXPRESSIVE -> "Пружины с отскоком, формы меняются при нажатии."
                UiStyle.MATERIAL3 -> "Спокойные пружины без отскока, обычные формы."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ListItem(
            headlineContent = { Text("Динамические цвета") },
            supportingContent = {
                Text(
                    if (Build.VERSION.SDK_INT >= 31) "Цвета из обоев (Material You)"
                    else "Нужен Android 12 или новее",
                )
            },
            trailingContent = {
                Switch(
                    checked = settings.dynamicColor && Build.VERSION.SDK_INT >= 31,
                    onCheckedChange = onDynamicColor,
                    enabled = Build.VERSION.SDK_INT >= 31,
                )
            },
        )

        ListItem(
            headlineContent = { Text("Классический интерфейс") },
            supportingContent = { Text("Скоро") },
        )

        ZhirButton("Выйти из аккаунта", onClick = onLogOut, tonal = true,
            modifier = Modifier.fillMaxWidth())
    }
}
