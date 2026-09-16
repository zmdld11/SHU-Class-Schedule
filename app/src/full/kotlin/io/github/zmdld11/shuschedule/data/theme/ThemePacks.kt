package io.github.zmdld11.shuschedule.data.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.zmdld11.shuschedule.ui.theme.CourseColors
import io.github.zmdld11.shuschedule.ui.theme.DefaultCourseColors
import io.github.zmdld11.shuschedule.ui.theme.ScheduleStyle
import io.github.zmdld11.shuschedule.ui.theme.ScheduleThemeDefinition
import io.github.zmdld11.shuschedule.ui.theme.WidgetThemeColors
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * .shutheme 主题包的数据模型与解析（纯 JVM 可单测）。
 * 包 = zip{ theme.json, background.jpg(可选) }，纯数据不含任何代码。
 */
@Serializable
data class ThemePack(
    val id: String,
    val name: String,
    val description: String = "",
    val version: Int = 1,
    val author: String = "",
    /** 固定深色：true 时忽略系统深浅色与动态取色 */
    val fixedDark: Boolean = true,
    val colors: PackColors = PackColors(),
    /** 课程色板 [["#容器色", "#文字色"], ...]，4-16 组取模使用；缺省用默认色板 */
    val courseColors: List<List<String>> = emptyList(),
    val shape: PackShape = PackShape(),
    val widget: PackWidgetColors? = null,
)

@Serializable
data class PackColors(
    val primary: String? = null,
    val onPrimary: String? = null,
    val primaryContainer: String? = null,
    val onPrimaryContainer: String? = null,
    val secondary: String? = null,
    val onSecondary: String? = null,
    val secondaryContainer: String? = null,
    val onSecondaryContainer: String? = null,
    val tertiary: String? = null,
    val onTertiary: String? = null,
    val tertiaryContainer: String? = null,
    val onTertiaryContainer: String? = null,
    val background: String? = null,
    val onBackground: String? = null,
    val surface: String? = null,
    val onSurface: String? = null,
    val surfaceVariant: String? = null,
    val onSurfaceVariant: String? = null,
    val outline: String? = null,
    val error: String? = null,
    val onError: String? = null,
)

@Serializable
data class PackShape(val type: String = "rounded", val radius: Int = 6)

@Serializable
data class PackWidgetColors(
    val rootBg: String? = null,
    val itemBg: String? = null,
    val textPrimary: String? = null,
    val textSecondary: String? = null,
)

object ThemePacks {

    private val json = Json { ignoreUnknownKeys = true }

    val ID_REGEX = Regex("^[a-z0-9_-]{1,32}$")

    /** 解析 + 校验；失败返回原因（中文，直接给用户看） */
    fun parse(themeJson: String): Result<ThemePack> = runCatching {
        val pack = json.decodeFromString<ThemePack>(themeJson)
        pack.validate()
    }

    fun ThemePack.validate(): ThemePack {
        require(ID_REGEX.matches(id)) { "ID 只能是小写字母/数字/-/_（1-32 位）" }
        require(name.isNotBlank()) { "name 不能为空" }
        with(colors) {
            listOfNotNull(
                primary, onPrimary, primaryContainer, onPrimaryContainer,
                secondary, onSecondary, secondaryContainer, onSecondaryContainer,
                tertiary, onTertiary, tertiaryContainer, onTertiaryContainer,
                background, onBackground, surface, onSurface,
                surfaceVariant, onSurfaceVariant, outline, error, onError,
            ).forEach { ThemePacks.parseColor(it) }
        }
        require(courseColors.all { it.size == 2 }) { "courseColors 每组需为 [容器色, 文字色]" }
        courseColors.forEach { pair -> pair.forEach { ThemePacks.parseColor(it) } }
        require(shape.type == "rounded" || shape.type == "cut") { "shape.type 只支持 rounded / cut" }
        require(shape.radius in 0..24) { "shape.radius 需在 0-24" }
        widget?.let { w ->
            listOfNotNull(w.rootBg, w.itemBg, w.textPrimary, w.textSecondary).forEach { ThemePacks.parseColor(it) }
        }
        return this
    }

    /** "#RGB/#RRGGBB/#AARRGGBB" → ARGB Long */
    fun parseColor(text: String): Long {
        val hex = text.removePrefix("#")
        val argb = when (hex.length) {
            6 -> "FF$hex"
            8 -> hex
            else -> throw IllegalArgumentException("颜色格式错误：$text（需 #RRGGBB 或 #AARRGGBB）")
        }
        return argb.toLongOrNull(16) ?: throw IllegalArgumentException("颜色格式错误：$text")
    }
}

