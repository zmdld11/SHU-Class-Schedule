package io.github.zmdld11.shuschedule.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.zmdld11.shuschedule.data.settings.AppTheme

/**
 * 单个主题的完整渲染定义：配色方案、形状、课表课程样式。
 *
 * 这是主题体系对外的扩展接口：内置主题在 [ThemeCatalog] 登记；
 * 主题商店/外部主题包将来实现本接口并注册进 [ThemeCatalog.register]，
 * 渲染管线（ShuScheduleTheme / ThemePicker / ScheduleScaffold）无需改动。
 */
interface ScheduleThemeDefinition {
    /** 持久化用稳定 ID 对应的内置枚举；未知来源主题回落 DEFAULT */
    val appTheme: AppTheme

    @Composable
    fun colorScheme(systemDark: Boolean, dynamicColor: Boolean): ColorScheme

    val shapes: Shapes

    val scheduleStyle: ScheduleStyle
}

/** 主题目录：内置主题 + 运行时注册的外部主题（主题商店接入点） */
object ThemeCatalog {

    val builtIn: List<ScheduleThemeDefinition> = listOf(DefaultThemeDefinition, ArknightsThemeDefinition)

    private val external = mutableListOf<ScheduleThemeDefinition>()

    /** 外部主题注册；ID 冲突时忽略后来者（内置优先） */
    fun register(definition: ScheduleThemeDefinition) {
        val ids = builtIn.map { it.appTheme.id } + external.map { it.appTheme.id }
        if (definition.appTheme.id !in ids) external += definition
    }

    fun available(): List<ScheduleThemeDefinition> = builtIn + external

    fun of(theme: AppTheme): ScheduleThemeDefinition =
        available().firstOrNull { it.appTheme == theme } ?: DefaultThemeDefinition
}

/** 默认主题：上大蓝，跟随系统深浅色，Android 12+ 可动态取色 */
object DefaultThemeDefinition : ScheduleThemeDefinition {
    override val appTheme = AppTheme.DEFAULT
    override val shapes = Shapes()

    @Composable
    override fun colorScheme(systemDark: Boolean, dynamicColor: Boolean): ColorScheme {
        val context = LocalContext.current
        return when {
            appTheme.usesDynamicColor(dynamicColor) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            systemDark -> DarkScheme
            else -> LightScheme
        }
    }

    override val scheduleStyle = ScheduleStyle()
}

/** 明日方舟主题：罗德岛终端，固定深色（mashirozx/arknights-ui 风格） */
object ArknightsThemeDefinition : ScheduleThemeDefinition {
    override val appTheme = AppTheme.ARKNIGHTS

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
        theme = AppTheme.ARKNIGHTS,
        courseShape = CutCornerShape(topEnd = 7.dp),
        courseColors = ArknightsCourseColors,
    )
}
