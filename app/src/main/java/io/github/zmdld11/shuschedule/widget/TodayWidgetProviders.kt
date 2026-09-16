package io.github.zmdld11.shuschedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.zmdld11.shuschedule.MainActivity
import io.github.zmdld11.shuschedule.R
import io.github.zmdld11.shuschedule.data.repo.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** 数据变化后主动刷新全部小组件（导入/恢复/打开App时调用） */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun pushAll() {
        scope.launch {
            val manager = AppWidgetManager.getInstance(context)
            listOf(
                TodayWidgetSmallProvider::class.java,
                TodayWidgetMediumProvider::class.java,
                TodayWidgetLargeProvider::class.java,
            ).forEach { cls ->
                val ids = manager.getAppWidgetIds(ComponentName(context, cls))
                if (ids.isNotEmpty()) {
                    context.sendBroadcast(
                        Intent(context, cls)
                            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                    )
                }
            }
        }
    }
}

@AndroidEntryPoint
abstract class BaseTodayWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var repository: ScheduleRepository

    final override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = runCatching {
                    val semester = repository.activeSemester()
                    if (semester == null) {
                        TodaySchedule.build(null, emptyList(), emptyList())
                    } else {
                        TodaySchedule.build(
                            semester,
                            repository.getSemesterCourses(semester.id),
                            repository.timeSlots(),
                            repository.getDayOverrides(semester.id),
                        )
                    }
                }.getOrElse { TodaySchedule.build(null, emptyList(), emptyList()) }
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, data, appWidgetManager, id))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    abstract fun buildViews(
        context: Context,
        data: TodayData,
        manager: AppWidgetManager,
        appWidgetId: Int,
    ): RemoteViews

    /** 启动入口走 ACTION_MAIN/LAUNCHER 标准形式；
     * FLAG_MUTABLE 是集合模板（setPendingIntentTemplate + FillInIntent）在 Android 12+ 的硬性要求 */
    protected fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(context, MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

    protected fun headerViews(views: RemoteViews, data: TodayData) {
        views.setTextViewText(R.id.widget_header_title, data.semesterName.ifBlank { "上大课表" })
        views.setTextViewText(R.id.widget_header_week, data.weekLabel)
    }

    protected fun bindList(
        context: Context,
        views: RemoteViews,
        manager: AppWidgetManager,
        appWidgetId: Int,
        data: TodayData,
    ) {
        val intent = Intent(context, TodayWidgetService::class.java)
        views.setRemoteAdapter(R.id.widget_list, intent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)
        // 空态区分「今天没课」与「课都上完了」
        views.setTextViewText(
            R.id.widget_empty,
            when {
                data.items.isEmpty() -> "今天没有课 🎉"
                data.upcomingItems.isEmpty() -> "今日课程已结束"
                else -> "今天没有课 🎉"
            },
        )
        // 列表项点击模板（item 侧 setOnClickFillInIntent）+ 空态也可点
        views.setPendingIntentTemplate(R.id.widget_list, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_empty, openAppIntent(context))
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
    }
}

/** 2×2：下一节课卡片 */
class TodayWidgetSmallProvider : BaseTodayWidgetProvider() {

    override fun buildViews(context: Context, data: TodayData, manager: AppWidgetManager, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today_small)
        val item = data.nextIndex?.let { data.items.getOrNull(it) }
        when {
            data.semesterName.isBlank() -> {
                views.setTextViewText(R.id.widget_small_status, "上大课表")
                views.setTextViewText(R.id.widget_small_name, "未导入课表")
                views.setTextViewText(R.id.widget_small_sub, "点此打开 App 导入")
            }
            item == null && data.items.isEmpty() -> {
                views.setTextViewText(R.id.widget_small_status, "今日")
                views.setTextViewText(R.id.widget_small_name, "今天没有课 🎉")
                views.setTextViewText(R.id.widget_small_sub, data.weekLabel)
            }
            item == null -> {
                views.setTextViewText(R.id.widget_small_status, "今日课程已结束")
                views.setTextViewText(R.id.widget_small_name, "共 ${data.items.size} 节")
                views.setTextViewText(R.id.widget_small_sub, data.weekLabel)
            }
            else -> {
                views.setTextViewText(
                    R.id.widget_small_status,
                    when {
                        data.inClass -> "上课中"
                        data.forTomorrow -> "明天 · 下一节"
                        else -> "下一节课"
                    },
                )
                views.setTextViewText(R.id.widget_small_name, item.name)
                views.setTextViewText(
                    R.id.widget_small_sub,
                    listOf(
                        "第${item.startNode}-${item.endNode}节",
                        "${item.startTime}-${item.endTime}",
                        item.place,
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                )
            }
        }
        views.setOnClickPendingIntent(R.id.widget_small_root, openAppIntent(context))
        return views
    }
}

/** 4×2：今日课程列表 */
class TodayWidgetMediumProvider : BaseTodayWidgetProvider() {

    override fun buildViews(context: Context, data: TodayData, manager: AppWidgetManager, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today_list)
        headerViews(views, data)
        bindList(context, views, manager, appWidgetId, data)
        views.setOnClickPendingIntent(R.id.widget_list_root, openAppIntent(context))
        return views
    }
}

/** 4×4：今日全部课程 */
class TodayWidgetLargeProvider : BaseTodayWidgetProvider() {

    override fun buildViews(context: Context, data: TodayData, manager: AppWidgetManager, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_today_list)
        headerViews(views, data)
        bindList(context, views, manager, appWidgetId, data)
        views.setOnClickPendingIntent(R.id.widget_list_root, openAppIntent(context))
        return views
    }
}
