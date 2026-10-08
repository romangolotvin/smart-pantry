package com.smartpantry.app.data

enum class ProduceKind {
    VEGETABLE,
    FRUIT;

    val title: String
        get() = when (this) {
            VEGETABLE -> "Укажите овощ"
            FRUIT -> "Укажите фрукт"
        }

    val nameHint: String
        get() = when (this) {
            VEGETABLE -> "Название овоща"
            FRUIT -> "Название фрукта"
        }

    val emoji: String
        get() = when (this) {
            VEGETABLE -> "🥕"
            FRUIT -> "🍎"
        }
}

object ProduceHelper {
    /** Если в названии есть «овощ» / «фрукт» — нужен уточняющий диалог. */
    fun detectKind(name: String): ProduceKind? {
        val n = name.lowercase()
        return when {
            "фрукт" in n -> ProduceKind.FRUIT
            "овощ" in n -> ProduceKind.VEGETABLE
            else -> null
        }
    }

    fun formatWeight(raw: String): String {
        val digits = raw.trim().replace(',', '.')
        if (digits.isBlank()) return ""
        val lower = digits.lowercase()
        if (lower.endsWith("г") || lower.endsWith("кг") || lower.endsWith("ml") || lower.endsWith("мл")) {
            return digits
        }
        return "$digits г"
    }
}
