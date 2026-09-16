package io.github.zmdld11.shuschedule

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.zmdld11.shuschedule.ui.theme.initExternalThemes

@HiltAndroidApp
class ShuScheduleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 启动即注册已导入的主题包（纯净版为空实现），保证首帧与持久化偏好一致
        initExternalThemes(this)
    }
}
