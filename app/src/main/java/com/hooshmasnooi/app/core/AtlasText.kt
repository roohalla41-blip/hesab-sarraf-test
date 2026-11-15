package com.hooshmasnooi.app.core

import java.util.Locale

/** Selects Persian for Persian/Dari devices and English as the international fallback. */
object AtlasText {
    fun of(persian: String, english: String): String {
        val language = LocalePolicy.normalizedLanguage(Locale.getDefault().toLanguageTag())
        return if (language == "fa" || language == "prs") persian else english
    }
}
