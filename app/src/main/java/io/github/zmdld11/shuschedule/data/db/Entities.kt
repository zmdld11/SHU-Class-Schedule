package io.github.zmdld11.shuschedule.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 上大四季学期制：秋季(长) → 冬季(短) → 春季(长) → 夏季(短) */
enum class TermType(val label: String) {
    AUTUMN("秋季"),
    WINTER("冬季"),
    SPRING("春季"),
    SUMMER("夏季");

    val orderInYear: Int get() = ordinal

    companion object {
        fun fromNameOrNull(s: String?): TermType? =
            s?.let { name -> entries.firstOrNull { it.name == name || it.label == name } }
    }
}

@Entity(
    tableName = "semesters",
    indices = [Index(value = ["year", "term"], unique = true)],
)
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 学年起始年，如 2026 表示 2026-2027 学年 */
    val year: Int,
    val term: TermType,
    /** 第一周周一的 epoch day */
    val startDateEpochDay: Long,
    val totalWeeks: Int = 16,
    val isActive: Boolean = false,
) {
    val displayName: String get() = "${year}-${year + 1}学年${term.label}"
}

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("semesterId")],
)
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    /** kcmc 课程名 */
    val name: String,
    /** kch 课程号 */
    val courseCode: String,
    /** jxbmc 教学班名 */
    val className: String,
    /** jxb_id 教学班 id */
    val classId: String,
    /** xf 学分（正方返回字符串，原样保存） */
    val credit: String,
    val colorIndex: Int = 0,
)

@Entity(
    tableName = "course_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("courseId")],
)
data class CourseSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    /** 星期 1=周一 … 7=周日 */
    val weekday: Int,
    /** 节次（含端点） */
    val startNode: Int,
    val endNode: Int,
    /** 周次集合的位掩码：第 w 周对应 bit (w-1)，w ∈ 1..25 */
    val weeksMask: Int,
    /** cdmc 教室（按场次存，同一门课不同周次可能换教室） */
    val room: String,
    /** xm 教师 */
    val teacher: String,
    /** 校区（kbList 若返回 xqumc/xqmc 则入库，可能为空串） */
    val campus: String = "",
    /** 调课标记：教务调课记录 zcd 形如「第13周」（导入时打标），UI 差异化展示 */
    val rescheduled: Boolean = false,
) {
    fun hasWeek(week: Int): Boolean = week in 1..MAX_WEEKS && (weeksMask and (1 shl (week - 1))) != 0

    companion object {
        const val MAX_WEEKS = 25

        fun maskOf(weeks: Collection<Int>): Int =
            weeks.fold(0) { acc, w -> if (w in 1..MAX_WEEKS) acc or (1 shl (w - 1)) else acc }

        fun weeksOf(mask: Int): Set<Int> =
            (1..MAX_WEEKS).filterTo(mutableSetOf()) { (mask and (1 shl (it - 1))) != 0 }
    }
}

@Entity(tableName = "time_slots")
data class TimeSlot(
    @PrimaryKey val node: Int,
    val startTime: String,
    val endTime: String,
)

/**
 * 节假日调休的按天覆盖：某学期的某周某天不上课（放假）或按另一周几的课表上（调休补课）。
 * 教务不回传整天级的调课安排，由用户长按周视图列头手动设置。
 */
@Entity(
    tableName = "day_overrides",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["semesterId", "week", "weekday"], unique = true)],
)
data class DayOverride(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    val week: Int,
    val weekday: Int,
    /** MODE_HOLIDAY=放假不上课；MODE_SUBSTITUTE=按 substituteWeekday 的课表上 */
    val mode: Int,
    val substituteWeekday: Int = 0,
    /** 班模式下来源教学周（跨周补课，如 9/20 补第 5 周的课）；null=当天所在周 */
    val sourceWeek: Int? = null,
) {
    companion object {
        const val MODE_HOLIDAY = 0
        const val MODE_SUBSTITUTE = 1

        /** 显示用：「休」/「班·周三」/「班·周三·第5周」 */
        val WEEKDAY_CHARS = listOf("一", "二", "三", "四", "五", "六", "日")
    }
}
