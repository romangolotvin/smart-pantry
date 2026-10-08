package com.smartpantry.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartpantry.app.data.ProduceHelper
import com.smartpantry.app.data.ProduceKind
import com.smartpantry.app.data.model.PantryItem
import com.smartpantry.app.ui.components.ExpiryBadge
import com.smartpantry.app.ui.components.ScreenHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    items: List<PantryItem>,
    onScanClick: () -> Unit,
    onDelete: (PantryItem) -> Unit,
    onAddManual: (name: String, expiry: LocalDate, quantity: String, imageHint: String) -> Unit
) {
    var showManual by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onScanClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Сканировать")
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = { showManual = true },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить вручную")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScreenHeader(
                title = "Ваши продукты",
                subtitle = "Сфотографируйте штрихкод и укажите срок годности — соберём меню из того, что есть."
            )
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Холодильник пуст", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Нажмите кнопку сканера: обычный штрихкод или Data Matrix «Честный знак».",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onScanClick) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Сканировать штрихкод")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        PantryItemRow(item = item, onDelete = { onDelete(item) })
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }

    if (showManual) {
        ManualProductDialog(
            onDismiss = { showManual = false },
            onConfirm = { name, date, quantity, imageHint ->
                onAddManual(name, date, quantity, imageHint)
                showManual = false
            }
        )
    }
}

@Composable
private fun PantryItemRow(item: PantryItem, onDelete: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.imageHint, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleLarge)
                if (item.brand.isNotBlank()) {
                    Text(
                        item.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "до ${item.expiryDate().format(formatter)} · ${formatQuantityLabel(item.quantity)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                ExpiryBadge(item.status(), item.daysUntilExpiry())
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, expiry: LocalDate, quantity: String, imageHint: String) -> Unit,
    initialName: String = ""
) {
    var name by remember { mutableStateOf(initialName) }
    var showDate by remember { mutableStateOf(false) }
    var produceKind by remember { mutableStateOf<ProduceKind?>(null) }
    var expiry by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    if (produceKind == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Добавить продукт") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Напишите «овощ» или «фрукт» — откроется окно с названием и весом.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { showDate = true }) {
                        Text("Срок годности: ${expiry.format(formatter)}")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isBlank()) return@TextButton
                        val kind = ProduceHelper.detectKind(trimmed)
                        if (kind != null) {
                            produceKind = kind
                        } else {
                            onConfirm(trimmed, expiry, "1", "🛒")
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        )
    }

    produceKind?.let { kind ->
        ProduceDetailsDialog(
            kind = kind,
            onDismiss = { produceKind = null },
            onConfirm = { produceName, weight ->
                onConfirm(produceName, expiry, weight, kind.emoji)
            }
        )
    }

    if (showDate) {
        ExpiryDatePicker(
            initial = expiry,
            onDismiss = { showDate = false },
            onConfirm = {
                expiry = it
                showDate = false
            }
        )
    }
}

@Composable
fun ProduceDetailsDialog(
    kind: ProduceKind,
    onDismiss: () -> Unit,
    onConfirm: (name: String, weight: String) -> Unit
) {
    var produceName by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(kind.title) },
        text = {
            Column {
                OutlinedTextField(
                    value = produceName,
                    onValueChange = { produceName = it },
                    label = { Text(kind.nameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
                    label = { Text("Вес, г") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Например: 500") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val n = produceName.trim()
                    val w = ProduceHelper.formatWeight(weight)
                    if (n.isNotBlank() && w.isNotBlank()) onConfirm(n, w)
                },
                enabled = produceName.isNotBlank() && weight.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Назад") }
        }
    )
}

private fun formatQuantityLabel(quantity: String): String {
    val q = quantity.trim()
    val lower = q.lowercase()
    return when {
        lower.contains("г") || lower.contains("кг") || lower.contains("мл") -> q
        else -> "шт: $q"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDatePicker(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onConfirm(date)
                }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DatePicker(state = state)
    }
}
