package io.github.zmdld11.shuschedule.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.zmdld11.shuschedule.R
import io.github.zmdld11.shuschedule.data.settings.AppTheme
import io.github.zmdld11.shuschedule.ui.theme.LocalScheduleStyle
import io.github.zmdld11.shuschedule.ui.theme.ShuScheduleTheme

@Composable
internal fun ThemePicker(selectedTheme: AppTheme, onSelect: (AppTheme) -> Unit) {
    Column(
        Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("应用主题", style = MaterialTheme.typography.titleMedium)
        AppTheme.entries.forEach { theme ->
            val selected = theme == selectedTheme
            Surface(
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = { onSelect(theme) })) {
                    ThemePreview(theme)
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Column(Modifier.weight(1f)) {
                            Text(theme.title, style = MaterialTheme.typography.titleSmall)
                            Text(theme.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreview(theme: AppTheme) {
    ShuScheduleTheme(theme = theme, dynamicColor = false) {
        Box(Modifier.fillMaxWidth().height(80.dp).background(MaterialTheme.colorScheme.background)) {
            if (theme == AppTheme.ARKNIGHTS) {
                Image(painterResource(R.drawable.arknights_background), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f)))
            }
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (theme == AppTheme.ARKNIGHTS) {
                    Image(painterResource(R.drawable.arknights_rhodes_island), contentDescription = null,
                        modifier = Modifier.size(48.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(if (theme == AppTheme.ARKNIGHTS) "RHODES ISLAND" else "SHU SCHEDULE",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) { index ->
                            Box(Modifier.weight(1f).height(12.dp).clip(LocalScheduleStyle.current.courseShape)
                                .background(LocalScheduleStyle.current.colorsFor(index).container))
                        }
                    }
                }
            }
        }
    }
}
