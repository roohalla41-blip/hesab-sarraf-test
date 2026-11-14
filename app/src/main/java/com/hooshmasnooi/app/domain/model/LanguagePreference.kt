package com.hooshmasnooi.app.domain.model

/** BCP-47 language tag, for example fa, prs, or en. */
data class LanguagePreference(
    val languageTag: String,
    val followDeviceLanguage: Boolean = true
) {
    init {
        require(languageTag.isNotBlank())
    }
}
