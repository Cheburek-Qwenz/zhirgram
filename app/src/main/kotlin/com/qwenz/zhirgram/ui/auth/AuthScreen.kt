package com.qwenz.zhirgram.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.qwenz.zhirgram.BuildConfig
import com.qwenz.zhirgram.data.UiStyle
import com.qwenz.zhirgram.td.AuthState
import com.qwenz.zhirgram.ui.components.ZhirButton
import com.qwenz.zhirgram.ui.theme.LocalUiStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthScreen(
    state: AuthState,
    onPhone: (String) -> Unit,
    onCode: (String) -> Unit,
    onPassword: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Пружины читаем из темы ДО AnimatedContent: лямбда transitionSpec не composable.
    val motion = MaterialTheme.motionScheme
    val spatial = motion.defaultSpatialSpec<IntOffset>()
    val effects = motion.defaultEffectsSpec<Float>()

    Box(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (slideInHorizontally(spatial) { it / 4 } + fadeIn(effects)) togetherWith
                    (slideOutHorizontally(spatial) { -it / 4 } + fadeOut(effects))
            },
            label = "authStep",
        ) { s ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (s) {
                    AuthState.Loading, AuthState.Ready -> LoadingBlock()
                    AuthState.WaitPhone -> PhoneStep(onPhone)
                    AuthState.WaitCode -> CodeStep(onCode)
                    is AuthState.WaitPassword -> PasswordStep(s.hint, onPassword)
                    is AuthState.Unsupported -> Text(
                        "Этот шаг входа пока не поддерживается: ${s.name}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (BuildConfig.TG_APP_ID == 0) {
                    Text(
                        "TG_APP_ID не задан. Добавь api_id и api_hash (см. README).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingBlock() {
    // Короткое неопределённое ожидание: LoadingIndicator (морфящиеся формы) в Expressive
    if (LocalUiStyle.current == UiStyle.EXPRESSIVE) LoadingIndicator()
    else CircularProgressIndicator()
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PhoneStep(onSubmit: (String) -> Unit) {
    var phone by rememberSaveable { mutableStateOf("") }
    StepHeader("Вход", "Введи номер телефона в международном формате")
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Номер телефона") },
        placeholder = { Text("+7 900 000 00 00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
    )
    ZhirButton("Далее", onClick = { onSubmit(phone) }, enabled = phone.length >= 5,
        modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CodeStep(onSubmit: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    StepHeader("Код", "Мы отправили код в Telegram или SMS")
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.filter(Char::isDigit) },
        label = { Text("Код") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
    ZhirButton("Войти", onClick = { onSubmit(code) }, enabled = code.length >= 4,
        modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PasswordStep(hint: String, onSubmit: (String) -> Unit) {
    var pass by rememberSaveable { mutableStateOf("") }
    StepHeader(
        "Облачный пароль",
        if (hint.isNotEmpty()) "Подсказка: $hint" else "Введи пароль двухэтапной проверки",
    )
    OutlinedTextField(
        value = pass,
        onValueChange = { pass = it },
        label = { Text("Пароль") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    ZhirButton("Продолжить", onClick = { onSubmit(pass) }, enabled = pass.isNotEmpty(),
        modifier = Modifier.fillMaxWidth())
}
