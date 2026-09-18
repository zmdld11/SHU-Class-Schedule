package io.github.zmdld11.shuschedule.data.backup

import io.github.zmdld11.shuschedule.data.db.Course
import io.github.zmdld11.shuschedule.data.db.CourseSession
import io.github.zmdld11.shuschedule.data.db.Semester
import io.github.zmdld11.shuschedule.data.db.TermType
import io.github.zmdld11.shuschedule.data.db.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupCodecTest {

    private fun sampleSnapshot() = BackupCodec.Snapshot(
        semesters = listOf(
            Semester(
                year = 2026,
                term = TermType.AUTUMN,
                startDateEpochDay = 21400,
                totalWeeks = 16,
                isActive = true,
            ) to listOf(
                Course(
                    semesterId = 1,
                    name = "数据结构",
                    courseCode = "CS1001",
                    className = "数据结构-01",
                    classId = "9100001",
                    credit = "4",
                    colorIndex = 3,
                    note = "带教材\n周五交作业",
                ) to listOf(
                    CourseSession(
                        courseId = 1,
                        weekday = 1,
                        startNode = 1,
                        endNode = 2,
                        weeksMask = CourseSession.maskOf((1..8).toList()),
                        room = "BJ103",
                        teacher = "张三",
                        campus = "宝山",
                    ),
                    CourseSession(
                        courseId = 1,
                        weekday = 3,
                        startNode = 3,
                        endNode = 4,
                        weeksMask = CourseSession.maskOf((2..16 step 2).toList()),
                        room = "实验楼404",
                        teacher = "张三",
                    ),
                ),
            ),
        ),
        timeSlots = listOf(TimeSlot(1, "08:00", "08:45"), TimeSlot(2, "08:55", "09:40")),
    )

    @Test
    fun encodeDecodeRoundtrip() {
        val text = BackupCodec.encode(sampleSnapshot())
        val back = BackupCodec.decode(text)
        assertNotNull(back)

        val (semester, courses) = back!!.semesters.single()
        assertEquals(2026, semester.year)
        assertEquals(TermType.AUTUMN, semester.term)
        assertEquals(21400, semester.startDateEpochDay)
        assertEquals(16, semester.totalWeeks)
        assertEquals(true, semester.isActive)

        val (course, sessions) = courses.single()
        assertEquals("数据结构", course.name)
        assertEquals("CS1001", course.courseCode)
        assertEquals(3, course.colorIndex)
        assertEquals("带教材\n周五交作业", course.note)
        assertEquals(2, sessions.size)

        val mon = sessions.first { it.weekday == 1 }
        assertEquals("BJ103", mon.room)
        assertEquals("宝山", mon.campus)
        assertEquals((1..8).toList(), CourseSession.weeksOf(mon.weeksMask).sorted())
        val wed = sessions.first { it.weekday == 3 }
        assertEquals((2..16 step 2).toList(), CourseSession.weeksOf(wed.weeksMask).sorted())

        assertEquals(2, back.timeSlots.size)
        assertEquals("08:00", back.timeSlots.first().startTime)
    }

    @Test
    fun oldBackupWithoutNoteDefaultsToEmpty() {
        val text = BackupCodec.encode(sampleSnapshot())
            .replace(Regex("\\s*\"note\"\\s*:\\s*\"(?:\\\\.|[^\"\\\\])*\",?"), "")
        val back = BackupCodec.decode(text)
        assertNotNull(back)
        assertEquals("", back!!.semesters.single().second.single().first.note)
    }

    @Test
    fun rejectsInvalidJson() {
        assertNull(BackupCodec.decode("garbage"))
    }

    @Test
    fun rejectsFutureVersion() {
        val future = BackupCodec.encode(sampleSnapshot())
            .replace("\"formatVersion\": 1", "\"formatVersion\": 99")
        assertNull(BackupCodec.decode(future))
    }
}
