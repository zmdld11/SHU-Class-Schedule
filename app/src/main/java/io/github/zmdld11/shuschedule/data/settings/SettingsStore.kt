package io.github.zmdld11.shuschedule.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val themeKey = stringPreferencesKey("app_theme")
    private val darkModeKey = stringPreferencesKey("dark_mode")
    private val showOffWeekKey = booleanPreferencesKey("show_off_week")
    private val showWeekendKey = booleanPreferencesKey("show_weekend")
    private val showSlotEndKey = booleanPreferencesKey("show_slot_end")

    val appearance: Flow<AppearanceSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppearanceSettings(
                themeId = preferences[themeKey] ?: AppTheme.DEFAULT.id, // 外部主题包 id 原样保存，未命中时渲染侧回落默认
                dynamicColor = preferences[dynamicColorKey] ?: true,
                darkMode = DarkMode.fromId(preferences[darkModeKey]),
            )
        }

    suspend fun setTheme(themeId: String) {
        context.dataStore.edit { it[themeKey] = themeId }
    }

    suspend fun setDarkMode(mode: DarkMode) {
        context.dataStore.edit { it[darkModeKey] = mode.id }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.dataStore.edit { it[dynamicColorKey] = value }
    }

    /** 周视图是否置灰显示非本周课程（默认隐藏） */
    val showOffWeek: Flow<Boolean> = context.dataStore.data.map { it[showOffWeekKey] ?: false }

    suspend fun setShowOffWeek(value: Boolean) {
        context.dataStore.edit { it[showOffWeekKey] = value }
    }

    /** 周视图是否显示周六周日（上大绝大多数周末无课，默认只显示工作日 5 列） */
    val showWeekend: Flow<Boolean> = context.dataStore.data.map { it[showWeekendKey] ?: false }

    suspend fun setShowWeekend(value: Boolean) {
        context.dataStore.edit { it[showWeekendKey] = value }
    }

    /** 节次时间列是否显示下课时间（每节固定 45 分钟，默认只显示开始时间） */
    val showSlotEnd: Flow<Boolean> = context.dataStore.data.map { it[showSlotEndKey] ?: false }

    suspend fun setShowSlotEnd(value: Boolean) {
        context.dataStore.edit { it[showSlotEndKey] = value }
    }

    /** 学期列表是否显示冬季学期；教务目前查不到冬季课表，默认隐藏（激活中的除外，在过滤处处理） */
    private val showWinterKey = booleanPreferencesKey("show_winter")

    val showWinter: Flow<Boolean> = context.dataStore.data.map { it[showWinterKey] ?: false }

    suspend fun setShowWinter(value: Boolean) {
        context.dataStore.edit { it[showWinterKey] = value }
    }

    /** 上次自动检查更新的日期（epoch day），用于每天最多检查一次 */
    private val lastUpdateCheckDayKey = intPreferencesKey("last_update_check_day")

    val lastUpdateCheckDay: Flow<Int> = context.dataStore.data.map { it[lastUpdateCheckDayKey] ?: 0 }

    suspend fun setLastUpdateCheckDay(day: Int) {
        context.dataStore.edit { it[lastUpdateCheckDayKey] = day }
    }

    /** 课表背景壁纸（图片文件固定存 filesDir/schedule_background.jpg，此处只记开关） */
    private val scheduleBackgroundKey = booleanPreferencesKey("schedule_background")

    val scheduleBackgroundEnabled: Flow<Boolean> = context.dataStore.data.map { it[scheduleBackgroundKey] ?: false }

    suspend fun setScheduleBackgroundEnabled(value: Boolean) {
        context.dataStore.edit { it[scheduleBackgroundKey] = value }
    }

    /** 启动时是否自动检查更新（默认开；手动检查不受影响） */
    private val autoUpdateCheckKey = booleanPreferencesKey("auto_update_check")

    val autoUpdateCheck: Flow<Boolean> = context.dataStore.data.map { it[autoUpdateCheckKey] ?: true }

    suspend fun setAutoUpdateCheck(value: Boolean) {
        context.dataStore.edit { it[autoUpdateCheckKey] = value }
    }
}
