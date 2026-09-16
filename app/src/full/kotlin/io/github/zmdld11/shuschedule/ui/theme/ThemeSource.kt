package io.github.zmdld11.shuschedule.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.zmdld11.shuschedule.data.theme.ThemePackStore

/**
 * 完整版主题接缝：额外内置明日方舟主题 + .shutheme 主题包导入能力。
 * 与 src/pure 的同名函数构成变体接缝（main 只调用不定义）。
 */

/** 应用级 context（initExternalThemes 时暂存，供删除主题等无参接缝使用） */
private var appContext: Context? = null

/** 额外内置主题：明日方舟（资源随 full 源集分发） */
internal fun builtInThemes(): List<ScheduleThemeDefinition> = listOf(ArknightsThemeDefinition)

/** 启动时注册已导入的主题包 */
fun initExternalThemes(context: Context) {
    appContext = context.applicationContext
    ThemePackStore.loadAll(context).forEach { ThemeCatalog.register(it) }
}

/** 解析并导入主题包；同 ID 重复导入=覆盖更新（先注销再按新数据注册） */
fun parseThemePack(context: Context, uri: Uri): ThemeImportResult =
    ThemePackStore.import(context, uri).fold(
        onSuccess = { pack ->
            ThemeCatalog.unregister(pack.id)
            ThemePackStore.loadOne(context, pack.id)?.let { ThemeCatalog.register(it) }
            ThemeImportResult.Ok(pack.name)
        },
        onFailure = { ThemeImportResult.Failed(it.message ?: "未知错误") },
    )

/** 删除主题包并注销 */
fun deleteThemePack(id: String) {
    appContext?.let { ThemePackStore.delete(it, id) }
    ThemeCatalog.unregister(id)
}

/** 主题导入入口 UI（完整版） */
@Composable
fun ThemeImportSection(onImport: () -> Unit) {
    OutlinedButton(
        onClick = onImport,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) { Text("导入主题包（.shutheme）") }
}
