package io.github.zmdld11.shuschedule.widget

import android.content.res.ColorStateList
import android.os.Build
import android.widget.RemoteViews
import io.github.zmdld11.shuschedule.R
import io.github.zmdld11.shuschedule.ui.theme.WidgetThemeColors

/**
 * 小组件主题渲染：按主题定义覆盖 XML 默认颜色。
 * 文字色全版本生效；根/项背景 31+ 走 backgroundTint（保留圆角），
 * Android 8-10 保持默认观感（见 docs/themes.md）。
 */
object WidgetTheming {

    private fun tintBackground(views: RemoteViews, viewId: Int, argb: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(argb.toInt()))
        }
    }

    /** 2×2：状态行/副文字=次级色，课程名=主色 */
    fun applySmall(views: RemoteViews, c: WidgetThemeColors) {
        views.setTextColor(R.id.widget_small_status, c.textSecondary.toInt())
        views.setTextColor(R.id.widget_small_name, c.textPrimary.toInt())
        views.setTextColor(R.id.widget_small_sub, c.textSecondary.toInt())
        tintBackground(views, R.id.widget_small_root, c.rootBackground)
    }

    /** 4×2 / 4×4 列表外壳 */
    fun applyList(views: RemoteViews, c: WidgetThemeColors) {
        views.setTextColor(R.id.widget_header_title, c.textPrimary.toInt())
        views.setTextColor(R.id.widget_header_week, c.textSecondary.toInt())
        views.setTextColor(R.id.widget_empty, c.textSecondary.toInt())
        tintBackground(views, R.id.widget_list_root, c.rootBackground)
    }

    /** 列表项 */
    fun applyItem(views: RemoteViews, c: WidgetThemeColors) {
        views.setTextColor(R.id.widget_item_time, c.textSecondary.toInt())
        views.setTextColor(R.id.widget_item_name, c.textPrimary.toInt())
        views.setTextColor(R.id.widget_item_info, c.textSecondary.toInt())
        tintBackground(views, R.id.widget_item_root, c.itemBackground)
    }
}
