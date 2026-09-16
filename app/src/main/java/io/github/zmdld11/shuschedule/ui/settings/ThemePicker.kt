package io.github.zmdld11.shuschedule.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.zmdld11.shuschedule.ui.theme.ScheduleThemeDefinition
import io.github.zmdld11.shuschedule.ui.theme.ThemeCatalog
import io.github.zmdld11.shuschedule.ui.theme.ThemeImportSection
import io.github.zmdld11.shuschedule.ui.theme.deleteThemePack

/**
 * 主题列表（主题商店入口）：内置主题 + 已导入主题包，全部来自 ThemeCatalog。
 * 导入入口与包解析按变体注入——纯净版渲染空导入区、解析恒失败。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ThemePicker(
    selectedId: String,
    onSelect: (String) -> Unit,
    onImport: () -> Unit,
) {
    val definitions = ThemeCatalog.available()
    val builtInIds = ThemeCatalog.builtIn.map { it.id }.toSet()
    var deleting by remember { mutableStateOf<ScheduleThemeDefinition?>(null) }

    Column(
        Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("应用主题", style = MaterialTheme.typography.titleMedium)
        definitions.forEach { def ->
            val selected = def.id == selectedId
            val external = def.id !in builtInIds
            Surface(
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onSelect(def.id) },
                            onLongClick = if (external) ({ deleting = def }) else null,
                        ),
                ) {
                    ThemePreview(def)
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column(Modifier.weight(1f)) {
                            Text(def.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                def.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (external) {
                            Text(
                                "导入",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }
        }
        // 导入入口（纯净版此区为空）
        ThemeImportSection(onImport = onImport)
    }

    deleting?.let { def ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除主题「${def.title}」？") },
            text = { Text("将从本机移除该主题包；若正在使用会自动切回默认主题。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteThemePack(def.id)
                    onSelect(ThemeCatalog.resolve(null).id) // 选中项被删时回落默认
                    deleting = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun ThemePreview(def: ScheduleThemeDefinition) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxWidth().height(80.dp)) {
            def.backgroundRes?.let {
                Image(
                    painterResource(it), contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)))
            }
            Row(
                Modifier.matchParentSize().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                def.logoRes?.let {
                    Image(painterResource(it), contentDescription = null, modifier = Modifier.size(48.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        def.title.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        def.scheduleStyle.courseColors.take(4).forEach { color ->
                            Box(
                                Modifier.weight(1f).height(12.dp).clip(def.scheduleStyle.courseShape)
                                    .background(color.container),
                            )
                        }
                    }
                }
                Box(
                    Modifier.size(12.dp).clip(CircleShape).background(Color(def.widget.textSecondary.toInt())),
                )
            }
        }
    }
}
