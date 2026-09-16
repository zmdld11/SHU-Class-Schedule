package io.github.zmdld11.shuschedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.zmdld11.shuschedule.data.db.CourseSession
import io.github.zmdld11.shuschedule.data.parser.WeekTextParser
import io.github.zmdld11.shuschedule.ui.theme.LocalScheduleStyle

/**
 * 课程/时段编辑表单：新建自定义课程、给已有课程加时段、改任意字段。
 * 周次文本走 WeekTextParser 语法（"1-16周"、"2-16周(单)"…），实时预览解析结果。
 * rescheduleMode：单周调休——选一个原本上课的周，单独改那周的时间/地点/教师。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorSheet(
    courseName: String,
    initialSession: CourseSession?,
    slotCount: Int,
    isNewCourse: Boolean,
    initialColorIndex: Int = 0,
    rescheduleMode: Boolean = false,
    currentWeek: Int = 1,
    onSave: (
        name: String,
        weekday: Int,
        startNode: Int,
        endNode: Int,
        weeksText: String,
        room: String,
        teacher: String,
        campus: String,
        colorIndex: Int,
    ) -> Unit,
    onSaveReschedule: ((week: Int, weekday: Int, startNode: Int, endNode: Int, room: String, teacher: String, campus: String) -> Unit)? = null,
    onDeleteSession: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(courseName) }
    var colorIndex by remember { mutableStateOf(initialColorIndex) }
    var weekday by remember { mutableStateOf(initialSession?.weekday ?: 1) }
    var startNode by remember { mutableStateOf(initialSession?.startNode ?: 1) }
    var endNode by remember { mutableStateOf(initialSession?.endNode ?: 2) }
    // 调休模式：单周选择；普通模式：周次文本
    var rescheduleWeek by remember {
        mutableStateOf(
            (currentWeek.takeIf { initialSession?.hasWeek(it) == true }
                ?: CourseSession.weeksOf(initialSession?.weeksMask ?: 0).minOrNull()
                ?: 1).toString()
        )
    }
    var weeksText by remember {
        mutableStateOf(
            initialSession?.let { formatWeeks(CourseSession.weeksOf(it.weeksMask)) } ?: "1-16周"
        )
    }
    var room by remember { mutableStateOf(initialSession?.room.orEmpty()) }
    var teacher by remember { mutableStateOf(initialSession?.teacher.orEmpty()) }
    var campus by remember { mutableStateOf(initialSession?.campus.orEmpty()) }

    val maxNode = maxOf(slotCount, endNode)
    val weeksPreview = formatWeeks(WeekTextParser.parseWeeks(weeksText))
    val rescheduleWeekValid = rescheduleWeek.toIntOrNull()
        ?.let { initialSession?.hasWeek(it) == true } == true
    val valid = if (rescheduleMode) {
        rescheduleWeekValid && startNode <= endNode
    } else {
        name.isNotBlank() && startNode <= endNode
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            when {
                rescheduleMode -> "调课（第 $rescheduleWeek 周）"
                isNewCourse -> "添加课程"
                initialSession == null -> "添加时段"
                else -> "编辑时段"
            },
            style = MaterialTheme.typography.titleLarge,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("课程名") },
            singleLine = true,
            readOnly = rescheduleMode,
            supportingText = if (rescheduleMode) {
                { Text("调课不改课程名") }
            } else if (name.isBlank()) {
                { Text("课程名不能为空") }
            } else null,
        )

        Text("星期", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..7).forEach { d ->
                FilterChip(
                    selected = weekday == d,
                    onClick = { weekday = d },
                    label = { Text(DAY_CHARS[d - 1]) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NodeDropdown(
                label = "开始",
                value = startNode,
                range = 1..maxNode,
                onChange = { startNode = it },
            )
            NodeDropdown(
                label = "结束",
                value = endNode,
                range = 1..maxNode,
                onChange = { endNode = it },
            )
            if (startNode > endNode) {
                Text(
                    "开始节需 ≤ 结束节",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (rescheduleMode) {
            OutlinedTextField(
                value = rescheduleWeek,
                onValueChange = { rescheduleWeek = it.filter(Char::isDigit).take(2) },
                label = { Text("调课周次") },
                singleLine = true,
                isError = !rescheduleWeekValid,
                supportingText = {
                    Text(
                        if (rescheduleWeekValid) "该时段第 $rescheduleWeek 周原本有课，将单独调整这一周"
                        else "这一周该时段没有课"
                    )
                },
            )
        } else {
            OutlinedTextField(
                value = weeksText,
                onValueChange = { weeksText = it },
                label = { Text("周次") },
                singleLine = true,
                supportingText = { Text("将保存为：$weeksPreview") },
            )
        }

        OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("教室") }, singleLine = true)
        OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("教师") }, singleLine = true)
        OutlinedTextField(value = campus, onValueChange = { campus = it }, label = { Text("校区（可空）") }, singleLine = true)

        // 课程颜色（整门课统一；调休模式无意义不显示）
        if (!rescheduleMode) {
            val colors = LocalScheduleStyle.current
            Text("课程颜色：", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (0 until colors.courseColors.size).forEach { i ->
                    val selected = colorIndex == i
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.colorsFor(i).container)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(50),
                            )
                            .clickable { colorIndex = i },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (rescheduleMode) {
                        onSaveReschedule?.invoke(
                            rescheduleWeek.toIntOrNull() ?: 1,
                            weekday,
                            startNode,
                            endNode,
                            room.trim(),
                            teacher.trim(),
                            campus.trim(),
                        )
                    } else {
                        onSave(name.trim(), weekday, startNode, endNode, weeksText, room.trim(), teacher.trim(), campus.trim(), colorIndex)
                    }
                },
                enabled = valid,
            ) { Text(if (rescheduleMode) "保存调课" else "保存") }
            OutlinedButton(onClick = onDismiss) { Text("取消") }
            if (!rescheduleMode && onDeleteSession != null) {
                TextButton(onClick = onDeleteSession) { Text("删除该时段") }
            }
        }
        if (rescheduleMode) {
            Text(
                "原时段其余周次不变，仅所选这一周按上面的新时间/地点上课（带调课标记）；撤销可在编辑时段里改回",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (onDeleteSession != null) {
            Text(
                "删除该学期的最后一个时段会连课程一起删除",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NodeDropdown(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$label 第 $value 节")
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            range.forEach { n ->
                DropdownMenuItem(
                    text = { Text("第 $n 节") },
                    onClick = {
                        onChange(n)
                        expanded = false
                    },
                )
            }
        }
    }
}
