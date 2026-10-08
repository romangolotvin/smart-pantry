package com.smartpantry.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartpantry.app.data.model.ExpiryStatus
import com.smartpantry.app.data.model.Recipe
import com.smartpantry.app.ui.theme.Cream
import com.smartpantry.app.ui.theme.Forest
import com.smartpantry.app.ui.theme.Leaf

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(listOf(Forest, Leaf))
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Умный холодильник",
            color = Cream.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            color = Cream,
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = Cream.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(recipe.accentColor).copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = recipe.emoji, fontSize = 30.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("${recipe.timeMinutes} мин")
                    Chip(recipe.difficulty)
                    trailing?.let { Chip(it, accent = true) }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, accent: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (accent) MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = if (accent) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun ExpiryBadge(status: ExpiryStatus, days: Long) {
    val (label, color) = when (status) {
        ExpiryStatus.EXPIRED -> "Просрочено" to Color(0xFF9B2226)
        ExpiryStatus.SOON -> "Срочно · $days дн." to Color(0xFFE76F51)
        ExpiryStatus.WATCH -> "Скоро · $days дн." to Color(0xFFE9C46A)
        ExpiryStatus.FRESH -> "Свежо · $days дн." to Color(0xFF2D6A4F)
    }
    Text(
        text = label,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = color
    )
}
