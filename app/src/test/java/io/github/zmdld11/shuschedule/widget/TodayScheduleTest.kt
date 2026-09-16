package io.github.zmdld11.shuschedule.widget

import io.github.zmdld11.shuschedule.data.db.Course
import io.github.zmdld11.shuschedule.data.db.CourseSession
import io.github.zmdld11.shuschedule.data.db.DayOverride
import io.github.zmdld11.shuschedule.data.db.CourseWithSessions
import io.github.zmdld11.shuschedule.data.db.Semester
import io.github.zmdld11.shuschedule.data.db.TermType
import io.github.zmdld11.shuschedule.data.db.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TodayScheduleTest {

    private val semester = Semester(
        year = 2026, term = TermType.AUTUMN,
        startDateEpochDay = LocalDate.of(2026, 9, 14).toEpochDay(), // 周一
        totalWeeks = 16,
    )

    private val slots = listOf(
        TimeSlot(1, "08:00", "08:45"), TimeSlot(2, "08:55", "09:40"),
        TimeSlot(7, "15:00", "15:45"), TimeSlot(8, "15:55", "16:40"),
        TimeSlot(9, "18:00", "18:45"), TimeSlot(10, "18:55", "19:40"),
    )

    private fun course(vararg sessions: CourseSession) = CourseWithSessions(
        course = Course(semesterId = 1, name = "测试课", courseCode = "", className = "", classId = "", credit = ""),
        sessions = sessions.toList(),
    )

    private val week1 = CourseSession.maskOf((1..16).toList())

    private val courses = listOf(
        course(
            CourseSession(courseId = 1, weekday = 3, startNode = 1, endNode = 2, weeksMask = week1, room = "A101", teacher = "早课老师"),
            CourseSession(courseId = 1, weekday = 3, startNode = 7, endNode = 8, weeksMask = week1, room = "A202", teacher = "下午老师"),
            CourseSession(courseId = 1, weekday = 3, startNode = 9, endNode = 10, weeksMask = week1, room = "A303", teacher = "晚课老师"),
        ),
    )

    private val wednesday = LocalDate.of(2026, 9, 16)

    @Test
    fun `mid-morning marks first class in progress and keeps it`() {
        val data = TodaySchedule.build(semester, courses, slots, now = wednesday, clock = LocalTime.of(8, 30))
        assertEquals(3, data.items.size)
        assertEquals(3, data.upcomingItems.size)
        assertTrue(data.upcomingItems.first().inProgress)
        assertTrue(data.inClass)
    }

    @Test
    fun `afternoon drops ended morning class and marks afternoon in progress`() {
        val data = TodaySchedule.build(semester, courses, slots, now = wednesday, clock = LocalTime.of(16, 0))
        assertEquals(2, data.upcomingItems.size) // 7-8节正上，1-2节已剔除
        assertFalse(data.upcomingItems.any { it.name == "测试课" && it.startNode == 1 })
        assertTrue(data.upcomingItems.first { it.startNode == 7 }.inProgress)
    }

    @Test
    fun `evening shows only upcoming and day not yet done`() {
        val data = TodaySchedule.build(semester, courses, slots, now = wednesday, clock = LocalTime.of(17, 0))
        assertEquals(1, data.upcomingItems.size) // 只剩 9-10 节
        assertEquals(9, data.upcomingItems.single().startNode)
    }

    @Test
    fun `all classes over keeps items but upcoming empty`() {
        val data = TodaySchedule.build(semester, courses, slots, now = wednesday, clock = LocalTime.of(20, 0))
        assertEquals(3, data.items.size)
        assertEquals(0, data.upcomingItems.size)
        assertEquals(null, data.nextIndex)
    }

    @Test
    fun `blank end time items are never dropped`() {
        val noSlots = emptyList<TimeSlot>()
        val data = TodaySchedule.build(semester, courses, noSlots, now = wednesday, clock = LocalTime.of(23, 0))
        assertEquals(3, data.upcomingItems.size) // 时间未知 → 不剔除
    }

    @Test
    fun `holiday override yields empty day and label`() {
        val overrides = listOf(
            DayOverride(semesterId = 1, week = 1, weekday = 3, mode = DayOverride.MODE_HOLIDAY),
        )
        val data = TodaySchedule.build(semester, courses, slots, overrides, now = wednesday, clock = LocalTime.of(9, 0))
        assertEquals(0, data.items.size)
        assertEquals(0, data.upcomingItems.size)
        assertTrue(data.weekLabel.contains("假期"))
    }

    @Test
    fun `substitute override renders source weekday sessions`() {
        // 周六（weekday=6）没有排课记录；设为按周三上 → 应渲染周三的三节课
        val overrides = listOf(
            DayOverride(semesterId = 1, week = 1, weekday = 6, mode = DayOverride.MODE_SUBSTITUTE, substituteWeekday = 3),
        )
        val saturday = LocalDate.of(2026, 9, 19)
        val data = TodaySchedule.build(semester, courses, slots, overrides, now = saturday, clock = LocalTime.of(8, 0))
        assertEquals(3, data.items.size)
        assertTrue(data.weekLabel.contains("按周三上"))
    }

    @Test
    fun `cross-week substitute renders source week sessions only`() {
        // 开学 9/14：9/20 为第 1 周周日，10/6 为第 4 周周二。
        // 「9/20 补 10/6 的课」→ 只显示第 4 周周二才有的课，其他周的周二课不显示
        val tuesdayCourses = listOf(
            course(CourseSession(courseId = 1, weekday = 2, startNode = 1, endNode = 2, weeksMask = CourseSession.maskOf(listOf(4)), room = "补课教室", teacher = "第4周老师")),
            course(CourseSession(courseId = 1, weekday = 2, startNode = 7, endNode = 8, weeksMask = CourseSession.maskOf(listOf(2)), room = "第2周教室", teacher = "第2周老师")),
        )
        val overrides = listOf(
            DayOverride(semesterId = 1, week = 1, weekday = 7, mode = DayOverride.MODE_SUBSTITUTE, substituteWeekday = 2, sourceWeek = 4),
        )
        val sunday = LocalDate.of(2026, 9, 20)
        val data = TodaySchedule.build(semester, tuesdayCourses, slots, overrides, now = sunday, clock = LocalTime.of(8, 0))
        assertEquals(1, data.items.size)
        assertEquals("补课教室", data.items.single().place)
        assertTrue(data.weekLabel.contains("补10/6的课"))
    }

    @Test
    fun `source week equal to current week behaves as same-week substitute`() {
        // sourceWeek=当天所在周时与不设来源周等价，标签仍走「按周X上」
        val tuesdayCourses = listOf(
            course(CourseSession(courseId = 1, weekday = 2, startNode = 1, endNode = 2, weeksMask = week1, room = "T1", teacher = "t")),
        )
        val overrides = listOf(
            DayOverride(semesterId = 1, week = 1, weekday = 7, mode = DayOverride.MODE_SUBSTITUTE, substituteWeekday = 2, sourceWeek = 1),
        )
        val sunday = LocalDate.of(2026, 9, 20)
        val data = TodaySchedule.build(semester, tuesdayCourses, slots, overrides, now = sunday, clock = LocalTime.of(8, 0))
        assertEquals(1, data.items.size)
        assertTrue(data.weekLabel.contains("按周二上"))
    }

    @Test
    fun `override outside current week is ignored`() {
        val overrides = listOf(
            DayOverride(semesterId = 1, week = 2, weekday = 3, mode = DayOverride.MODE_HOLIDAY),
        )
        val data = TodaySchedule.build(semester, courses, slots, overrides, now = wednesday, clock = LocalTime.of(9, 0))
        assertEquals(3, data.items.size)
    }

    private val wedThuCourses = listOf(
        course(CourseSession(courseId = 1, weekday = 3, startNode = 1, endNode = 2, weeksMask = week1, room = "A101", teacher = "早")),
        course(CourseSession(courseId = 1, weekday = 4, startNode = 7, endNode = 8, weeksMask = week1, room = "B202", teacher = "明")),
    )

    @Test
    fun `all done today switches to tomorrow preview`() {
        val data = TodaySchedule.build(semester, wedThuCourses, slots, now = wednesday, clock = LocalTime.of(20, 0))
        assertTrue(data.forTomorrow)
        assertEquals(1, data.items.size)              // 明天(周四)的 7-8 节
        assertEquals(7, data.items.single().startNode)
        assertEquals(0, data.nextIndex)
        assertTrue(data.weekLabel.startsWith("明天"))
        assertFalse(data.inClass)
    }

    @Test
    fun `tomorrow holiday keeps today ended state`() {
        val overrides = listOf(DayOverride(semesterId = 1, week = 1, weekday = 4, mode = DayOverride.MODE_HOLIDAY))
        val data = TodaySchedule.build(semester, wedThuCourses, slots, overrides, now = wednesday, clock = LocalTime.of(20, 0))
        assertFalse(data.forTomorrow)
        assertEquals(null, data.nextIndex)
        assertEquals(0, data.upcomingItems.size)
    }

    @Test
    fun `tomorrow without classes keeps today ended state`() {
        // 原始 fixture 只有周三课，周四无课
        val data = TodaySchedule.build(semester, courses, slots, now = wednesday, clock = LocalTime.of(20, 0))
        assertFalse(data.forTomorrow)
        assertEquals(0, data.upcomingItems.size)
    }

    @Test
    fun `last day of semester does not peek beyond semester`() {
        val lastDayCourses = listOf(
            course(CourseSession(courseId = 1, weekday = 7, startNode = 1, endNode = 2, weeksMask = week1, room = "A101", teacher = "末")),
        )
        val lastSunday = LocalDate.of(2027, 1, 3) // 第16周周日
        val data = TodaySchedule.build(semester, lastDayCourses, slots, now = lastSunday, clock = LocalTime.of(20, 0))
        assertFalse(data.forTomorrow)
        assertEquals(0, data.upcomingItems.size)
    }
}
