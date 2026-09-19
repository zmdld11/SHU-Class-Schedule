package io.github.zmdld11.shuschedule.data.backup

import io.github.zmdld11.shuschedule.data.db.Course
import io.github.zmdld11.shuschedule.data.db.CourseSession
import io.github.zmdld11.shuschedule.data.db.Semester
import io.github.zmdld11.shuschedule.data.db.TermType
import io.github.zmdld11.shuschedule.data.db.TimeSlot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 数据库快照与备份 JSON 的互转。formatVersion 只增不改，向前兼容读取。 */
object BackupCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    const val FORMAT_VERSION = 1

    @Serializable
    data class BackupFile(
        val formatVersion: Int = FORMAT_VERSION,
        val exportedAt: Long,
        val semesters: List<BackupSemester> = emptyList(),
        val timeSlots: List<BackupTimeSlot> = emptyList(),
    )

    @Serializable
    data class BackupSemester(
        val year: Int,
        val term: String,
        val startDateEpochDay: Long,
        val totalWeeks: Int,
        val isActive: Boolean = false,
        val courses: List<BackupCourse> = emptyList(),
    )

    @Serializable
    data class BackupCourse(
        val name: String,
        val courseCode: String,
        val className: String,
        val classId: String,
        val credit: String,
        val colorIndex: Int = 0,
        val note: String = "",
        val sessions: List<BackupSession> = emptyList(),
    )

    @Serializable
    data class BackupSession(
        val weekday: Int,
        val startNode: Int,
        val endNode: Int,
        @SerialName("weeks") val weeksList: List<Int> = emptyList(),
        val room: String = "",
        val teacher: String = "",
        val campus: String = "",
        val rescheduled: Boolean = false,
    )

    @Serializable
    data class BackupTimeSlot(val node: Int, val startTime: String, val endTime: String)

    /** DB 快照（semesterId 无需导出，导入时按 year+term 重建） */
    data class Snapshot(
        val semesters: List<Pair<Semester, List<Pair<Course, List<CourseSession>>>>> = emptyList(),
        val timeSlots: List<TimeSlot> = emptyList(),
    )

    fun encode(snapshot: Snapshot): String = json.encodeToString(
        BackupFile(
            exportedAt = System.currentTimeMillis(),
            semesters = snapshot.semesters.map { (semester, courses) ->
                BackupSemester(
                    year = semester.year,
                    term = semester.term.name,
                    startDateEpochDay = semester.startDateEpochDay,
                    totalWeeks = semester.totalWeeks,
                    isActive = semester.isActive,
                    courses = courses.map { (course, sessions) ->
                        BackupCourse(
                            name = course.name,
                            courseCode = course.courseCode,
                            className = course.className,
                            classId = course.classId,
                            credit = course.credit,
                            colorIndex = course.colorIndex,
                            note = course.note,
                            sessions = sessions.map { s ->
                                BackupSession(
                                    weekday = s.weekday,
                                    startNode = s.startNode,
                                    endNode = s.endNode,
                                    weeksList = CourseSession.weeksOf(s.weeksMask).sorted(),
                                    room = s.room,
                                    teacher = s.teacher,
                                    campus = s.campus,
                                    rescheduled = s.rescheduled,
                                )
                            },
                        )
                    },
                )
            },
            timeSlots = snapshot.timeSlots.map { BackupTimeSlot(it.node, it.startTime, it.endTime) },
        )
    )

    fun decode(text: String): Snapshot? {
        val file = runCatching { json.decodeFromString<BackupFile>(text) }.getOrNull() ?: return null
        if (file.formatVersion > FORMAT_VERSION) return null
        return Snapshot(
            semesters = file.semesters.mapNotNull { bs ->
                val term = TermType.fromNameOrNull(bs.term) ?: return@mapNotNull null
                val semester = Semester(
                    year = bs.year,
                    term = term,
                    startDateEpochDay = bs.startDateEpochDay,
                    totalWeeks = bs.totalWeeks,
                    isActive = bs.isActive,
                )
                semester to bs.courses.map { bc ->
                    val course = Course(
                        semesterId = 0,
                        name = bc.name,
                        courseCode = bc.courseCode,
                        className = bc.className,
                        classId = bc.classId,
                        credit = bc.credit,
                        colorIndex = bc.colorIndex,
                        note = bc.note,
                    )
                    course to bc.sessions.map { s ->
                        CourseSession(
                            courseId = 0,
                            weekday = s.weekday,
                            startNode = s.startNode,
                            endNode = s.endNode,
                            weeksMask = CourseSession.maskOf(s.weeksList),
                            room = s.room,
                            teacher = s.teacher,
                            campus = s.campus,
                            rescheduled = s.rescheduled,
                        )
                    }
                }
            },
            timeSlots = file.timeSlots.map { TimeSlot(it.node, it.startTime, it.endTime) },
        )
    }
}
