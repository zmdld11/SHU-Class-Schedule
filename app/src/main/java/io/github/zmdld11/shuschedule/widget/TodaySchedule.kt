package io.github.zmdld11.shuschedule.widget

import io.github.zmdld11.shuschedule.data.db.CourseSession
import io.github.zmdld11.shuschedule.data.db.DayOverride
import io.github.zmdld11.shuschedule.data.db.CourseWithSessions
import io.github.zmdld11.shuschedule.data.db.Semester
import io.github.zmdld11.shuschedule.data.db.TimeSlot
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 小组件用的"今日课程"快照 */
data class TodayItem(
    val name: String,
    val startNode: Int,
    val endNode: Int,
    val startTime: String,
    val endTime: String,
    val place: String,   // 校区·教室
    val teacher: String,
    /** 该节正在上课中（当前时间落在其起止内） */
    val inProgress: Boolean = false,
)

data class TodayData(
    val semesterName: String,
    val week: Int,
    val weekLabel: String,   // "第3周 · 周三 9/16"；预览明天时以「明天 · …」开头
    val items: List<TodayItem>,
    /** 未结束的课程（已下课的剔除，列表小组件往上顶显示） */
    val upcomingItems: List<TodayItem>,
    /** items 中"接下来"那节的下标（正在上=该节；已全结束=null）；无课=null */
    val nextIndex: Int?,
    val inClass: Boolean,
    /** 今日课全上完、已切换为明天的课表预览 */
    val forTomorrow: Boolean = false,
)

object TodaySchedule {

    private val DAY_CHARS = listOf("一", "二", "三", "四", "五", "六", "日")

    fun build(
        semester: Semester?,
        courses: List<CourseWithSessions>,
        timeSlots: List<TimeSlot>,
        overrides: List<DayOverride> = emptyList(),
        now: LocalDate = LocalDate.now(),
        clock: LocalTime = LocalTime.now(),
    ): TodayData {
        if (semester == null) {
            return TodayData("", 0, "未导入课表", emptyList(), emptyList(), null, inClass = false)
        }
        val slots = timeSlots.associateBy { it.node }

        fun itemsOf(date: LocalDate): Triple<Int, Boolean, List<TodayItem>> {
            // returns (week, isHoliday, items)，学期外返回空
            val days = date.toEpochDay() - semester.startDateEpochDay
            val week = ((days / 7) + 1).toInt()
            val inSemester = days >= 0 && week <= semester.totalWeeks
            if (!inSemester) return Triple(week.coerceIn(1, semester.totalWeeks), false, emptyList())
            val weekday = date.dayOfWeek.value
            val override = overrides.firstOrNull { it.week == week && it.weekday == weekday }
            val holiday = override?.mode == DayOverride.MODE_HOLIDAY
            val substitute = override?.takeIf { it.mode == DayOverride.MODE_SUBSTITUTE && it.substituteWeekday in 1..7 }
            val effectiveWeekday = substitute?.substituteWeekday ?: weekday
            // 跨周补课（如 9/20 补第 5 周的课）按来源周取课，单双周/分段周次过滤才正确
            val effectiveWeek = substitute?.sourceWeek ?: week
            val items = if (holiday) {
                emptyList()
            } else {
                courses
                    .flatMap { c -> c.sessions.filter { it.weekday == effectiveWeekday && it.hasWeek(effectiveWeek) }.map { c to it } }
                    .sortedWith(compareBy({ (_, s) -> s.startNode }, { (c, _) -> c.course.name }))
                    .map { (c, s) ->
                        TodayItem(
                            name = c.course.name,
                            startNode = s.startNode,
                            endNode = s.endNode,
                            startTime = slots[s.startNode]?.startTime ?: "",
                            endTime = slots[s.endNode]?.endTime ?: "",
                            place = listOf(s.campus.takeIf { it.isNotBlank() }, s.room.takeIf { it.isNotBlank() })
                                .filterNotNull().joinToString("·"),
                            teacher = s.teacher,
                        )
                    }
            }
            return Triple(week, holiday, items)
        }

        fun labelOf(date: LocalDate, week: Int, prefix: String, overridesApplied: DayOverride?): String = buildString {
            append(prefix)
            append("第${week}周 · 周${DAY_CHARS[date.dayOfWeek.value - 1]} ${date.monthValue}/${date.dayOfMonth}")
            if (overridesApplied?.mode == DayOverride.MODE_HOLIDAY) append(" · 假期")
            if (overridesApplied?.mode == DayOverride.MODE_SUBSTITUTE) {
                val src = overridesApplied.sourceWeek
                if (src != null && src != week) {
                    val srcDate = LocalDate.ofEpochDay(semester.startDateEpochDay + (src - 1) * 7L + overridesApplied.substituteWeekday - 1)
                    append(" · 补${srcDate.monthValue}/${srcDate.dayOfMonth}的课")
                } else {
                    append(" · 按周${DAY_CHARS[overridesApplied.substituteWeekday - 1]}上")
                }
            }
        }

        val week = ((now.toEpochDay() - semester.startDateEpochDay) / 7 + 1).toInt().coerceIn(1, semester.totalWeeks)
        val weekday = now.dayOfWeek.value
        val todayOverride = overrides.firstOrNull { it.week == week && it.weekday == weekday }
        val isHoliday = todayOverride?.mode == DayOverride.MODE_HOLIDAY

        val (todayWeek, _, items) = itemsOf(now)

        val hhmm = DateTimeFormatter.ofPattern("HH:mm")
        val nowStr = clock.format(hhmm)
        var nextIndex: Int? = null
        var inClass = false
        if (!isHoliday) {
            for ((i, item) in items.withIndex()) {
                if (item.endTime.isBlank()) continue
                when {
                    nowStr <= item.startTime -> { nextIndex = i; inClass = false; break }
                    nowStr <= item.endTime -> { nextIndex = i; inClass = true; break }
                }
            }
        }

        val marked = items.mapIndexed { i, item -> item.copy(inProgress = i == nextIndex && inClass) }
        val upcoming = marked.filter { it.endTime.isBlank() || nowStr <= it.endTime }

        // 今天有课且全部上完（非假期）→ 明天还在学期内且有课时，预览明天的课表
        if (!isHoliday && marked.isNotEmpty() && upcoming.isEmpty()) {
            val tomorrow = now.plusDays(1)
            val (tWeek, tHoliday, tItems) = itemsOf(tomorrow)
            if (!tHoliday && tItems.isNotEmpty()) {
                val tOverride = overrides.firstOrNull { it.week == tWeek && it.weekday == tomorrow.dayOfWeek.value }
                return TodayData(
                    semesterName = semester.displayName,
                    week = tWeek,
                    weekLabel = labelOf(tomorrow, tWeek, "明天 · ", tOverride),
                    items = tItems,
                    upcomingItems = tItems,
                    nextIndex = 0,
                    inClass = false,
                    forTomorrow = true,
                )
            }
        }

        return TodayData(
            semesterName = semester.displayName,
            week = todayWeek,
            weekLabel = labelOf(now, todayWeek, "", todayOverride),
            items = marked,
            upcomingItems = upcoming,
            nextIndex = nextIndex,
            inClass = inClass,
        )
    }
}
