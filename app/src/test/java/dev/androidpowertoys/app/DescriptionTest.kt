package dev.androidpowertoys.app

import org.junit.Assert.*
import org.junit.Test

class DescriptionTest {
    @Test fun exactSourceMatchesRemainUnassessedWithoutInferredDefaults() {
        val sources=sourceDescriptions()
        assertTrue(sources.size>1200)
        val peak=sources.getValue(Table.SYSTEM to "peak_refresh_rate")
        assertTrue(peak.text.contains("refresh rate"))
        assertTrue(peak.documentationOnly)
        assertEquals(Risk.UNKNOWN,peak.risk)
        assertNull(peak.aospDefault)
        assertFalse(riskExplanation(Table.SYSTEM,"peak_refresh_rate").contains("Description unavailable"))
        assertTrue(peak.source.contains("94b4c163b7dfe5ce3607f7bb8456f9573f7de57d"))
        assertNull(sources[Table.GLOBAL to "peak_refresh_rate"])
        assertNull(explanations[Table.SECURE to "vendor_unknown_test_setting"])
        assertEquals(Risk.UNKNOWN,riskFor(Table.SECURE,"vendor_unknown_test_setting"))
    }
    @Test fun audioFamilyUsesOnlyVerifiedStreamsAndRoutes() {
        assertTrue(explanations.getValue(Table.SYSTEM to "volume_music_speaker").text.contains("not a percentage"))
        assertEquals(Risk.HIGH,riskFor(Table.SYSTEM,"volume_alarm_speaker"))
        assertNull(explanations[Table.SYSTEM to "volume_unknown_speaker"])
        assertNull(explanations[Table.SYSTEM to "volume_music_unknown"])
    }
    @Test fun curatedSafetyAndDefaultsOverrideDocumentationFallbacks() {
        val brightness=explanations.getValue(Table.SYSTEM to "screen_brightness")
        assertFalse(brightness.documentationOnly)
        assertEquals(Risk.DISPLAY,brightness.risk)
        assertEquals(Cell(true,"102"),brightness.aospDefault)
        assertEquals(Risk.BLOCKED,riskFor(Table.SECURE,"accessibility_enabled"))
        assertTrue(explanations.getValue(Table.SYSTEM to "pointer_speed").text.contains("-7"))
    }
}
