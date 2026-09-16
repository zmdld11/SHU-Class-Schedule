package io.github.zmdld11.shuschedule.data.settings

/** 内置主题注册表（稳定存储 ID，改名会丢已有偏好）。
 *  外部主题包（.shutheme）不进枚举，直接以字符串 id 参与解析——见 ui/theme/ThemeCatalog。 */
enum class AppTheme(val id: String, val title: String, val description: String) {
    DEFAULT("default", "默认", "上大蓝 · 跟随系统深浅色，可启用壁纸动态取色"),
    ARKNIGHTS("arknights", "明日方舟", "罗德岛终端 · 深色面板、战术黄与工业背景"),
    ;

    fun isDark(systemDark: Boolean): Boolean = this == ARKNIGHTS || systemDark

    fun usesDynamicColor(enabled: Boolean): Boolean = this == DEFAULT && enabled

    companion object {
        fun fromId(id: String?): AppTheme = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** 深色模式偏好：跟随系统 / 手动浅色 / 手动深色（固定深色主题忽略） */
enum class DarkMode(val id: String, val title: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "浅色"),
    DARK("dark", "深色"),
    ;

    companion object {
        fun fromId(id: String?): DarkMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

/** 持久化的外观偏好：themeId 为内置枚举 id 或已导入主题包的 id */
data class AppearanceSettings(
    val themeId: String = AppTheme.DEFAULT.id,
    val dynamicColor: Boolean = true,
    val darkMode: DarkMode = DarkMode.SYSTEM,
)
