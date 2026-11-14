package com.hooshmasnooi.app.feature.catalog

import com.hooshmasnooi.app.feature.navigation.AtlasDestination

enum class FeatureAvailability {
    AVAILABLE,
    FOUNDATION_ONLY,
    PLANNED
}

data class AtlasFeature(
    val destination: AtlasDestination,
    val titleKey: String,
    val descriptionKey: String,
    val availability: FeatureAvailability
)
