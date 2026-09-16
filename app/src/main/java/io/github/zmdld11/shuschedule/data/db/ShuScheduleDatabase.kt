package io.github.zmdld11.shuschedule.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun termToString(term: TermType): String = term.name

    @TypeConverter
    fun stringToTerm(name: String): TermType =
        TermType.fromNameOrNull(name) ?: TermType.AUTUMN
}

@Database(
    entities = [Semester::class, Course::class, CourseSession::class, TimeSlot::class, DayOverride::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ShuScheduleDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun timeSlotDao(): TimeSlotDao
    abstract fun dayOverrideDao(): DayOverrideDao
}
