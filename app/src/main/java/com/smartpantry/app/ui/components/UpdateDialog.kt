package com.smartpantry.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartpantry.app.update.UpdateState

@Composable
fun UpdateDialog(
    state: UpdateState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        is UpdateState.Available -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Доступна версия ${state.manifest.version}") },
                text = {
                    Text(
                        state.manifest.notes.ifBlank {
                            "Есть новая версия приложения. Обновить сейчас?"
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = onUpdate) { Text("Обновить") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Позже") }
                }
            )
        }

        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Скачиваю обновление…") },
                text = {
                    Column {
                        val total = state.total
                        if (total > 0) {
                            LinearProgressIndicator(
                                progress = { (state.loaded.toFloat() / total).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("${state.loaded / 1024 / 1024} / ${total / 1024 / 1024} МБ")
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = {}
            )
        }

        is UpdateState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Обновление") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            )
        }

        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Обновления") },
                text = { Text("У тебя последняя версия.") },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            )
        }

        is UpdateState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Готово") },
                text = { Text("Подтверди установку в окне Android.") },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            )
        }

        else -> Unit
    }
}
