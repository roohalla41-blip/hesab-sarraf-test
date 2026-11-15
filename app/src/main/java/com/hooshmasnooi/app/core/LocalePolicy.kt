package com.hooshmasnooi.app.core

import java.util.Locale

/**
 * Language and layout policy for the first multilingual release.
 * AtlasText selects Persian/English copy; this policy selects layout direction.
 */
object LocalePolicy {
    private val rtlLanguages = setOf("fa", "prs", "ar", "ur", "he")

    fun normalizedLanguage(languageTag: String?): String {
        if (languageTag.isNullOrBlank()) return "en"
        return Locale.forLanguageTag(languageTag).language.ifBlank { "en" }
    }

    fun isRightToLeft(languageTag: String?): Boolean =
        normalizedLanguage(languageTag) in rtlLanguages
}
