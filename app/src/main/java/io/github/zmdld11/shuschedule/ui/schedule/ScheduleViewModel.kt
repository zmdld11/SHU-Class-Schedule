package io.github.zmdld11.shuschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zmdld11.shuschedule.BuildConfig
import io.github.zmdld11.shuschedule.data.db.Course
import io.github.zmdld11.shuschedule.data.db.CourseSession
import io.github.zmdld11.shuschedule.data.db.CourseWithSessions
import io.github.zmdld11.shuschedule.data.db.DayOverride
import io.github.zmdld11.shuschedule.data.db.Semester
import io.github.zmdld11.shuschedule.data.db.TermType
import io.github.zmdld11.shuschedule.data.db.TimeSlot
import io.github.zmdld11.shuschedule.data.parser.WeekTextParser
import io.github.zmdld11.shuschedule.data.repo.ScheduleRepository
import io.github.zmdld11.shuschedule.data.settings.SettingsStore
import io.github.zmdld11.shuschedule.data.update.UpdateCheckClient
import io.github.zmdld11.shuschedule.data.update.UpdateChecker
import io.github.zmdld11.shuschedule.widget.WidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ScheduleUiState(
    val semester: Semester? = null,
    val courses: List<CourseWithSessions> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    /** 调休覆盖：week → (weekday → override) */
    val dayOverrides: Map<Int, Map<Int, DayOverride>> = emptyMap(),
) {
    val currentWeek: Int
        get() {
            val s = semester ?: return 1
            val days = LocalDate.now().toEpochDay() - s.startDateEpochDay
            return ((days / 7) + 1).toInt().coerceIn(1, s.totalWeeks)
        }

    /** 第 week 周第 weekday 天的日期 */
    fun dateOf(week: Int, weekday: Int): LocalDate? {
        val s = semester ?: return null
        return LocalDate.ofEpochDay(s.startDateEpochDay + (week - 1) * 7L + (weekday - 1))
    }

    fun overrideOf(week: Int, weekday: Int): DayOverride? = dayOverrides[week]?.get(weekday)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
    private val repository: ScheduleRepository,
    private val widgetUpdater: WidgetUpdater,
    private val updateClient: UpdateCheckClient,
    settings: SettingsStore,
) : ViewModel() {

    /** 课表背景图路径（未设置或文件丢失为 null） */
    val scheduleBackgroundPath: StateFlow<String?> =
        settings.scheduleBackgroundEnabled
            .map { enabled ->
                val f = java.io.File(context.filesDir, "schedule_background.jpg")
                if (enabled && f.exists()) f.absolutePath else null
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // 启动自动检查更新（开关可关；开了也每天最多一次，失败静默）
        viewModelScope.launch {
            if (!settings.autoUpdateCheck.first()) return@launch
            val today = java.time.LocalDate.now().toEpochDay().toInt()
            val last = settings.lastUpdateCheckDay.first()
            if (today - last >= 1) {
                settings.setLastUpdateCheckDay(today)
                val latest = updateClient.fetchLatest() ?: return@launch
                if (UpdateChecker.isNewer(BuildConfig.VERSION_NAME, latest.versionName)) {
                    _updateInfo.value = latest
                }
            }
        }
    }

    /** 有新版本时的弹窗数据 */
    private val _updateInfo = MutableStateFlow<UpdateChecker.ReleaseInfo?>(null)
    val updateInfo: StateFlow<UpdateChecker.ReleaseInfo?> = _updateInfo.asStateFlow()

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    /** 周视图是否置灰显示非本周课程 */
    val showOffWeek: StateFlow<Boolean> =
        settings.showOffWeek.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 周视图是否显示周六周日（默认只显示工作日） */
    val showWeekend: StateFlow<Boolean> =
        settings.showWeekend.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 节次时间列是否显示下课时间 */
    val showSlotEnd: StateFlow<Boolean> =
        settings.showSlotEnd.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _semesterFlow = repository.observeActiveSemester()

    /** 顶栏下拉展示的学期：默认隐藏冬季（教务查不到数据），激活学期始终可见 */
    val semesters: StateFlow<List<Semester>> =
        kotlinx.coroutines.flow.combine(repository.observeSemesters(), settings.showWinter) { list, showWinter ->
            if (showWinter) list else list.filter { it.isActive || it.term != TermType.WINTER }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<ScheduleUiState> = _semesterFlow
        .flatMapLatest { semester ->
            if (semester == null) {
                flowOf(ScheduleUiState())
            } else {
                kotlinx.coroutines.flow.combine(
                    repository.observeCourses(semester.id),
                    repository.observeTimeSlots(),
                    repository.observeDayOverrides(semester.id),
                ) { courses, slots, overrides ->
                    ScheduleUiState(
                        semester = semester,
                        courses = courses,
                        timeSlots = slots,
                        dayOverrides = overrides.groupBy({ it.week }, { it }).mapValues { (_, list) ->
                            list.associateBy { it.weekday }
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    val selectedWeek = MutableStateFlow<Int?>(null) // null = 跟随当前周

    private val _detailCourse = MutableStateFlow<CourseWithSessions?>(null)
    val detailCourse: StateFlow<CourseWithSessions?> = _detailCourse.asStateFlow()


    fun selectWeek(week: Int?) {
        selectedWeek.value = week?.let { it.coerceAtLeast(1) }
    }

    /** 顶栏快捷切换学期：整个 App 与小组件跟随激活学期 */
    fun activateSemester(id: Long) {
        viewModelScope.launch {
            repository.activateSemester(id)
            widgetUpdater.pushAll()
            selectedWeek.value = null
        }
    }

    fun showDetail(course: CourseWithSessions?) {
        _detailCourse.value = course
    }

    // ---------- 课程手动编辑 ----------

    /** 编辑目标：session=null 表示给该课新增时段；course.id=0 表示新建自定义课程；reschedule=true 为调休模式 */
    data class SessionEditTarget(
        val course: Course,
        val session: CourseSession?,
        val reschedule: Boolean = false,
    )

    private val _editorTarget = MutableStateFlow<SessionEditTarget?>(null)
    val editorTarget: StateFlow<SessionEditTarget?> = _editorTarget.asStateFlow()

    fun openSessionEditor(course: Course, session: CourseSession?, reschedule: Boolean = false) {
        _detailCourse.value = null
        _editorTarget.value = SessionEditTarget(course, session, reschedule)
    }

    fun openNewCourseEditor() {
        val semester = state.value.semester ?: return
        _editorTarget.value = SessionEditTarget(
            course = Course(
                semesterId = semester.id,
                name = "",
                courseCode = "自定义",
                className = "",
                classId = "",
                credit = "",
            ),
            session = null,
        )
    }

    fun closeEditor() {
        _editorTarget.value = null
    }

    fun saveSessionEdit(
        name: String,
        weekday: Int,
        startNode: Int,
        endNode: Int,
        weeksText: String,
        room: String,
        teacher: String,
        campus: String,
        colorIndex: Int = -1,
    ) {
        val target = _editorTarget.value ?: return
        val weeksMask = WeekTextParser.parseMask(weeksText)
        viewModelScope.launch {
            runCatching {
                when {
                    target.session == null && target.course.id == 0L ->
                        repository.addCustomCourse(
                            semesterId = target.course.semesterId,
                            name = name,
                            colorIndex = colorIndex.takeIf { it >= 0 },
                            session = CourseSession(
                                courseId = 0,
                                weekday = weekday,
                                startNode = startNode,
                                endNode = endNode,
                                weeksMask = weeksMask,
                                room = room,
                                teacher = teacher,
                                campus = campus,
                            ),
                        )

                    target.session == null ->
                        repository.addSession(
                            course = target.course.copy(name = name, colorIndex = colorIndex.takeIf { it >= 0 } ?: target.course.colorIndex),
                            session = CourseSession(
                                courseId = target.course.id,
                                weekday = weekday,
                                startNode = startNode,
                                endNode = endNode,
                                weeksMask = weeksMask,
                                room = room,
                                teacher = teacher,
                                campus = campus,
                            ),
                        )

                    else ->
                        repository.saveSessionEdit(
                            course = target.course.copy(name = name, colorIndex = colorIndex.takeIf { it >= 0 } ?: target.course.colorIndex),
                            session = target.session.copy(
                                weekday = weekday,
                                startNode = startNode,
                                endNode = endNode,
                                weeksMask = weeksMask,
                                room = room,
                                teacher = teacher,
                                campus = campus,
                            ),
                        )
                }
            }.onSuccess {
                widgetUpdater.pushAll()
                _editorTarget.value = null
            }
        }
    }

    /** 删除编辑中的时段（课程因此无时段则整门课删除） */
    fun deleteEditingSession() {
        val target = _editorTarget.value ?: return
        val session = target.session ?: return
        viewModelScope.launch {
            repository.deleteSessionAndOrphanCourse(session)
            widgetUpdater.pushAll()
            _editorTarget.value = null
        }
    }

    /** 调休保存：原时段拆掉该周，新增单周记录（带调课标记） */
    fun saveReschedule(
        week: Int,
        weekday: Int,
        startNode: Int,
        endNode: Int,
        room: String,
        teacher: String,
        campus: String,
    ) {
        val target = _editorTarget.value ?: return
        val session = target.session ?: return
        if (!session.hasWeek(week)) return
        viewModelScope.launch {
            runCatching {
                repository.rescheduleSession(
                    original = session,
                    week = week,
                    newSession = CourseSession(
                        courseId = session.courseId,
                        weekday = weekday,
                        startNode = startNode,
                        endNode = endNode,
                        weeksMask = 1 shl (week - 1),
                        room = room,
                        teacher = teacher,
                        campus = campus,
                        rescheduled = true,
                    ),
                )
            }.onSuccess {
                widgetUpdater.pushAll()
                _editorTarget.value = null
            }
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            widgetUpdater.pushAll()
            _detailCourse.value = null
        }
    }

    /** 某周某天的课程块：CourseSession + 对应 Course；inWeek=该周是否上这节课 */
    data class DayBlock(
        val course: CourseWithSessions,
        val session: CourseSession,
        val inWeek: Boolean,
    )

    fun blocksFor(week: Int, weekday: Int, includeOffWeek: Boolean = false): List<DayBlock> =
        filterBlocks(state.value.courses, week, weekday, includeOffWeek)

    /** 设置/清除某天的调休覆盖（mode<0=恢复正常），写入后同步小组件 */
    fun setDayOverride(week: Int, weekday: Int, mode: Int, substituteWeekday: Int = 0, sourceWeek: Int? = null) {
        val semester = state.value.semester ?: return
        viewModelScope.launch {
            repository.setDayOverride(semester.id, week, weekday, mode, substituteWeekday, sourceWeek)
            widgetUpdater.pushAll()
        }
    }
}

/** 纯函数便于单测：按周/星期过滤排课块
 *
 * 非本周显示（includeOffWeek=true）的槽位级规则：
 * - 本周该时段有课 → 只显示本周的课（非本周的同槽位一律不显示，避免叠块）
 * - 本周该时段无课 → 置灰显示最近一次未来出现的课（调课周自然显示调课记录）
 * - 该时段之后再也没有课 → 不显示
 */
fun filterBlocks(
    courses: List<CourseWithSessions>,
    week: Int,
    weekday: Int,
    includeOffWeek: Boolean,
): List<ScheduleViewModel.DayBlock> {
    val inWeek = mutableListOf<ScheduleViewModel.DayBlock>()
    val offBySlot = mutableMapOf<Pair<Int, Int>, MutableList<ScheduleViewModel.DayBlock>>()
    courses.forEach { c ->
        c.sessions.filter { it.weekday == weekday }.forEach { s ->
            val block = ScheduleViewModel.DayBlock(c, s, s.hasWeek(week))
            if (block.inWeek) {
                inWeek += block
            } else if (includeOffWeek) {
                offBySlot.getOrPut(s.startNode to s.endNode) { mutableListOf() } += block
            }
        }
    }
    val occupiedSlots = inWeek.map { it.session.startNode to it.session.endNode }.toSet()
    val nearestFuturePerEmptySlot = offBySlot
        .filterKeys { it !in occupiedSlots }
        .mapNotNull { (_, blocks) ->
            blocks.mapNotNull { b ->
                CourseSession.weeksOf(b.session.weeksMask).filter { it > week }.minOrNull()
                    ?.let { nextWeek -> b to nextWeek }
            }.minByOrNull { it.second }?.first
        }
    return (inWeek + nearestFuturePerEmptySlot)
        .sortedWith(compareBy({ it.session.startNode }, { it.course.course.name }))
}