/** 由主题包数据构造的运行时主题定义（导入即注册进 ThemeCatalog） */
class DynamicThemeDefinition(
    private val pack: ThemePack,
    private val backgroundPath: String? = null,
) : ScheduleThemeDefinition {

    override val id = pack.id
    override val title = pack.name
    override val description = pack.description.ifBlank { "导入的主题包${if (pack.author.isNotBlank()) " · ${pack.author}" else ""}" }

    override fun isDark(systemDark: Boolean) = pack.fixedDark || systemDark
    override fun usesDynamicColor(enabled: Boolean) = false // 外部主题一律固定配色
    override val backgroundRes: Int? = null
    override val logoRes: Int? = null

    override val widget: WidgetThemeColors = WidgetThemeColors(
        rootBackground = pack.widget?.rootBg?.let { ThemePacks.parseColor(it) } ?: 0xE61E3A5F,
        itemBackground = pack.widget?.itemBg?.let { ThemePacks.parseColor(it) } ?: 0x24FFFFFF,
        textPrimary = pack.widget?.textPrimary?.let { ThemePacks.parseColor(it) } ?: 0xFFFFFFFF,
        textSecondary = pack.widget?.textSecondary?.let { ThemePacks.parseColor(it) } ?: 0xB3FFFFFF,
    )

    private val scheme: ColorScheme = run {
        val base = if (pack.fixedDark) darkColorScheme() else lightColorScheme()
        val c = pack.colors
        base.copy(
            primary = c.primary?.let { Color(ThemePacks.parseColor(it)) } ?: base.primary,
            onPrimary = c.onPrimary?.let { Color(ThemePacks.parseColor(it)) } ?: base.onPrimary,
            primaryContainer = c.primaryContainer?.let { Color(ThemePacks.parseColor(it)) } ?: base.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer?.let { Color(ThemePacks.parseColor(it)) } ?: base.onPrimaryContainer,
            secondary = c.secondary?.let { Color(ThemePacks.parseColor(it)) } ?: base.secondary,
            onSecondary = c.onSecondary?.let { Color(ThemePacks.parseColor(it)) } ?: base.onSecondary,
            secondaryContainer = c.secondaryContainer?.let { Color(ThemePacks.parseColor(it)) } ?: base.secondaryContainer,
            onSecondaryContainer = c.onSecondaryContainer?.let { Color(ThemePacks.parseColor(it)) } ?: base.onSecondaryContainer,
            tertiary = c.tertiary?.let { Color(ThemePacks.parseColor(it)) } ?: base.tertiary,
            onTertiary = c.onTertiary?.let { Color(ThemePacks.parseColor(it)) } ?: base.onTertiary,
            tertiaryContainer = c.tertiaryContainer?.let { Color(ThemePacks.parseColor(it)) } ?: base.tertiaryContainer,
            onTertiaryContainer = c.onTertiaryContainer?.let { Color(ThemePacks.parseColor(it)) } ?: base.onTertiaryContainer,
            background = c.background?.let { Color(ThemePacks.parseColor(it)) } ?: base.background,
            onBackground = c.onBackground?.let { Color(ThemePacks.parseColor(it)) } ?: base.onBackground,
            surface = c.surface?.let { Color(ThemePacks.parseColor(it)) } ?: base.surface,
            onSurface = c.onSurface?.let { Color(ThemePacks.parseColor(it)) } ?: base.onSurface,
            surfaceVariant = c.surfaceVariant?.let { Color(ThemePacks.parseColor(it)) } ?: base.surfaceVariant,
            onSurfaceVariant = c.onSurfaceVariant?.let { Color(ThemePacks.parseColor(it)) } ?: base.onSurfaceVariant,
            outline = c.outline?.let { Color(ThemePacks.parseColor(it)) } ?: base.outline,
            error = c.error?.let { Color(ThemePacks.parseColor(it)) } ?: base.error,
            onError = c.onError?.let { Color(ThemePacks.parseColor(it)) } ?: base.onError,
        )
    }

    @Composable
    override fun colorScheme(systemDark: Boolean, dynamicColor: Boolean): ColorScheme = scheme

    private val cornerShape = if (pack.shape.type == "cut") {
        CutCornerShape(pack.shape.radius.dp)
    } else {
        RoundedCornerShape(pack.shape.radius.dp)
    }

    override val shapes = Shapes(
        extraSmall = cornerShape,
        small = cornerShape,
        medium = cornerShape,
        large = cornerShape,
        extraLarge = cornerShape,
    )

    private val packCourseColors: List<CourseColors> =
        if (pack.courseColors.isEmpty()) DefaultCourseColors
        else pack.courseColors.map { (container, content) ->
            CourseColors(Color(ThemePacks.parseColor(container)), Color(ThemePacks.parseColor(content)))
        }

    override val scheduleStyle = ScheduleStyle(
        courseShape = cornerShape,
        courseColors = packCourseColors,
        backgroundPath = backgroundPath,
        fixedDark = pack.fixedDark,
    )
}
