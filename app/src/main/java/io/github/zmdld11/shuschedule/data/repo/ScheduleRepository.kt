package io.github.zmdld11.shuschedule.data.repo

import androidx.room.withTransaction
import io.github.zmdld11.shuschedule.data.backup.BackupCodec
import io.github.zmdld11.shuschedule.data.db.Course
import io.github.zmdld11.shuschedule.data.db.CourseDao
import io.github.zmdld11.shuschedule.data.db.DayOverrideDao
import io.github.zmdld11.shuschedule.data.db.CourseSession
import io.github.zmdld11.shuschedule.data.db.CourseWithSessions
import io.github.zmdld11.shuschedule.data.db.DayOverride
import io.github.zmdld11.shuschedule.data.db.Semester
import io.github.zmdld11.shuschedule.data.db.SemesterDao
import io.github.zmdld11.shuschedule.data.db.ShuScheduleDatabase
import io.github.zmdld11.shuschedule.data.db.TermType
import io.github.zmdld11.shuschedule.data.db.TimeSlot
import io.github.zmdld11.shuschedule.data.db.TimeSlotDao
import io.github.zmdld11.shuschedule.data.parser.ParsedCourse
import io.github.zmdld11.shuschedule.data.parser.TimeSlotDefaults
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 课表色板数量 */
const val COURSE_PALETTE_SIZE = 8

/**
 * 课程名 → 色号分配：以名字 hash 为种子（同名课跨学期同色），种子位被占时顺序取第一个空位，
 * 8 色用尽才允许共存——同学期 ≤8 门课时颜色互不相同。确定性算法，重复导入结果一致。
 */
fun assignColorIndices(names: List<String>, preoccupied: Set<Int> = emptySet()): Map<String, Int> {
    val used = preoccupied.toMutableSet()
    val result = mutableMapOf<String, Int>()
    for (name in names) {
        val seed = ((name.hashCode() % COURSE_PALETTE_SIZE) + COURSE_PALETTE_SIZE) % COURSE_PALETTE_SIZE
        val idx = if (seed in used && used.size < COURSE_PALETTE_SIZE) {
            (0 until COURSE_PALETTE_SIZE).firstOrNull { it !in used } ?: seed
        } else {
            seed
        }
        used += idx
        result[name] = idx
    }
    return result
}

