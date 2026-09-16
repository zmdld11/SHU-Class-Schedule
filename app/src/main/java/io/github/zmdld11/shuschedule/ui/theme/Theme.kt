package io.github.zmdld11.shuschedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.zmdld11.shuschedule.data.settings.AppTheme
import io.github.zmdld11.shuschedule.data.settings.DarkMode

private val ShuBlue = Color(0xFF1E5AA8)
private val ShuBlueDark = Color(0xFFA8C8F0)

internal val LightScheme = lightColorScheme(
    primary = ShuBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
)

internal val DarkScheme = darkColorScheme(
    primary = ShuBlueDark,
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF10477F),
    onPrimaryContainer = Color(0xFFD6E3FF),
)

internal val ArknightsScheme = darkColorScheme(
    primary = Color(0xFFFFD900),
    onPrimary = Color(0xFF252100),
    primaryContainer = Color(0xFF514800),
    onPrimaryContainer = Color(0xFFFFE877),
    secondary = Color(0xFF71D3F3),
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF174B5C),
    onSecondaryContainer = Color(0xFFB4EAFF),
    tertiary = Color(0xFFD0D6DA),
    onTertiary = Color(0xFF303639),
    tertiaryContainer = Color(0xFF424B50),
    onTertiaryContainer = Color(0xFFE7EEF2),
    background = Color(0xFF151A1D),
    onBackground = Color(0xFFF0F2F3),
    surface = Color(0xFF1C2226),
    onSurface = Color(0xFFF0F2F3),
    surfaceVariant = Color(0xFF343D43),
    onSurfaceVariant = Color(0xFFC2CBD0),
    surfaceTint = Color(0xFFFFD900),
    inverseSurface = Color(0xFFE2E6E8),
    inverseOnSurface = Color(0xFF292F33),
    inversePrimary = Color(0xFF6C5D00),
    outline = Color(0xFF8C989F),
    outlineVariant = Color(0xFF465159),
    surfaceDim = Color(0xFF151A1D),
    surfaceBright = Color(0xFF3B444A),
    surfaceContainerLowest = Color(0xFF101518),
    surfaceContainerLow = Color(0xFF1C2226),
    surfaceContainer = Color(0xFF242C31),
    surfaceContainerHigh = Color(0xFF2C353B),
    surfaceContainerHighest = Color(0xFF343E45),
)

data class CourseColors(val container: Color, val content: Color)

/** Course color indices belong to data; their rendering belongs to the selected theme. */
data class ScheduleStyle(
    val courseShape: Shape = RoundedCornerShape(6.dp),
    val courseColors: List<CourseColors> = DefaultCourseColors,
    /** 主题内置背景图资源（定义侧提供，渲染侧动态引用，主代码不感知具体主题） */
    @androidx.annotation.DrawableRes val backgroundRes: Int? = null,
    /** 主题内置背景图文件路径（外部主题包解出的本地文件，优先级同 backgroundRes） */
    val backgroundPath: String? = null,
    @androidx.annotation.DrawableRes val logoRes: Int? = null,
    /** 固定深色主题（弹窗系统栏图标等场景使用） */
    val fixedDark: Boolean = false,
) {
    fun colorsFor(index: Int): CourseColors = courseColors[Math.floorMod(index, courseColors.size)]
}

internal val DefaultCourseColors = listOf(
    0xFFD7E8FF to 0xFF0D3B7A, 0xFFFFE3EE to 0xFF8A2450,
    0xFFE2F5E1 to 0xFF205C24, 0xFFFFF0CC to 0xFF7A5410,
    0xFFEAE1FF to 0xFF4A2E8F, 0xFFFFE4D6 to 0xFF843B12,
    0xFFDDF4F5 to 0xFF0F5B60, 0xFFF6E0E8 to 0xFF75364E,
).map { (background, foreground) -> CourseColors(Color(background), Color(foreground)) }

internal val ArknightsCourseColors = listOf(
    0xFF354750 to 0xFFBDEBFA, 0xFF514921 to 0xFFFFE781,
    0xFF3B493E to 0xFFCEE7C1, 0xFF504034 to 0xFFFFD4AA,
    0xFF444056 to 0xFFE1D8FF, 0xFF533C40 to 0xFFFFD1D7,
    0xFF304D4D to 0xFFB9ECE6, 0xFF41484E to 0xFFE8EDF1,
).map { (background, foreground) -> CourseColors(Color(background), Color(foreground)) }

val LocalScheduleStyle = staticCompositionLocalOf { ScheduleStyle() }

/** 最终生效的深色状态（主题定义 × 用户深浅色偏好解析后的结果），弹窗系统栏等非 Compose 场景消费 */
val LocalScheduleDark = staticCompositionLocalOf { false }

/** 主题解析统一走 ThemeCatalog（内置 + 外部注册），此处只做装配 */
@Composable
fun ShuScheduleTheme(
    definition: ScheduleThemeDefinition = DefaultThemeDefinition,
    dynamicColor: Boolean = true,
    darkMode: DarkMode = DarkMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    // 用户手动深浅色对固定深色主题无效
    val wantDark = darkMode == DarkMode.DARK || (darkMode == DarkMode.SYSTEM && isSystemInDarkTheme())
    val dark = definition.isDark(wantDark)
    CompositionLocalProvider(
        LocalScheduleStyle provides definition.scheduleStyle,
        LocalScheduleDark provides dark,
    ) {
        MaterialTheme(
            colorScheme = definition.colorScheme(wantDark, dynamicColor),
            shapes = definition.shapes,
            content = content,
        )
    }
}
