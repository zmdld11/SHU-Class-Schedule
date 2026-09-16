package io.github.zmdld11.shuschedule.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.zmdld11.shuschedule.data.backup.BackupCodec
import io.github.zmdld11.shuschedule.data.db.TimeSlot
import io.github.zmdld11.shuschedule.data.repo.ScheduleRepository
import io.github.zmdld11.shuschedule.data.settings.AppTheme
import io.github.zmdld11.shuschedule.data.settings.AppearanceSettings
import io.github.zmdld11.shuschedule.data.settings.SettingsStore
import io.github.zmdld11.shuschedule.ui.MainViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val repository: ScheduleRepository,
    private val settings: SettingsStore,
    private val updateClient: io.github.zmdld11.shuschedule.data.update.UpdateCheckClient,
    private val widgetUpdater: io.github.zmdld11.shuschedule.widget.WidgetUpdater,
) : ViewModel() {

    val scheduleBackgroundEnabled: StateFlow<Boolean> =
        settings.scheduleBackgroundEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoUpdateCheck: StateFlow<Boolean> =
        settings.autoUpdateCheck.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setAutoUpdateCheck(value: Boolean) = viewModelScope.launch { settings.setAutoUpdateCheck(value) }

    /** 课表背景：把选中图片压缩存入应用私有目录并开启 */
    fun applyScheduleBackground(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = android.graphics.BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(uri),
                    ) ?: error("无法读取图片")
                    val target = java.io.File(context.filesDir, "schedule_background.jpg")
                    target.outputStream().use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
                    }
                    bitmap.recycle()
                }.isSuccess
            }
            if (ok) settings.setScheduleBackgroundEnabled(true)
            onResult(ok)
        }
    }

    fun clearScheduleBackground() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            java.io.File(context.filesDir, "schedule_background.jpg").delete()
        }
        settings.setScheduleBackgroundEnabled(false)
    }

    /** 手动检查更新：返回 消息 + 新版本页链接（null=已是最新或失败） */
    fun checkUpdate(onResult: (String, String?) -> Unit) = viewModelScope.launch {
        val latest = updateClient.fetchLatest()
        when {
            latest == null -> onResult("检查更新失败，请稍后再试", null)
            io.github.zmdld11.shuschedule.data.update.UpdateChecker.isNewer(
                io.github.zmdld11.shuschedule.BuildConfig.VERSION_NAME, latest.versionName,
            ) -> onResult("发现新版本 ${latest.tagName}", latest.htmlUrl.ifBlank { latest.apkUrl })

            else -> onResult(
                "已是最新版本 v${io.github.zmdld11.shuschedule.BuildConfig.VERSION_NAME}",
                null,
            )
        }
    }

    val timeSlots: StateFlow<List<TimeSlot>> =
        repository.observeTimeSlots().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showOffWeek: StateFlow<Boolean> =
        settings.showOffWeek.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showWeekend: StateFlow<Boolean> =
        settings.showWeekend.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showSlotEnd: StateFlow<Boolean> =
        settings.showSlotEnd.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showWinter: StateFlow<Boolean> =
        settings.showWinter.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setShowOffWeek(value: Boolean) = viewModelScope.launch { settings.setShowOffWeek(value) }

    fun setShowWeekend(value: Boolean) = viewModelScope.launch { settings.setShowWeekend(value) }

    fun setShowSlotEnd(value: Boolean) = viewModelScope.launch { settings.setShowSlotEnd(value) }

    fun setShowWinter(value: Boolean) = viewModelScope.launch { settings.setShowWinter(value) }

    fun saveSlot(slot: TimeSlot) = viewModelScope.launch { repository.upsertTimeSlot(slot) }

    fun resetSlots() = viewModelScope.launch { repository.resetTimeSlots() }

    fun exportTo(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(BackupCodec.encode(repository.backupSnapshot()).toByteArray())
                    } != null
                }.getOrDefault(false)
            }
            onResult(ok)
        }
    }

    fun importFrom(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val (ok, msg) = withContext(Dispatchers.IO) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("无法读取文件")
                    val snapshot = BackupCodec.decode(text) ?: error("备份文件格式不正确")
                    repository.restoreBackup(snapshot)
                    widgetUpdater.pushAll()
                    true to "备份已恢复"
                }.getOrElse { false to (it.message ?: "恢复失败") }
            }
            onResult(ok, msg)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onImport: () -> Unit,
    mainViewModel: MainViewModel,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val timeSlots by viewModel.timeSlots.collectAsStateWithLifecycle()
    val appearance by mainViewModel.appearance.collectAsStateWithLifecycle()
    val currentAppearance = appearance ?: AppearanceSettings()
    val showOffWeek by viewModel.showOffWeek.collectAsStateWithLifecycle()
    val showWeekend by viewModel.showWeekend.collectAsStateWithLifecycle()
    val showSlotEnd by viewModel.showSlotEnd.collectAsStateWithLifecycle()
    val showWinter by viewModel.showWinter.collectAsStateWithLifecycle()
    val scheduleBgEnabled by viewModel.scheduleBackgroundEnabled.collectAsStateWithLifecycle()
    val autoUpdateCheck by viewModel.autoUpdateCheck.collectAsStateWithLifecycle()
    var editingSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var confirmingResetSlots by remember { mutableStateOf(false) }

    val bgPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.applyScheduleBackground(context, uri) { ok ->
            scope.launch { snackbar.showSnackbar(if (ok) "课表背景已更新" else "图片读取失败") }
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportTo(context, uri) { ok ->
            scope.launch { snackbar.showSnackbar(if (ok) "备份已导出" else "导出失败") }
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importFrom(context, uri) { ok, msg ->
            scope.launch { snackbar.showSnackbar(if (ok) msg else "恢复失败：$msg") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                ThemePicker(selectedTheme = currentAppearance.theme, onSelect = mainViewModel::setTheme)
            }
            item {
                ListItem(
                    headlineContent = { Text("跟随系统动态取色") },
                    supportingContent = {
                        Text(if (currentAppearance.theme == AppTheme.ARKNIGHTS)
                            "明日方舟使用固定深色配色；切回默认主题后恢复此设置"
                        else "配色跟随手机壁纸（Android 12+）；关闭或系统不支持时使用上大蓝")
                    },
                    trailingContent = {
                        Switch(
                            checked = currentAppearance.dynamicColor,
                            onCheckedChange = mainViewModel::setDynamicColor,
                            enabled = currentAppearance.theme == AppTheme.DEFAULT && android.os.Build.VERSION.SDK_INT >= 31,
                        )
                    },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("从教务导入课表") },
                    supportingContent = { Text("登录 jwxt.shu.edu.cn 抓取当前学年学期") },
                    modifier = Modifier.clickable(onClick = onImport),
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("显示非本周课程") },
                    supportingContent = { Text("开启后周视图以置灰样式显示本周不上的课") },
                    trailingContent = {
                        Switch(checked = showOffWeek, onCheckedChange = viewModel::setShowOffWeek)
                    },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("显示周六周日") },
                    supportingContent = { Text("上大绝大多数周末无课，默认只显示工作日 5 列") },
                    trailingContent = {
                        Switch(checked = showWeekend, onCheckedChange = viewModel::setShowWeekend)
                    },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("显示节次结束时间") },
                    supportingContent = { Text("每节课固定 45 分钟，默认只显示开始时间") },
                    trailingContent = {
                        Switch(checked = showSlotEnd, onCheckedChange = viewModel::setShowSlotEnd)
                    },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("显示冬季学期") },
                    supportingContent = { Text("教务系统目前查不到冬季课表，学期列表默认隐藏；正在使用中的冬季不受影响") },
                    trailingContent = {
                        Switch(checked = showWinter, onCheckedChange = viewModel::setShowWinter)
                    },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("课表背景") },
                    supportingContent = {
                        Text(when {
                            scheduleBgEnabled -> "已设置自选图片，优先于主题背景；清除后恢复主题背景"
                            currentAppearance.theme == AppTheme.ARKNIGHTS -> "正在使用罗德岛背景；可从相册选择图片替换"
                            else -> "从相册选一张图作为周视图背景（自动加蒙版保证可读）"
                        })
                    },
                    trailingContent = if (scheduleBgEnabled) {
                        {
                            TextButton(onClick = { viewModel.clearScheduleBackground() }) { Text("清除") }
                        }
                    } else null,
                    modifier = Modifier.clickable {
                        bgPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(headlineContent = { Text("节次时间") }, supportingContent = { Text("点击修改上下课时间") })
            }
            item {
                TextButton(
                    onClick = { confirmingResetSlots = true },
                    modifier = Modifier.padding(start = 16.dp),
                ) { Text("恢复默认（12 节）") }
            }
            items(timeSlots, key = { it.node }) { slot ->
                ListItem(
                    headlineContent = { Text("第 ${slot.node} 节") },
                    supportingContent = { Text("${slot.startTime} - ${slot.endTime}") },
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .clickable { editingSlot = slot },
                )
            }
            item { HorizontalDivider() }
            item {
                ListItem(headlineContent = { Text("备份与恢复") }, supportingContent = { Text("课表数据仅存本机，换机前记得导出") })
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { exportLauncher.launch("shu-schedule-backup.json") }, modifier = Modifier.weight(1f)) {
                        Text("导出备份")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) {
                        Text("从备份恢复")
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text("关于") },
                    supportingContent = {
                        Text(
                            "上大课表 v${io.github.zmdld11.shuschedule.BuildConfig.VERSION_NAME} · 课表数据全部保存在本机"
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("检查更新") },
                    supportingContent = { Text("从 GitHub Releases 检查新版本") },
                    modifier = Modifier.clickable {
                        viewModel.checkUpdate { msg, url ->
                            scope.launch {
                                val result = snackbar.showSnackbar(msg, actionLabel = url?.let { "下载" })
                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed && url != null) {
                                    runCatching {
                                        context.startActivity(
                                            android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(url),
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("自动检查更新") },
                    supportingContent = { Text("每天启动时静默检查一次新版本") },
                    trailingContent = {
                        Switch(checked = autoUpdateCheck, onCheckedChange = viewModel::setAutoUpdateCheck)
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("明日方舟主题素材") },
                    supportingContent = { Text("来自 mashirozx/arknights-ui（代码 MIT）；游戏贴图版权归原权利人，仅供学习，请勿商用。本应用为非官方项目。") },
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                "https://github.com/mashirozx/arknights-ui".toUri(),
                            ))
                        }
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("开源仓库") },
                    supportingContent = { Text("zmdld11/SHU-Class-Schedule · MIT") },
                    modifier = Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/zmdld11/SHU-Class-Schedule"),
                                ),
                            )
                        }
                    },
                )
            }
        }
    }

    if (confirmingResetSlots) {
        AlertDialog(
            onDismissRequest = { confirmingResetSlots = false },
            title = { Text("恢复默认节次？") },
            text = { Text("将清空手动修改，恢复为上大 12 节默认作息。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSlots()
                    confirmingResetSlots = false
                }) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { confirmingResetSlots = false }) { Text("取消") } },
        )
    }

    editingSlot?.let { slot ->
        var start by remember(slot.node) { mutableStateOf(slot.startTime) }
        var end by remember(slot.node) { mutableStateOf(slot.endTime) }
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("第 ${slot.node} 节时间") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = start, onValueChange = { start = it.take(5) }, label = { Text("开始 (HH:mm)") }, singleLine = true)
                    OutlinedTextField(value = end, onValueChange = { end = it.take(5) }, label = { Text("结束 (HH:mm)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (Regex("""^\d{2}:\d{2}$""").matches(start) && Regex("""^\d{2}:\d{2}$""").matches(end)) {
                        viewModel.saveSlot(TimeSlot(slot.node, start, end))
                        editingSlot = null
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingSlot = null }) { Text("取消") } },
        )
    }
}
