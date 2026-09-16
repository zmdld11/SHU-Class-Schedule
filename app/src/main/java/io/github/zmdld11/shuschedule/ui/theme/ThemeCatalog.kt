package io.github.zmdld11.shuschedule.ui.theme

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.zmdld11.shuschedule.data.settings.AppTheme

/**
 * 单个主题的完整渲染定义：配色方案、形状、课表课程样式、桌面小组件配色、可选背景/标识图。
 *
 * 这是主题体系对外的扩展接口：内置主题在 [ThemeCatalog] 登记；
 * 主题商店/外部主题包（.shutheme）解析成 [ScheduleThemeDefinition] 后
 * 经 [ThemeCatalog.register] 注册，渲染管线（ShuScheduleTheme / ThemePicker /
 * ScheduleScaffold / 小组件）无需改动。主题=纯数据，不加载任何代码。
 */
interface ScheduleThemeDefinition {
    /** 稳定 ID（内置=AppTheme.id；主题包=pack id） */
    val id: String
    val title: String
    val description: String

    /** 固定深色主题返回 true（忽略系统深浅色与手动深浅色切换） */
    fun isDark(systemDark: Boolean): Boolean

    /** 深浅色是否锁定（isDark 与入参无关） */
    val fixedDark: Boolean get() = isDark(false) == isDark(true)

    /** 动态取色仅默认主题允许 */
    fun usesDynamicColor(enabled: Boolean): Boolean

    /** 主题内置课表背景图（如罗德岛工业背景）；自选壁纸优先于它 */
    @get:DrawableRes val backgroundRes: Int?

    /** 标题栏标识图（可选） */
    @get:DrawableRes val logoRes: Int?

    /** 桌面小组件配色（RemoteViews 侧按此覆盖默认观感） */
    val widget: WidgetThemeColors

    @Composable
    fun colorScheme(systemDark: Boolean, dynamicColor: Boolean): ColorScheme

    val shapes: Shapes

    val scheduleStyle: ScheduleStyle
}

/** 小组件四组颜色的开放定义（ARGB，RemoteViews 侧直接消费） */
data class WidgetThemeColors(
    val rootBackground: Long = 0xE61E3A5F, // 深蓝 90% 不透明（现有默认观感）
    val itemBackground: Long = 0x24FFFFFF,
    val textPrimary: Long = 0xFFFFFFFF,
    val textSecondary: Long = 0xB3FFFFFF,
)

/** 主题目录：内置主题 + 运行时注册的外部主题（主题商店接入点） */
object ThemeCatalog {

    val builtIn: List<ScheduleThemeDefinition> =
        listOf(DefaultThemeDefinition) + builtInThemes()

    private val external = mutableListOf<ScheduleThemeDefinition>()

    /** 外部主题注册；ID 冲突时忽略后来者（内置优先） */
    fun register(definition: ScheduleThemeDefinition) {
        val ids = builtIn.map { it.id } + external.map { it.id }
        if (definition.id !in ids) external += definition
    }

    fun unregister(id: String) {
        external.removeAll { it.id == id }
    }

    val externalThemes: List<ScheduleThemeDefinition> get() = external.toList()

    fun available(): List<ScheduleThemeDefinition> = builtIn + external

    /** id → 定义；内置与外部都未命中时回落默认（含所选主题包被删除的情形） */
    fun resolve(id: String?): ScheduleThemeDefinition =
        available().firstOrNull { it.id == id } ?: DefaultThemeDefinition
}

/** 默认主题：上大蓝，跟随系统深浅色，Android 12+ 可动态取色 */
object DefaultThemeDefinition : ScheduleThemeDefinition {
    override val id = AppTheme.DEFAULT.id
    override val title = AppTheme.DEFAULT.title
    override val description = AppTheme.DEFAULT.description
    override val backgroundRes: Int? = null
    override val logoRes: Int? = null
    override val widget = WidgetThemeColors()

    override fun isDark(systemDark: Boolean) = AppTheme.DEFAULT.isDark(systemDark)
    override fun usesDynamicColor(enabled: Boolean) = AppTheme.DEFAULT.usesDynamicColor(enabled)

    override val shapes = Shapes()

    @Composable
    override fun colorScheme(systemDark: Boolean, dynamicColor: Boolean): ColorScheme {
        val context = LocalContext.current
        return when {
            usesDynamicColor(dynamicColor) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            systemDark -> DarkScheme
            else -> LightScheme
        }
    }

    override val scheduleStyle = ScheduleStyle()
}

