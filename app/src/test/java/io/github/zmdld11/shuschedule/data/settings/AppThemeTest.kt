package io.github.zmdld11.shuschedule.data.settings

import org.junit.Assert.*
import org.junit.Test

class AppThemeTest {
    @Test fun missingOrUnknownPreferenceKeepsExistingDefaultAppearance() {
        listOf(null, "", "removed-theme", "ARKNIGHTS").forEach { id ->
            assertEquals(AppTheme.DEFAULT, AppTheme.fromId(id))
        }
        assertEquals(AppearanceSettings(AppTheme.DEFAULT, true), AppearanceSettings())
    }

    @Test fun stableStorageIdsResolveToTheirThemes() {
        assertEquals(AppTheme.DEFAULT, AppTheme.fromId("default"))
        assertEquals(AppTheme.ARKNIGHTS, AppTheme.fromId("arknights"))
        assertEquals(AppTheme.entries.size, AppTheme.entries.map { it.id }.toSet().size)
    }

    @Test fun arknightsIsDarkAndNeverUsesWallpaperColors() {
        for (systemDark in listOf(false, true)) {
            assertTrue(AppTheme.ARKNIGHTS.isDark(systemDark))
            assertEquals(systemDark, AppTheme.DEFAULT.isDark(systemDark))
        }
        for (dynamicColor in listOf(false, true)) {
            assertFalse(AppTheme.ARKNIGHTS.usesDynamicColor(dynamicColor))
            assertEquals(dynamicColor, AppTheme.DEFAULT.usesDynamicColor(dynamicColor))
        }
    }
}
