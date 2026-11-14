package com.hooshmasnooi.app.domain.model

data class ToolDescriptor(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val requiredCapability: ToolCapability,
    val isEnabled: Boolean = false
)

enum class ToolCapability {
    IMAGE_UNDERSTANDING,
    FILE_ANALYSIS,
    WEB_RESEARCH,
    CODE_EXECUTION,
    VOICE_INPUT,
    VOICE_OUTPUT
}
