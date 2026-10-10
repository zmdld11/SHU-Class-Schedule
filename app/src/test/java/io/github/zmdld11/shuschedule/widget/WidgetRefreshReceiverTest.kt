package io.github.zmdld11.shuschedule.widget

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRefreshReceiverTest {

    @Test
    fun systemEventsThatInvalidateTodayTriggerRefresh() {
        assertTrue(WidgetRefreshReceiver.shouldRefresh(Intent.ACTION_DATE_CHANGED))
        assertTrue(WidgetRefreshReceiver.shouldRefresh(Intent.ACTION_TIME_CHANGED))
        assertTrue(WidgetRefreshReceiver.shouldRefresh(Intent.ACTION_TIMEZONE_CHANGED))
        assertTrue(WidgetRefreshReceiver.shouldRefresh(Intent.ACTION_BOOT_COMPLETED))
    }

    @Test
    fun widgetTickAndUnknownActionsAreIgnored() {
        // 30 分钟系统 tick 由 AppWidgetService 自己派发，接收器不重复处理
        assertFalse(WidgetRefreshReceiver.shouldRefresh("android.appwidget.action.APPWIDGET_UPDATE"))
        assertFalse(WidgetRefreshReceiver.shouldRefresh(Intent.ACTION_TIME_TICK))
        assertFalse(WidgetRefreshReceiver.shouldRefresh(null))
        assertFalse(WidgetRefreshReceiver.shouldRefresh("something.else"))
    }
}
