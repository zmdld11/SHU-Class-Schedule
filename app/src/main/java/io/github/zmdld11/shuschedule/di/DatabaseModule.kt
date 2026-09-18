package io.github.zmdld11.shuschedule.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.zmdld11.shuschedule.data.db.CourseDao
import io.github.zmdld11.shuschedule.data.db.DayOverrideDao
import io.github.zmdld11.shuschedule.data.db.SemesterDao
import io.github.zmdld11.shuschedule.data.db.ShuScheduleDatabase
import io.github.zmdld11.shuschedule.data.db.TimeSlotDao
import javax.inject.Singleton

/** v2→v3：调课标记列（老数据一律 false，重新导入后由解析器打标）。
 * 注意须为顶层命名类：@Module 内的匿名 object 字段会触发 Dagger KSP 校验异常。 */
private class Migration2To3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE course_sessions ADD COLUMN rescheduled INTEGER NOT NULL DEFAULT 0")
    }
}

/** v3→v4：节假日调休按天覆盖表 */
private class Migration3To4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `day_overrides` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `semesterId` INTEGER NOT NULL,
              `week` INTEGER NOT NULL,
              `weekday` INTEGER NOT NULL,
              `mode` INTEGER NOT NULL,
              `substituteWeekday` INTEGER NOT NULL DEFAULT 0,
              FOREIGN KEY(`semesterId`) REFERENCES `semesters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_day_overrides_semesterId_week_weekday` " +
                "ON `day_overrides` (`semesterId`, `week`, `weekday`)"
        )
    }
}

/** v4→v5：班模式来源教学周（跨周补课）；老数据 null=当天所在周，行为不变 */
private class Migration4To5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE day_overrides ADD COLUMN sourceWeek INTEGER DEFAULT NULL")
    }
}

private class Migration5To6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN note TEXT NOT NULL DEFAULT ''")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShuScheduleDatabase =
        Room.databaseBuilder(context, ShuScheduleDatabase::class.java, "shu_schedule.db")
            .addMigrations(Migration2To3(), Migration3To4(), Migration4To5(), Migration5To6())
            // 开发期兜底（已提供 v2→v6 迁移，正常升级不触发破坏性重建）
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides fun provideSemesterDao(db: ShuScheduleDatabase): SemesterDao = db.semesterDao()

    @Provides fun provideCourseDao(db: ShuScheduleDatabase): CourseDao = db.courseDao()

    @Provides fun provideTimeSlotDao(db: ShuScheduleDatabase): TimeSlotDao = db.timeSlotDao()

    @Provides fun provideDayOverrideDao(db: ShuScheduleDatabase): DayOverrideDao = db.dayOverrideDao()
}