@Singleton
class ScheduleRepository @Inject constructor(
    private val db: ShuScheduleDatabase,
    private val semesterDao: SemesterDao,
    private val courseDao: CourseDao,
    private val timeSlotDao: TimeSlotDao,
    private val dayOverrideDao: DayOverrideDao,
) {

    fun observeSemesters(): Flow<List<Semester>> = semesterDao.observeAll()

    fun observeActiveSemester(): Flow<Semester?> = semesterDao.observeActive()

    fun observeDayOverrides(semesterId: Long): Flow<List<DayOverride>> =
        dayOverrideDao.observeForSemester(semesterId)

    suspend fun getDayOverrides(semesterId: Long): List<DayOverride> =
        dayOverrideDao.getForSemester(semesterId)

    /** 设置某天覆盖；mode<0 表示清除（恢复正常）。班模式 sourceWeek=跨周补课的来源教学周 */
    suspend fun setDayOverride(
        semesterId: Long,
        week: Int,
        weekday: Int,
        mode: Int,
        substituteWeekday: Int,
        sourceWeek: Int? = null,
    ) {
        if (mode < 0) {
            dayOverrideDao.delete(semesterId, week, weekday)
        } else {
            dayOverrideDao.upsert(
                DayOverride(
                    semesterId = semesterId,
                    week = week,
                    weekday = weekday,
                    mode = mode,
                    substituteWeekday = substituteWeekday,
                    sourceWeek = sourceWeek,
                ),
            )
        }
    }

    fun observeCourses(semesterId: Long): Flow<List<CourseWithSessions>> =
        courseDao.observeSemesterCourses(semesterId)

    fun observeTimeSlots(): Flow<List<TimeSlot>> = timeSlotDao.observeAll()

    suspend fun getSemesterCourses(semesterId: Long): List<CourseWithSessions> =
        courseDao.getSemesterCourses(semesterId)

    suspend fun activeSemester(): Semester? = semesterDao.getActive()

    suspend fun timeSlots(): List<TimeSlot> = timeSlotDao.getAll()

    suspend fun activateSemester(id: Long) = semesterDao.activate(id)

    /** 手动添加空白学期；同年同学期已存在时返回 false */
    suspend fun addSemesterManually(
        year: Int,
        term: TermType,
        startDateEpochDay: Long,
        totalWeeks: Int,
    ): Boolean {
        if (semesterDao.findByYearTerm(year, term) != null) return false
        db.withTransaction {
            semesterDao.upsert(
                Semester(
                    year = year,
                    term = term,
                    startDateEpochDay = startDateEpochDay,
                    totalWeeks = totalWeeks,
                )
            )
        }
        return true
    }

    suspend fun updateSemesterRange(id: Long, startDateEpochDay: Long, totalWeeks: Int) =
        semesterDao.updateRange(id, startDateEpochDay, totalWeeks)

    suspend fun deleteSemester(id: Long) = semesterDao.delete(id)

    suspend fun upsertTimeSlot(slot: TimeSlot) = timeSlotDao.upsertAll(listOf(slot))

    /** 恢复默认 12 节作息（覆盖手动修改） */
    suspend fun resetTimeSlots() = db.withTransaction {
        timeSlotDao.deleteAll()
        timeSlotDao.upsertAll(TimeSlotDefaults.all)
    }

    /** 手动编辑保存：课程名与单个时段一起落库 */
    suspend fun saveSessionEdit(course: Course, session: CourseSession) = db.withTransaction {
        courseDao.updateCourse(course)
        courseDao.updateSession(session)
    }

    /** 给已有课程追加一个时段（课程名可同步改名） */
    suspend fun addSession(course: Course, session: CourseSession) = db.withTransaction {
        courseDao.updateCourse(course)
        courseDao.insertSessions(listOf(session.copy(courseId = course.id)))
    }

    /** 新建自定义课程（含第一个时段）：色号默认同学期避让自动分配，可显式指定 */
    suspend fun addCustomCourse(
        semesterId: Long,
        name: String,
        session: CourseSession,
        colorIndex: Int? = null,
    ): Long = db.withTransaction {
        val usedColors = courseDao.getSemesterCourses(semesterId).map { it.course.colorIndex }.toSet()
        val assigned = colorIndex ?: assignColorIndices(listOf(name), usedColors)[name] ?: 0
        val courseId = courseDao.insertCourses(
            listOf(
                Course(
                    semesterId = semesterId,
                    name = name,
                    courseCode = "自定义",
                    className = "",
                    classId = "custom-${System.currentTimeMillis()}",
                    credit = "",
                    colorIndex = assigned,
                )
            )
        ).first()
        courseDao.insertSessions(listOf(session.copy(courseId = courseId)))
        courseId
    }

    /** 删除单个时段；若因此课程再无时段，整门课一并删除 */
    suspend fun deleteSessionAndOrphanCourse(session: CourseSession) = db.withTransaction {
        courseDao.deleteSession(session.id)
        if (courseDao.countSessions(session.courseId) == 0) {
            courseDao.deleteCourse(session.courseId)
        }
    }

    /** 删除整门课（时段级联删除） */
    suspend fun deleteCourse(courseId: Long) = courseDao.deleteCourse(courseId)

    /**
     * 手动调休：把 original 中第 week 周的一次课拆出来，换成 newSession（单周、带调课标记）。
     * 原时段周次减去该周；减完为空则原位替换。
     */
    suspend fun rescheduleSession(
        original: CourseSession,
        week: Int,
        newSession: CourseSession,
    ) = db.withTransaction {
        require(week in 1..CourseSession.MAX_WEEKS) { "周次越界: $week" }
        val weekBit = 1 shl (week - 1)
        val remaining = original.weeksMask and weekBit.inv()
        if (remaining == 0) {
            courseDao.updateSession(newSession.copy(id = original.id))
        } else {
            courseDao.updateSession(original.copy(weeksMask = remaining))
            courseDao.insertSessions(listOf(newSession.copy(id = 0)))
        }
    }

    /** 全量快照（备份导出用） */
    suspend fun backupSnapshot(): BackupCodec.Snapshot = db.withTransaction {
        val snapshot = mutableListOf<Pair<Semester, List<Pair<Course, List<CourseSession>>>>>()
        for (semester in semesterDao.getAll()) {
            val courses = courseDao.getSemesterCourses(semester.id).map { c ->
                c.course to c.sessions
            }
            snapshot += semester to courses
        }
        BackupCodec.Snapshot(semesters = snapshot, timeSlots = timeSlotDao.getAll())
    }

    /** 从备份恢复（整库重建式写入，保留备份中的激活学期标记） */
    suspend fun restoreBackup(snapshot: BackupCodec.Snapshot) = db.withTransaction {
        val activeKey = snapshot.semesters
            .firstOrNull { (s, _) -> s.isActive }
            ?.let { (s, _) -> s.year to s.term }
        snapshot.semesters.forEach { (semester, courses) ->
            val existing = semesterDao.findByYearTerm(semester.year, semester.term)
            val semesterId = if (existing != null) {
                semesterDao.updateRange(existing.id, semester.startDateEpochDay, semester.totalWeeks)
                existing.id
            } else {
                semesterDao.upsert(
                    Semester(
                        year = semester.year,
                        term = semester.term,
                        startDateEpochDay = semester.startDateEpochDay,
                        totalWeeks = semester.totalWeeks,
                    )
                )
            }
            courseDao.deleteBySemester(semesterId)
            courses.forEach { (course, sessions) ->
                val courseId = courseDao.insertCourses(listOf(course.copy(semesterId = semesterId))).first()
                courseDao.insertSessions(sessions.map { it.copy(courseId = courseId) })
            }
            if (timeSlotDao.getAll().isEmpty() && snapshot.timeSlots.isNotEmpty()) {
                timeSlotDao.upsertAll(snapshot.timeSlots)
            }
        }
        if (activeKey != null) {
            val current = semesterDao.getAll().firstOrNull { it.isActive }
            if (current == null) {
                semesterDao.findByYearTerm(activeKey.first, activeKey.second)?.let { activateSemester(it.id) }
            }
        }
    }

    /**
     * 导入解析后的课表：按 (year, term) 复用或新建学期，整体替换该学期的课程。
     * 首次导入自动设为激活学期并补默认节次作息。
     */
    suspend fun importParsed(
        year: Int,
        term: TermType,
        startDateEpochDay: Long,
        totalWeeks: Int,
        parsed: List<ParsedCourse>,
    ): Long = db.withTransaction {
        val existing = semesterDao.findByYearTerm(year, term)
        val semesterId = if (existing != null) {
            semesterDao.updateRange(existing.id, startDateEpochDay, totalWeeks)
            existing.id
        } else {
            semesterDao.upsert(
                Semester(
                    year = year,
                    term = term,
                    startDateEpochDay = startDateEpochDay,
                    totalWeeks = totalWeeks,
                )
            )
        }

        courseDao.deleteBySemester(semesterId)
        // 先按课名 hash 定种子色，再消解碰撞：同学期 ≤8 门课时保证颜色互不相同
        val colorAssignments = assignColorIndices(parsed.map { it.name })
        parsed.forEach { p ->
            val courseId = courseDao.insertCourses(
                listOf(
                    Course(
                        semesterId = semesterId,
                        name = p.name,
                        courseCode = p.courseCode,
                        className = p.className,
                        classId = p.classId,
                        credit = p.credit,
                        colorIndex = colorAssignments[p.name] ?: 0,
                    )
                )
            ).first()
            courseDao.insertSessions(
                p.sessions.map {
                    CourseSession(
                        courseId = courseId,
                        weekday = it.weekday,
                        startNode = it.startNode,
                        endNode = it.endNode,
                        weeksMask = it.weeksMask,
                        room = it.room,
                        teacher = it.teacher,
                        campus = it.campus,
                        rescheduled = it.rescheduled,
                    )
                }
            )
        }

        if (timeSlotDao.getAll().isEmpty()) timeSlotDao.upsertAll(TimeSlotDefaults.all)

        if (!semesterDao.hasActive()) semesterDao.activate(semesterId)

        semesterId
    }
}
