package io.github.zmdld11.shuschedule.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.zmdld11.shuschedule.R
import io.github.zmdld11.shuschedule.data.db.CourseSession
import io.github.zmdld11.shuschedule.data.db.CourseWithSessions
import io.github.zmdld11.shuschedule.ui.theme.LocalScheduleStyle
import io.github.zmdld11.shuschedule.ui.theme.ScheduleScaffold
import io.github.zmdld11.shuschedule.ui.theme.ThemeDialogSystemBars
import io.github.zmdld11.shuschedule.data.db.DayOverride
import io.github.zmdld11.shuschedule.data.repo.resolveSubstitutePlan
import java.time.LocalDate
import java.time.YearMonth

private val CELL_HEIGHT = 52.dp
internal val DAY_CHARS = listOf("一", "二", "三", "四", "五", "六", "日")

/** 列头单行文字：列窄或系统字体放大时自动缩字号保持单行，不折行不截断 */
@Composable
private fun HeaderSingleLine(
    text: String,
    color: Color,
    fontWeight: FontWeight? = null,
) {
    val style = MaterialTheme.typography.labelSmall
    var shrink by remember(text) { mutableStateOf(1f) }
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        style = style,
        fontSize = (style.fontSize.value * shrink).sp,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { if (it.didOverflowWidth && shrink > 0.55f) shrink -= 0.1f },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    onImport: () -> Unit,
    onSettings: () -> Unit,
    onSemesters: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val scheduleStyle = LocalScheduleStyle.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedWeek by viewModel.selectedWeek.collectAsStateWithLifecycle()
    val detail by viewModel.detailCourse.collectAsStateWithLifecycle()
    val editorTarget by viewModel.editorTarget.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val showOffWeek by viewModel.showOffWeek.collectAsStateWithLifecycle()
    val showWeekend by viewModel.showWeekend.collectAsStateWithLifecycle()
    val showSlotEnd by viewModel.showSlotEnd.collectAsStateWithLifecycle()
    val backgroundPath by viewModel.scheduleBackgroundPath.collectAsStateWithLifecycle()

    val currentWeek = state.currentWeek
    // 纯 Compose 派生：selectedWeek 只经追踪的 State 读，避免原始 Flow.value 读取与重组时序分歧
    val week = selectedWeek ?: currentWeek
    var showJumpDialog by remember { mutableStateOf(false) }
    var deletingCourse by remember { mutableStateOf<CourseWithSessions?>(null) }
    var dayOverrideDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }


    val semester = state.semester

    ScheduleScaffold(
        backgroundPath = backgroundPath,
        topBar = {
            var semesterMenu by remember { mutableStateOf(false) }
            TopAppBar(
                title = {
                    Column(
                        Modifier.clickable { semesterMenu = true },
                    ) {
                        Text(
                            semester?.displayName ?: "上大课表",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (semester == null) "去导入第一张课表吧" else "第 $week / ${semester.totalWeeks} 周${if (week == currentWeek) " · 本周" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DropdownMenu(expanded = semesterMenu, onDismissRequest = { semesterMenu = false }) {
                            semesters.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.displayName) },
                                    trailingIcon = if (s.isActive) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else null,
                                    onClick = {
                                        viewModel.activateSemester(s.id)
                                        semesterMenu = false
                                    },
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("管理学期…") },
                                onClick = {
                                    semesterMenu = false
                                    onSemesters()
                                },
                            )
                        }
                    }
                },
                navigationIcon = {
                    scheduleStyle.logoRes?.let { logo ->
                        Image(
                            painterResource(logo),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 12.dp, end = 8.dp).size(32.dp),
                        )
                    }
                },
                actions = {
                    if (semester != null) {
                        IconButton(onClick = viewModel::openNewCourseEditor) {
                            Icon(Icons.Filled.Add, contentDescription = "添加课程")
                        }
                    }
                    IconButton(onClick = onSemesters) {
                        Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = "学期管理")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) {
        if (semester == null) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("还没有课表", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "从上海大学教务系统一键导入",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onImport) { Text("从教务导入课表") }
            }
            return@ScheduleScaffold
        }

        Column(Modifier.fillMaxSize()) {
            // 周切换条
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { viewModel.selectWeek((week - 1).coerceAtLeast(1)) },
                    enabled = week > 1,
                ) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                Text(
                    "第 $week 周",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showJumpDialog = true },
                    textAlign = TextAlign.Center,
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = week != currentWeek,
                    enter = androidx.compose.animation.expandHorizontally() + fadeIn(),
                    exit = androidx.compose.animation.shrinkHorizontally() + fadeOut(),
                ) {
                    TextButton(onClick = { viewModel.selectWeek(null) }) { Text("回本周") }
                }
                TextButton(
                    onClick = { viewModel.selectWeek((week + 1).coerceAtMost(semester.totalWeeks)) },
                    enabled = week < semester.totalWeeks,
                ) { Text("›", style = MaterialTheme.typography.titleLarge) }
            }

            // 表头 + 网格（切周滑动动画：前进周从右滑入，后退周从左滑入）
            val today = LocalDate.now()
            val nodeCount = maxOf(state.timeSlots.size, state.courses.maxOfOrNull { c -> c.sessions.maxOfOrNull { it.endNode } ?: 0 } ?: 0, 10)
            // 左滑下一周 / 右滑上一周（阈值防误触；垂直滚动不受影响）
            val weekNow = rememberUpdatedState(week)
            val totalWeeksNow = semester.totalWeeks
            val gridScroll = rememberScrollState()
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(totalWeeksNow) {
                        var acc = 0f
                        val threshold = 40.dp.toPx()
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (acc <= -threshold) {
                                    viewModel.selectWeek((weekNow.value + 1).coerceAtMost(totalWeeksNow))
                                } else if (acc >= threshold) {
                                    viewModel.selectWeek((weekNow.value - 1).coerceAtLeast(1))
                                }
                                acc = 0f
                            },
                            onDragCancel = { acc = 0f },
                        ) { _, dragAmount -> acc += dragAmount }
                    },
            ) {
                AnimatedContent(
                    targetState = week,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { if (forward) -it else it } + fadeOut())
                    },
                    label = "weekTransition",
                ) { w ->
                    Column(Modifier.fillMaxSize()) {
                        val weekOverride = state.dayOverrides[w].orEmpty()
                        // 周末列默认隐藏；本周六/日有调休覆盖时自动显示该列
                        val visibleDays = (1..5) + (6..7).filter { showWeekend || weekOverride.containsKey(it) }

                        // 表头：星期 + 日期（长按设置该天调休/放假）
                        Row(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.width(40.dp))
                            visibleDays.forEach { wd ->
                                val date = state.dateOf(w, wd)
                                val isToday = date == today
                                val override = weekOverride[wd]
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .padding(vertical = 2.dp)
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = { dayOverrideDialog = w to wd },
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        DAY_CHARS[wd - 1],
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isToday) FontWeight.Bold else null,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    HeaderSingleLine(
                                        text = date?.let { "${it.monthValue}/${it.dayOfMonth}" } ?: "",
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (override != null) {
                                        val subDate = override.sourceWeek
                                            ?.takeIf { it != w }
                                            ?.let { state.dateOf(it, override.substituteWeekday) }
                                        HeaderSingleLine(
                                            text = when {
                                                override.mode == DayOverride.MODE_HOLIDAY -> "休"
                                                subDate != null -> "补${subDate.monthValue}/${subDate.dayOfMonth}"
                                                else -> "班·周${DAY_CHARS[override.substituteWeekday - 1]}"
                                            },
                                            color = if (override.mode == DayOverride.MODE_HOLIDAY) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.tertiary
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxSize().verticalScroll(gridScroll)) {
                // 左侧节次时间列
                Column(Modifier.width(40.dp)) {
                    repeat(nodeCount) { i ->
                        val slot = state.timeSlots.getOrNull(i)
                        Column(
                            Modifier.height(CELL_HEIGHT).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall)
                            Text(
                                slot?.startTime ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (showSlotEnd) {
                                Text(
                                    slot?.endTime ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                // 课程列（默认工作日；调休到周末的周自动加列）
                visibleDays.forEach { weekday ->
                    val override = weekOverride[weekday]
                    // 调休差异底色：放假=主色淡洗 / 调休上课=强调色淡洗，一眼可辨
                    val overrideTint = when (override?.mode) {
                        DayOverride.MODE_HOLIDAY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                        DayOverride.MODE_SUBSTITUTE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.09f)
                        else -> androidx.compose.ui.graphics.Color.Transparent
                    }
                    if (override?.mode == DayOverride.MODE_HOLIDAY) {
                        // 放假：不排课，居中轻提示
                        Box(
                            Modifier
                                .weight(1f)
                                .height(CELL_HEIGHT * nodeCount)
                                .background(overrideTint),
                        ) {
                            Text(
                                "放假",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    } else {
                        // 调休列按来源星期的课表渲染；跨周补课（sourceWeek）按来源教学周取课
                        val substitute = override?.takeIf { it.mode == DayOverride.MODE_SUBSTITUTE }
                        val effectiveWeekday = substitute?.substituteWeekday ?: weekday
                        val effectiveWeek = substitute?.sourceWeek ?: w
                        // 调休列只显示实际要上的课（来源周当天的课表）；置灰的未来课对补课日无意义
                        val blocks = viewModel.blocksFor(effectiveWeek, effectiveWeekday, showOffWeek && substitute == null)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(CELL_HEIGHT * nodeCount)
                            .background(overrideTint),
                    ) {
                        blocks.forEach { block ->
                            val courseColors = scheduleStyle.colorsFor(block.course.course.colorIndex)
                            val span = block.session.endNode - block.session.startNode + 1
                            Box(
                                Modifier
                                    .offset(y = CELL_HEIGHT * (block.session.startNode - 1))
                                    .fillMaxWidth()
                                    .height(CELL_HEIGHT * span - 2.dp)
                                    .padding(horizontal = 1.dp)
                                    .alpha(if (block.inWeek) 1f else 0.35f)
                                    .clip(scheduleStyle.courseShape)
                                    .background(courseColors.container)
                                    .clickable { viewModel.showDetail(block.course) }
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                            ) {
                                Column {
                                    Text(
                                        block.course.course.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = courseColors.content,
                                        maxLines = if (span >= 2) 2 else 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (span >= 2) {
                                        // 教室/教师/校区各占一行：合行时教室会被省略号吃掉
                                        if (block.session.room.isNotBlank()) {
                                            Text(
                                                "@${block.session.room}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                lineHeight = 11.sp,
                                                color = courseColors.content,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        if (block.session.teacher.isNotBlank()) {
                                            Text(
                                                block.session.teacher,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                lineHeight = 11.sp,
                                                color = courseColors.content,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        if (block.session.campus.isNotBlank()) {
                                            Text(
                                                block.session.campus,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                lineHeight = 11.sp,
                                                color = courseColors.content,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        if (block.session.rescheduled) {
                                            // 调课徽标：描边小标签，与正文文字区分
                                            Text(
                                                "调课",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.sp,
                                                lineHeight = 10.sp,
                                                color = courseColors.content,
                                                modifier = Modifier
                                                    .padding(top = 1.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .border(
                                                        0.75.dp,
                                                        courseColors.content.copy(alpha = 0.6f),
                                                        RoundedCornerShape(3.dp),
                                                    )
                                                    .padding(horizontal = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                        }
                    }
                }
            }
            }
            }
                }

    // 课程详情
    detail?.let { course ->
        ModalBottomSheet(onDismissRequest = { viewModel.showDetail(null) }) {
            ThemeDialogSystemBars()
            CourseDetailContent(
                course = course,
                currentWeek = week,
                onEditSession = { s -> viewModel.openSessionEditor(course.course, s) },
                onAddSession = { viewModel.openSessionEditor(course.course, null) },
                onReschedule = { s -> viewModel.openSessionEditor(course.course, s, reschedule = true) },
                onDeleteCourse = { deletingCourse = course },
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            )
        }
    }

    // 课程/时段编辑
    editorTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = viewModel::closeEditor) {
            ThemeDialogSystemBars()
            SessionEditorSheet(
                courseName = target.course.name,
                initialSession = target.session,
                slotCount = maxOf(state.timeSlots.size, 12),
                isNewCourse = target.course.id == 0L,
                initialColorIndex = target.course.colorIndex,
                rescheduleMode = target.reschedule,
                currentWeek = currentWeek,
                onSave = { nm, wd, sn, en, wt, r, t, cp, ci ->
                    viewModel.saveSessionEdit(nm, wd, sn, en, wt, r, t, cp, ci)
                },
                onSaveReschedule = { w, wd, sn, en, r, t, cp ->
                    viewModel.saveReschedule(w, wd, sn, en, r, t, cp)
                },
                onDeleteSession = if (target.session != null && !target.reschedule) viewModel::deleteEditingSession else null,
                onDismiss = viewModel::closeEditor,
            )
        }
    }

    // 发现新版本（应用内下载，未授权安装时引导设置，可回退浏览器）
    var pendingInstall by remember { mutableStateOf<io.github.zmdld11.shuschedule.data.update.UpdateChecker.ReleaseInfo?>(null) }
    updateInfo?.let { info ->
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdate,
            title = { Text("发现新版本 ${info.tagName}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(info.body.ifBlank { info.name }, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissUpdate()
                    io.github.zmdld11.shuschedule.ui.update.UpdateActions.startDownload(context, info) { pendingInstall = it }
                }) { Text("下载更新") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissUpdate) { Text("稍后") } },
        )
    }
    io.github.zmdld11.shuschedule.ui.update.InstallPermissionDialog(info = pendingInstall, onDismiss = { pendingInstall = null })

    // 删除整门课确认
    deletingCourse?.let { c ->
        AlertDialog(
            onDismissRequest = { deletingCourse = null },
            title = { Text("删除 ${c.course.name}？") },
            text = { Text("该课全部 ${c.sessions.size} 个时段将一并删除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCourse(c.course.id)
                    deletingCourse = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingCourse = null }) { Text("取消") } },
        )
    }

    // 调休/放假设置（长按列头触发）
    dayOverrideDialog?.let { (dw, dd) ->
        DayOverrideDialog(
            week = dw,
            weekday = dd,
            totalWeeks = semester?.totalWeeks ?: 16,
            startEpochDay = semester?.startDateEpochDay ?: LocalDate.now().toEpochDay(),
            existing = state.overrideOf(dw, dd),
            dateOfSource = { sw, sd -> state.dateOf(sw, sd) },
            onSave = { mode, anchorWeek, anchorWeekday, srcWeek, srcWeekday ->
                viewModel.setDayOverride(anchorWeek, anchorWeekday, mode, srcWeekday, srcWeek.takeIf { mode == DayOverride.MODE_SUBSTITUTE })
                dayOverrideDialog = null
            },
            onClear = {
                viewModel.setDayOverride(dw, dd, -1)
                dayOverrideDialog = null
            },
            onDismiss = { dayOverrideDialog = null },
        )
    }

    // 跳周对话框
    if (showJumpDialog && semester != null) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("取消") }
            },
            title = { Text("跳转到周次") },
            text = {
                Column {
                    repeat(semester.totalWeeks) { i ->
                        val w = i + 1
                        TextButton(
                            onClick = {
                                viewModel.selectWeek(w)
                                showJumpDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "第 $w 周${if (w == currentWeek) "（本周）" else ""}",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun CourseDetailContent(
    course: CourseWithSessions,
    currentWeek: Int,
    onEditSession: (CourseSession) -> Unit,
    onAddSession: () -> Unit,
    onReschedule: (CourseSession) -> Unit,
    onDeleteCourse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(course.course.name, style = MaterialTheme.typography.titleLarge)
        Text(
            listOfNotNull(
                course.course.className.takeIf { it.isNotBlank() },
                "课程号 ${course.course.courseCode}".takeIf { course.course.courseCode.isNotBlank() },
                course.course.credit.takeIf { it.isNotBlank() }?.let { "${it} 学分" },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        course.sessions.sortedWith(compareBy({ it.weekday }, { it.startNode })).forEach { s ->
            SessionRow(
                session = s,
                currentWeek = currentWeek,
                onClick = { onEditSession(s) },
                onReschedule = if (CourseSession.weeksOf(s.weeksMask).size >= 2) {
                    { onReschedule(s) }
                } else null,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAddSession) { Text("添加时段") }
            TextButton(
                onClick = onDeleteCourse,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("删除整门课") }
        }
    }
}

@Composable
private fun SessionRow(
    session: CourseSession,
    currentWeek: Int,
    onClick: () -> Unit,
    onReschedule: (() -> Unit)? = null,
) {
    val hasThisWeek = session.hasWeek(currentWeek)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "周${DAY_CHARS[session.weekday - 1]} 第${session.startNode}-${session.endNode}节",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (session.rescheduled) {
                    Text(
                        "调课",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                listOfNotNull(
                    formatWeeks(CourseSession.weeksOf(session.weeksMask)),
                    session.campus.takeIf { it.isNotBlank() },
                    session.room.takeIf { it.isNotBlank() },
                    session.teacher.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (hasThisWeek) "本周有课" else "本周无课",
            style = MaterialTheme.typography.labelMedium,
            color = if (hasThisWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        if (onReschedule != null) {
            TextButton(onClick = onReschedule) { Text("调课", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

/** 周次集合 → 压缩文本：{1..8}→"1-8周"；全奇/全偶→"2-16周(双)"；杂散→"1,5,9周" */
internal fun formatWeeks(weeks: Set<Int>): String {    if (weeks.isEmpty()) return "全学期"
    val sorted = weeks.sorted()
    if (sorted.size >= 3) {
        val allOdd = sorted.all { it % 2 == 1 }
        val allEven = sorted.all { it % 2 == 0 }
        if ((allOdd || allEven) && (sorted.last() - sorted.first()) / 2 + 1 == sorted.size) {
            val tag = if (allOdd) "单" else "双"
            return "${sorted.first()}-${sorted.last()}周($tag)"
        }
    }
    val parts = mutableListOf<String>()
    var start = sorted.first()
    var prev = start
    for (w in sorted.drop(1)) {
        if (w == prev + 1) {
            prev = w
        } else {
            parts += if (start == prev) "${start}周" else "${start}-${prev}周"
            start = w
            prev = w
        }
    }
    parts += if (start == prev) "${start}周" else "${start}-${prev}周"
    return parts.joinToString(",")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOverrideDialog(
    week: Int,
    weekday: Int,
    totalWeeks: Int,
    startEpochDay: Long,
    existing: DayOverride?,
    dateOfSource: (week: Int, weekday: Int) -> LocalDate?,
    onSave: (mode: Int, anchorWeek: Int, anchorWeekday: Int, sourceWeek: Int, sourceWeekday: Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by remember(existing) {
        mutableStateOf(existing?.mode?.takeIf { it >= 0 } ?: -1) // -1 = 未选（默认正常）
    }
    // 调休方向：0=本日上所选日期的课；1=所选日期上本日的课（周末列隐藏时从工作日列反向设置）
    var direction by remember(existing) { mutableStateOf(0) }
    // 选课日期完全决定周次与星期：existing 有值回显其日期，否则从今天开始
    var pickedDate by remember(existing) {
        mutableStateOf(
            existing?.let { e ->
                (e.sourceWeek ?: week).let { sw -> dateOfSource(sw, e.substituteWeekday.takeIf { it in 1..7 } ?: weekday) }
            } ?: LocalDate.now()
        )
    }
    var displayedMonth by remember(existing) { mutableStateOf(YearMonth.from(pickedDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("第 $week 周 · 周${DAY_CHARS[weekday - 1]} 调休") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("设置这一天的上课安排：", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode < 0 || mode == -2, onClick = { mode = -2 }, label = { Text("正常上课") })
                    FilterChip(
                        selected = mode == DayOverride.MODE_HOLIDAY,
                        onClick = { mode = DayOverride.MODE_HOLIDAY },
                        label = { Text("放假") },
                    )
                    FilterChip(
                        selected = mode == DayOverride.MODE_SUBSTITUTE,
                        onClick = { mode = DayOverride.MODE_SUBSTITUTE },
                        label = { Text("调休上课") },
                    )
                }
                if (mode == DayOverride.MODE_SUBSTITUTE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = direction == 0,
                            onClick = { direction = 0 },
                            label = { Text("本日按所选日期上课") },
                        )
                        FilterChip(
                            selected = direction == 1,
                            onClick = { direction = 1 },
                            label = { Text("所选日期按本日上课") },
                        )
                    }
                    val pickedWeek = ((pickedDate.toEpochDay() - startEpochDay) / 7 + 1).toInt()
                    val inRange = pickedWeek in 1..totalWeeks
                    val sameDay = inRange && pickedWeek == week && pickedDate.dayOfWeek.value == weekday
                    Text(
                        when {
                            !inRange -> "所选日期不在本学期内，请重新选择"
                            sameDay -> "所选日期就是这一天本身，请另选"
                            direction == 0 -> "这一天按 ${pickedDate.monthValue}/${pickedDate.dayOfMonth}（第 $pickedWeek 周周${DAY_CHARS[pickedDate.dayOfWeek.value - 1]}）的课表上课"
                            else -> "${pickedDate.monthValue}/${pickedDate.dayOfMonth}（第 $pickedWeek 周周${DAY_CHARS[pickedDate.dayOfWeek.value - 1]}）按这一天（周${DAY_CHARS[weekday - 1]}）的课表上课"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (inRange && !sameDay) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    )
                    CompactMonthPicker(
                        displayedMonth = displayedMonth,
                        selected = pickedDate,
                        today = LocalDate.now(),
                        onMonthChange = { displayedMonth = it },
                        onPick = { pickedDate = it },
                        inSemester = { d -> ((d.toEpochDay() - startEpochDay) / 7 + 1).toInt() in 1..totalWeeks },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (mode) {
                    -2 -> onClear()          // 显式选了正常上课 = 清除覆盖
                    DayOverride.MODE_HOLIDAY -> onSave(DayOverride.MODE_HOLIDAY, week, weekday, 0, 0)
                    DayOverride.MODE_SUBSTITUTE -> {
                        val pickedWeek = ((pickedDate.toEpochDay() - startEpochDay) / 7 + 1).toInt()
                        if (pickedWeek in 1..totalWeeks) {
                            // 学期外日期在月历上不可点，自指（所选=本日）不落库——提示文案已在上方给出
                            resolveSubstitutePlan(direction, week, weekday, pickedWeek, pickedDate.dayOfWeek.value)?.let { plan ->
                                onSave(DayOverride.MODE_SUBSTITUTE, plan.anchorWeek, plan.anchorWeekday, plan.sourceWeek, plan.sourceWeekday)
                            }
                        }
                    }
                    else -> onClear()
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/**
 * 紧凑月历：M3 DatePicker 无法缩小尺寸，弹窗里放不下整月——自绘 7 列小月历（34dp 行高），
 * 学期范围外日期置灰不可点，选中的日期实底高亮，今天以主色标出。
 */
@Composable
private fun CompactMonthPicker(
    displayedMonth: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onPick: (LocalDate) -> Unit,
    inSemester: (LocalDate) -> Boolean,
) {
    val leadingBlanks = displayedMonth.atDay(1).dayOfWeek.value - 1 // 周一=0
    val cells = List(leadingBlanks) { null } + (1..displayedMonth.lengthOfMonth()).map { displayedMonth.atDay(it) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onMonthChange(displayedMonth.minusMonths(1)) }) { Text("‹") }
            Text("${displayedMonth.year}年${displayedMonth.monthValue}月", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { onMonthChange(displayedMonth.plusMonths(1)) }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            DAY_CHARS.forEach { c ->
                Text(
                    c,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { rowDays ->
            Row(Modifier.fillMaxWidth()) {
                rowDays.forEach { d ->
                    if (d == null) {
                        Spacer(Modifier.weight(1f).height(34.dp))
                    } else {
                        val isSelected = d == selected
                        val enabled = inSemester(d)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .then(if (enabled) Modifier.clickable { onPick(d) } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${d.dayOfMonth}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    d == today -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
                repeat(7 - rowDays.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
