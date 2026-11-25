package dev.voicejournal.ui.designsystem.responsive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveScaleTest {

    @Test
    fun `reference 412dp phone produces unit scale factor`() {
        val info = computeAppWindowInfo(
            screenWidthDp = 412,
            screenHeightDp = 915,
            isLandscape = false,
            rawFontScale = 1.0f
        )

        assertEquals(1.0f, info.uiScaleFactor, 0.001f)
        assertEquals(1.0f, info.effectiveFontScale, 0.001f)
        assertEquals(412.0f, info.normalizedWidthDp, 0.5f)
        assertEquals(AppWidthSizeClass.COMPACT, info.widthSizeClass)
        assertFalse(info.isWideLayout)
        assertFalse(info.isNarrowPhone)
        assertFalse(info.isCompactLandscape)
    }

    @Test
    fun `narrow 360dp phone scales down proportionally to preserve 412dp visual baseline`() {
        val info = computeAppWindowInfo(
            screenWidthDp = 360,
            screenHeightDp = 800,
            isLandscape = false,
            rawFontScale = 1.0f
        )

        assertEquals(360f / 412f, info.uiScaleFactor, 0.001f)
        assertEquals(412.0f, info.normalizedWidthDp, 0.5f)
        assertTrue(info.isNarrowPhone)
        assertEquals(AppWidthSizeClass.COMPACT, info.widthSizeClass)
    }

    @Test
    fun `wide 443dp phone scales up proportionally to preserve 412dp visual baseline`() {
        val info = computeAppWindowInfo(
            screenWidthDp = 443,
            screenHeightDp = 984,
            isLandscape = false,
            rawFontScale = 1.0f
        )

        assertEquals(443f / 412f, info.uiScaleFactor, 0.001f)
        assertEquals(412.0f, info.normalizedWidthDp, 0.5f)
        assertFalse(info.isNarrowPhone)
        assertEquals(AppWidthSizeClass.COMPACT, info.widthSizeClass)
    }

    @Test
    fun `extreme phone widths clamp scale factor within safe bounds`() {
        assertEquals(0.84f, calculateUiScaleFactor(280), 0.001f)
        assertEquals(1.15f, calculateUiScaleFactor(520), 0.001f)
    }

    @Test
    fun `tablets and foldables keep native 1f density scale and enable wide layout`() {
        val info = computeAppWindowInfo(
            screenWidthDp = 800,
            screenHeightDp = 1280,
            isLandscape = false,
            rawFontScale = 1.25f
        )

        assertEquals(1.0f, info.uiScaleFactor, 0.001f)
        assertEquals(1.25f, info.effectiveFontScale, 0.001f)
        assertEquals(AppWidthSizeClass.MEDIUM, info.widthSizeClass)
        assertTrue(info.isWideLayout)
    }

    @Test
    fun `phone font scale is clamped within 0_95 to 1_15`() {
        assertEquals(0.95f, normalizeFontScale(0.80f, 412), 0.001f)
        assertEquals(1.05f, normalizeFontScale(1.05f, 360), 0.001f)
        assertEquals(1.15f, normalizeFontScale(1.40f, 443), 0.001f)
    }

    @Test
    fun `compact landscape uses shortest width so orientation keeps scale stable`() {
        val portrait = computeAppWindowInfo(
            screenWidthDp = 390,
            screenHeightDp = 860,
            isLandscape = false,
            rawFontScale = 1.0f
        )
        val landscape = computeAppWindowInfo(
            screenWidthDp = 860,
            screenHeightDp = 390,
            isLandscape = true,
            rawFontScale = 1.0f
        )

        assertEquals(portrait.uiScaleFactor, landscape.uiScaleFactor, 0.001f)
        assertTrue(landscape.isCompactLandscape)
        assertTrue(landscape.isWideLayout)
    }

    @Test
    fun `zero or negative dimensions do not throw or produce NaN`() {
        val info = computeAppWindowInfo(
            screenWidthDp = 0,
            screenHeightDp = 0,
            isLandscape = false,
            rawFontScale = 1.0f
        )

        assertTrue(info.uiScaleFactor.isFinite())
        assertTrue(info.normalizedWidthDp.isFinite())
        assertTrue(info.normalizedHeightDp.isFinite())
        assertTrue(info.aspectRatio.isFinite())
    }
}
