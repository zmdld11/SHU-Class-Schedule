package io.github.zmdld11.shuschedule.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import dagger.hilt.android.AndroidEntryPoint
import io.github.zmdld11.shuschedule.R
import io.github.zmdld11.shuschedule.data.repo.ScheduleRepository
import io.github.zmdld11.shuschedule.data.settings.SettingsStore
import io.github.zmdld11.shuschedule.ui.theme.ThemeCatalog
import io.github.zmdld11.shuschedule.ui.theme.WidgetThemeColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/** 4×2 / 4×4 今日课程列表的集合小组件数据源 */
@AndroidEntryPoint
class TodayWidgetService : RemoteViewsService() {

    @Inject lateinit var repository: ScheduleRepository
    @Inject lateinit var settings: SettingsStore

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodayListFactory(
            applicationContext,
            repository,
            // 集合视图无法逐次读取主题，绑定工厂时解析一次（主题切换后小组件整体刷新）
            runCatching {
                ThemeCatalog.resolve(runBlocking { settings.appearance.first() }.themeId).widget
            }.getOrDefault(WidgetThemeColors()),
        )

    class TodayListFactory(
        private val context: Context,
        private val repository: ScheduleRepository,
        private val colors: WidgetThemeColors,
    ) : RemoteViewsFactory {

        private var items: List<TodayItem> = emptyList()

        override fun onCreate() {}

        override fun onDataSetChanged() {
            // RemoteViewsFactory 回调在主线程外的 binder 线程，同步短查询可接受（课表数据量极小）
            runBlocking {
                val semester = repository.activeSemester()
                val data = if (semester == null) {
                    TodaySchedule.build(null, emptyList(), emptyList())
                } else {
                    TodaySchedule.build(
                        semester,
                        repository.getSemesterCourses(semester.id),
                        repository.timeSlots(),
                        repository.getDayOverrides(semester.id),
                    )
                }
                // 只显示未结束的课程（已下课的上移剔除），正在上的那节标注
                items = data.upcomingItems
            }
        }

        override fun onDestroy() {
            items = emptyList()
        }

        override fun getCount(): Int = items.size

        override fun getViewAt(position: Int): RemoteViews {
            val item = items.getOrElse(position) { return RemoteViews(context.packageName, R.layout.widget_today_list_item) }
            return RemoteViews(context.packageName, R.layout.widget_today_list_item).apply {
                setTextViewText(R.id.widget_item_time, if (item.startTime.isBlank()) "第${item.startNode}节" else "${item.startTime}\n${item.endTime}")
                setTextViewText(R.id.widget_item_name, item.name)
                setTextViewText(
                    R.id.widget_item_info,
                    listOf(item.place, item.teacher, if (item.inProgress) "正在上课" else null)
                        .filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
                )
                // 配合 provider 端 setPendingIntentTemplate：点列表项也能打开应用
                setOnClickFillInIntent(R.id.widget_item_root, Intent())
                WidgetTheming.applyItem(this, colors)
            }
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = false
    }
}
