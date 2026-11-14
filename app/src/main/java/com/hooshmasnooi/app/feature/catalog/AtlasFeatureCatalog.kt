package com.hooshmasnooi.app.feature.catalog

import com.hooshmasnooi.app.feature.navigation.AtlasDestination

/**
 * Central product catalog. UI must respect availability and must not advertise
 * foundation-only or planned capabilities as working features.
 */
object AtlasFeatureCatalog {
    val all: List<AtlasFeature> = listOf(
        AtlasFeature(AtlasDestination.CHAT, "feature_chat_title", "feature_chat_description", FeatureAvailability.FOUNDATION_ONLY),
        AtlasFeature(AtlasDestination.HISTORY, "feature_history_title", "feature_history_description", FeatureAvailability.FOUNDATION_ONLY),
        AtlasFeature(AtlasDestination.EXPLORE, "feature_explore_title", "feature_explore_description", FeatureAvailability.PLANNED),
        AtlasFeature(AtlasDestination.TOOLS, "feature_tools_title", "feature_tools_description", FeatureAvailability.PLANNED),
        AtlasFeature(AtlasDestination.PROFILE, "feature_profile_title", "feature_profile_description", FeatureAvailability.PLANNED),
        AtlasFeature(AtlasDestination.SUBSCRIPTION, "feature_subscription_title", "feature_subscription_description", FeatureAvailability.PLANNED),
        AtlasFeature(AtlasDestination.SETTINGS, "feature_settings_title", "feature_settings_description", FeatureAvailability.FOUNDATION_ONLY),
        AtlasFeature(AtlasDestination.ABOUT, "feature_about_title", "feature_about_description", FeatureAvailability.AVAILABLE)
    )

    fun find(destination: AtlasDestination): AtlasFeature? =
        all.firstOrNull { it.destination == destination }
}
