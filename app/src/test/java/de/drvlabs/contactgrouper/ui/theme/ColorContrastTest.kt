package de.drvlabs.contactgrouper.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {

    @Test
    fun `relative luminance follows wcag endpoints and primary channels`() {
        assertEquals(0.0, relativeLuminance(Color.Black), 0.0001)
        assertEquals(1.0, relativeLuminance(Color.White), 0.0001)
        assertEquals(0.2126, relativeLuminance(Color.Red), 0.0001)
        assertEquals(0.7152, relativeLuminance(Color.Green), 0.0001)
        assertEquals(0.0722, relativeLuminance(Color.Blue), 0.0001)
    }

    @Test
    fun `contrast ratio is symmetric and reaches maximum for black and white`() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.0001)
        assertEquals(
            contrastRatio(Color.Black, Color.White),
            contrastRatio(Color.White, Color.Black),
            0.0001
        )
    }

    @Test
    fun `readable content color uses black for light group colors`() {
        val background = Color(0xFFFFF59D)
        val contentColor = readableContentColorFor(background)

        assertEquals(Color.Black, contentColor)
        assertMeetsReadableContrast(contentColor, background)
    }

    @Test
    fun `readable content color uses white for dark group colors`() {
        val background = Color(0xFF0D47A1)
        val contentColor = readableContentColorFor(background)

        assertEquals(Color.White, contentColor)
        assertMeetsReadableContrast(contentColor, background)
    }

    @Test
    fun `readable content color handles saturated group colors`() {
        val saturatedRed = Color.Red
        val saturatedBlue = Color.Blue

        assertEquals(Color.Black, readableContentColorFor(saturatedRed))
        assertEquals(Color.White, readableContentColorFor(saturatedBlue))
        assertMeetsReadableContrast(readableContentColorFor(saturatedRed), saturatedRed)
        assertMeetsReadableContrast(readableContentColorFor(saturatedBlue), saturatedBlue)
    }

    @Test
    fun `transparent group color resolves against backdrop before content selection`() {
        val visibleBlueOnLightSurface = visibleGroupColor(
            groupColor = Color.Blue.copy(alpha = 0.5f),
            backdrop = Color.White
        )
        val contentColor = readableContentColorFor(visibleBlueOnLightSurface)

        assertEquals(Color.Black, contentColor)
        assertMeetsReadableContrast(contentColor, visibleBlueOnLightSurface)
    }

    private fun assertMeetsReadableContrast(foreground: Color, background: Color) {
        assertTrue(
            "Expected contrast ${contrastRatio(foreground, background)} to meet " +
                MinimumReadableContrastRatio,
            contrastRatio(foreground, background) >= MinimumReadableContrastRatio
        )
    }
}
