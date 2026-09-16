package io.github.zmdld11.shuschedule.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.zmdld11.shuschedule.R
import io.github.zmdld11.shuschedule.data.settings.AppTheme

/** 明日方舟主题：罗德岛终端，固定深色（mashirozx/arknights-ui 风格）；仅随 full 变体分发 */
object ArknightsThemeDefinition : ScheduleThemeDefinition {
    override val id = AppTheme.ARKNIGHTS.id
    override val title = AppTheme.ARKNIGHTS.title
    override val description = AppTheme.ARKNIGHTS.description
    override val backgroundRes = R.drawable.arknights_background
    override val logoRes = R.drawable.arknights_rhodes_island
    override val widget = WidgetThemeColors(
        rootBackground = 0xF0151A1D, // 罗德岛终端深色底
        itemBackground = 0x2EFFFFFF,
        textPrimary = 0xFFF0F2F3,
        textSecondary = 0xFFFFD900, // 战术黄点缀
    )

    override fun isDark(systemDark: Boolean) = AppTheme.ARKNIGHTS.isDark(systemDark)
    override fun usesDynamicColor(enabled: Boolean) = AppTheme.ARKNIGHTS.usesDynamicColor(enabled)

    @Composable
    override fun colorScheme(systemDark: Boolean, dynamicColor: Boolean): ColorScheme = ArknightsScheme

    override val shapes = Shapes(
        extraSmall = CutCornerShape(2.dp),
        small = CutCornerShape(4.dp),
        medium = CutCornerShape(6.dp),
        large = CutCornerShape(10.dp),
        extraLarge = CutCornerShape(14.dp),
    )

    override val scheduleStyle = ScheduleStyle(
        courseShape = CutCornerShape(topEnd = 7.dp),
        courseColors = ArknightsCourseColors,
        backgroundRes = backgroundRes,
        logoRes = logoRes,
        fixedDark = true,
    )
}
