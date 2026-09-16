package io.github.zmdld11.shuschedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zmdld11.shuschedule.data.settings.AppearanceSettings
import io.github.zmdld11.shuschedule.data.settings.SettingsStore
import io.github.zmdld11.shuschedule.widget.WidgetUpdater
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 应用级 UI 状态（主题等），Activity 层持有；打开 App 顺带刷新小组件 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    init {
        widgetUpdater.pushAll()
    }

    // Wait for the persisted appearance before drawing the first screen (no default-theme flash).
    val appearance: StateFlow<AppearanceSettings?> =
        settings.appearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** themeId：内置枚举 id 或已导入主题包 id；切换后小组件随新配色重绘 */
    fun setTheme(themeId: String) {
        viewModelScope.launch {
            settings.setTheme(themeId)
            widgetUpdater.pushAll()
        }
    }

    fun setDarkMode(mode: io.github.zmdld11.shuschedule.data.settings.DarkMode) {
        viewModelScope.launch { settings.setDarkMode(mode) }
    }

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch {
            settings.setDynamicColor(value)
            widgetUpdater.pushAll()
        }
    }
}
