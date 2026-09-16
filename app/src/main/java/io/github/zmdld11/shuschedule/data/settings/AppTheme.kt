package io.github.zmdld11.shuschedule.data.settings

/** Stable storage IDs: renaming an enum entry must not reset an existing preference. */
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

data class AppearanceSettings(
    val theme: AppTheme = AppTheme.DEFAULT,
    val dynamicColor: Boolean = true,
)
