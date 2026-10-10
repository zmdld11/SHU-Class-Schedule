package io.github.zmdld11.shuschedule.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 日切/时间/时区/开机事件补刷小组件。
 *  部分系统（实测 HyperOS 3）会把 updatePeriodMillis 批处理到 ~12h 且带数小时投递窗口，
 *  「每天早上组件是新的」只能靠自己接这些系统广播兜底 */
@AndroidEntryPoint
class WidgetRefreshReceiver : BroadcastReceiver() {

    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onReceive(context: Context, intent: Intent) {
        if (!shouldRefresh(intent.action)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                widgetUpdater.pushAllBlocking()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** 这些系统事件的共同点：都可能让小组件里的「今天」失效 */
        val REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
        )

        fun shouldRefresh(action: String?): Boolean = action in REFRESH_ACTIONS
    }
}
