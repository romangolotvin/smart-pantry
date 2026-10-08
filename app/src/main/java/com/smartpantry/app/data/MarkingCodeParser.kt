package com.smartpantry.app.data

import java.time.LocalDate

data class ParsedCode(
    val raw: String,
    val isMarking: Boolean,
    val gtin: String? = null,
    val expiryFromCode: LocalDate? = null,
    val codeType: String = "ean13"
)

object MarkingCodeParser {
    fun parse(raw: String): ParsedCode {
        val cleaned = raw.trim().replace("\u001D", "")
        val looksLikeMarking =
            cleaned.startsWith("01") && cleaned.length >= 16 &&
                cleaned.contains("21")

        if (!looksLikeMarking) {
            return ParsedCode(
                raw = cleaned,
                isMarking = false,
                gtin = cleaned.takeIf { it.all(Char::isDigit) && it.length in 8..14 },
                codeType = "ean13"
            )
        }

        val gtin14 = Regex("^01(\\d{14})").find(cleaned)?.groupValues?.getOrNull(1)
        val gtin = gtin14?.let { normalizeGtin(it) }
        val expiry = Regex("17(\\d{6})").find(cleaned)?.groupValues?.getOrNull(1)?.let(::parseYyMmDd)

        return ParsedCode(
            raw = cleaned,
            isMarking = true,
            gtin = gtin,
            expiryFromCode = expiry,
            codeType = "datamatrix"
        )
    }

    private fun normalizeGtin(gtin14: String): String {
        // AI (01) всегда 14 цифр; для OFF удобнее EAN-13 без ведущего нуля.
        return if (gtin14.startsWith("0") && gtin14.length == 14) gtin14.substring(1) else gtin14
    }

    private fun parseYyMmDd(value: String): LocalDate? {
        return try {
            val yy = value.substring(0, 2).toInt()
            val mm = value.substring(2, 4).toInt()
            val dd = value.substring(4, 6).toInt()
            val year = 2000 + yy
            if (dd == 0) {
                LocalDate.of(year, mm, 1).withDayOfMonth(
                    LocalDate.of(year, mm, 1).lengthOfMonth()
                )
            } else {
                LocalDate.of(year, mm, dd)
            }
        } catch (_: Exception) {
            null
        }
    }
}
