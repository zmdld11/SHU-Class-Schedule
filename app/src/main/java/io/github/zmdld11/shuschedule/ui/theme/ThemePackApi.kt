package io.github.zmdld11.shuschedule.ui.theme

/** 主题包导入结果（纯净版恒为 [ThemeImportResult.NotSupported]） */
sealed interface ThemeImportResult {
    data class Ok(val title: String) : ThemeImportResult
    data class Failed(val reason: String) : ThemeImportResult
    data object NotSupported : ThemeImportResult
}
