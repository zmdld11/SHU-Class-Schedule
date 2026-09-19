package io.github.zmdld11.shuschedule.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CourseWithSessions(
    @Embedded val course: Course,
    @Relation(parentColumn = "id", entityColumn = "courseId")
    val sessions: List<CourseSession>,
)

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY year DESC, term ASC")
    fun observeAll(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters ORDER BY year DESC, term ASC")
    suspend fun getAll(): List<Semester>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<Semester?>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): Semester?

    @Query("SELECT * FROM semesters WHERE year = :year AND term = :term LIMIT 1")
    suspend fun findByYearTerm(year: Int, term: TermType): Semester?

    @Query("SELECT EXISTS(SELECT 1 FROM semesters WHERE isActive = 1)")
    suspend fun hasActive(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(semester: Semester): Long

    @Query("UPDATE semesters SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE semesters SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Transaction
    suspend fun activate(id: Long) {
        clearActive()
        setActive(id)
    }

    @Query("UPDATE semesters SET startDateEpochDay = :epochDay, totalWeeks = :weeks WHERE id = :id")
    suspend fun updateRange(id: Long, epochDay: Long, weeks: Int)

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CourseDao {
    @Transaction
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name")
    fun observeSemesterCourses(semesterId: Long): Flow<List<CourseWithSessions>>

    @Transaction
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name")
    suspend fun getSemesterCourses(semesterId: Long): List<CourseWithSessions>

    @Insert
    suspend fun insertCourses(courses: List<Course>): List<Long>

    @Insert
    suspend fun insertSessions(sessions: List<CourseSession>)

    @Query("UPDATE courses SET note = :note WHERE id = :courseId")
    suspend fun updateNote(courseId: Long, note: String)

    @Update
    suspend fun updateCourse(course: Course)

    @Update
    suspend fun updateSession(session: CourseSession)

    @Query("SELECT COUNT(*) FROM course_sessions WHERE courseId = :courseId")
    suspend fun countSessions(courseId: Long): Int

    @Query("DELETE FROM course_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    /** 课程删除时时段经外键 CASCADE 一并删除 */
    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourse(courseId: Long)

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteBySemester(semesterId: Long)
}

@Dao
interface TimeSlotDao {
    @Query("SELECT * FROM time_slots ORDER BY node")
    fun observeAll(): Flow<List<TimeSlot>>

    @Query("SELECT * FROM time_slots ORDER BY node")
    suspend fun getAll(): List<TimeSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(slots: List<TimeSlot>)

    @Query("DELETE FROM time_slots")
    suspend fun deleteAll()
}

@Dao
interface DayOverrideDao {
    @Query("SELECT * FROM day_overrides WHERE semesterId = :semesterId")
    fun observeForSemester(semesterId: Long): Flow<List<DayOverride>>

    @Query("SELECT * FROM day_overrides WHERE semesterId = :semesterId")
    suspend fun getForSemester(semesterId: Long): List<DayOverride>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: DayOverride)

    @Query("DELETE FROM day_overrides WHERE semesterId = :semesterId AND week = :week AND weekday = :weekday")
    suspend fun delete(semesterId: Long, week: Int, weekday: Int)
}
