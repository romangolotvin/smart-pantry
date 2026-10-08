package com.smartpantry.app.update

object UpdateConfig {
    /** Локальная версия этой сборки. Поднимай при каждом релизе. */
    const val APP_VERSION = "1.1.1"

    /**
     * Файл version.json в интернете.
     * Формат: { "version": "1.0.1", "notes": "...", "android": "https://..." }
     */
    const val MANIFEST_URL =
        "https://raw.githubusercontent.com/romangolotvin/smart-pantry/main/updates/version.json"
}
