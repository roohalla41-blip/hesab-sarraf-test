package com.hooshmasnooi.app.domain.model

/** Safe, provider-neutral generation preferences. */
data class GenerationOptions(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED
) {
    init {
        require(temperature == null || temperature in 0f..2f)
        require(maxOutputTokens == null || maxOutputTokens > 0)
    }
}

enum class ResponseStyle {
    CONCISE,
    BALANCED,
    DETAILED
}
