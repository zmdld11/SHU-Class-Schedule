package io.github.zmdld11.shuschedule.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 纯净版主题接缝：仅默认内置主题，无外部主题包能力。
 * 与 src/full 的同名函数构成变体接缝（main 只调用不定义）。
 */

/** 额外内置主题：纯净版没有 */
internal fun builtInThemes(): List<ScheduleThemeDefinition> = emptyList()

/** 启动时注册已导入主题包：纯净版无事可做 */
fun initExternalThemes(context: Context) {}

/** 解析主题包：纯净版不支持 */
fun parseThemePack(context: Context, uri: Uri): ThemeImportResult = ThemeImportResult.NotSupported

/** 删除主题包：纯净版无外部主题 */
fun deleteThemePack(id: String) {}

/** 主题导入入口 UI：纯净版不渲染 */
@Composable
fun ThemeImportSection(onImport: () -> Unit) {
}
