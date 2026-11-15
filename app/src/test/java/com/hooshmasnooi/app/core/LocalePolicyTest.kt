package com.hooshmasnooi.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocalePolicyTest {
    @Test
    fun persianDeviceUsesRightToLeftLayout() {
        assertTrue(LocalePolicy.isRightToLeft("fa-IR"))
    }

    @Test
    fun dariDeviceUsesRightToLeftLayout() {
        assertTrue(LocalePolicy.isRightToLeft("prs-AF"))
    }

    @Test
    fun englishDeviceUsesLeftToRightLayout() {
        assertFalse(LocalePolicy.isRightToLeft("en-US"))
    }

    @Test
    fun missingLocaleFallsBackToEnglish() {
        assertEquals("en", LocalePolicy.normalizedLanguage(null))
        assertFalse(LocalePolicy.isRightToLeft(null))
    }

    @Test
    fun atlasTextUsesPersianOnPersianDevices() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("fa-IR"))
            assertEquals("سلام", AtlasText.of("سلام", "Hello"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun atlasTextUsesEnglishFallbackOnOtherDevices() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("en-US"))
            assertEquals("Hello", AtlasText.of("سلام", "Hello"))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
