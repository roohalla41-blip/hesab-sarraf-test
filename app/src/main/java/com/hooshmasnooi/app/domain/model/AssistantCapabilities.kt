package com.hooshmasnooi.app.domain.model

data class AssistantCapabilities(
    val text: Boolean = true,
    val imageUnderstanding: Boolean = false,
    val fileUnderstanding: Boolean = false,
    val voiceInput: Boolean = false,
    val voiceOutput: Boolean = false,
    val webResearch: Boolean = false,
    val codeExecution: Boolean = false
)
