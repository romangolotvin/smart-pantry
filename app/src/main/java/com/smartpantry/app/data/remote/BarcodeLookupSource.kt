package com.smartpantry.app.data.remote

import com.smartpantry.app.data.model.ScannedProduct

/** Источник названий по штрихкоду. null = в этой базе нет. */
fun interface BarcodeLookupSource {
    suspend fun lookup(barcode: String): ScannedProduct?
}

object ProductNameQuality {
    fun isUseful(name: String?, barcode: String = ""): Boolean {
        val n = name?.trim().orEmpty()
        if (n.length < 2) return false
        if (n.equals(barcode, ignoreCase = true)) return false
        if (n.matches(Regex("""(?i)продукт\s*[\d\s]+"""))) return false
        if (n.matches(Regex("""(?i)product\s*[\d\s]+"""))) return false
        if (n.matches(Regex("""(?i)unknown.*"""))) return false
        return true
    }
}

object BarcodeVariants {
    /** Варианты одного кода: как отсканировали + GTIN/EAN нормализации. */
    fun of(raw: String): List<String> {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return listOf(raw.trim()).filter { it.isNotEmpty() }

        val variants = linkedSetOf<String>()
        variants += digits
        variants += digits.trimStart('0').ifEmpty { digits }

        // EAN-13 / UPC-A
        if (digits.length <= 13) {
            variants += digits.padStart(13, '0')
        }
        // GTIN-14
        if (digits.length <= 14) {
            variants += digits.padStart(14, '0')
        }
        // UPC-A (12) из EAN-13 с ведущим 0
        if (digits.length == 13 && digits.startsWith("0")) {
            variants += digits.substring(1)
        }
        // EAN-13 из UPC-A
        if (digits.length == 12) {
            variants += "0$digits"
        }

        return variants.toList()
    }
}
