package io.github.zmdld11.shuschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class ThemeContrastTest {
    @Test fun courseTextMeetsNormalTextContrastOnBothPalettes() {
        (DefaultCourseColors + ArknightsCourseColors).forEach { pair ->
            assertReadable(pair.content, pair.container)
        }
    }

    @Test fun arknightsControlsAndPanelsHaveReadableText() {
        with(ArknightsScheme) {
            listOf(
                onPrimary to primary, onPrimaryContainer to primaryContainer,
                onSecondary to secondary, onSecondaryContainer to secondaryContainer,
                onTertiary to tertiary, onTertiaryContainer to tertiaryContainer,
                onBackground to background, onSurface to surface,
                onSurfaceVariant to surfaceVariant, onSurface to surfaceContainerHighest,
                inverseOnSurface to inverseSurface,
            ).forEach { (text, background) -> assertReadable(text, background) }
        }
    }

    @Test fun importedColorIndicesWrapSafelyWithoutChangingTheirStoredValue() {
        val style = ScheduleStyle(courseColors = ArknightsCourseColors)
        assertEquals(style.colorsFor(0), style.colorsFor(8))
        assertEquals(style.colorsFor(7), style.colorsFor(-1))
        assertNotNull(style.colorsFor(Int.MIN_VALUE))
        assertNotNull(style.colorsFor(Int.MAX_VALUE))
    }

    private fun assertReadable(text: Color, background: Color) {
        val light = maxOf(text.luminance(), background.luminance())
        val dark = minOf(text.luminance(), background.luminance())
        val ratio = (light + 0.05f) / (dark + 0.05f)
        assertTrue("Text $text on $background has contrast $ratio (expected >= 4.5)", ratio >= 4.5f)
    }
}
