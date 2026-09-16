package io.github.zmdld11.shuschedule.ui.update

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.zmdld11.shuschedule.BuildConfig
import io.github.zmdld11.shuschedule.data.update.ApkDownloader
import io.github.zmdld11.shuschedule.data.update.UpdateChecker.ReleaseInfo

/** 更新下载的共享交互：设置页 snackbar 与课表页更新弹窗走同一套逻辑 */
object UpdateActions {

    /** 尝试应用内下载；未授权安装或入队失败时走 onNeedPermission / 浏览器回退 */
    fun startDownload(context: Context, info: ReleaseInfo, onNeedPermission: (ReleaseInfo) -> Unit) {
        when {
            !ApkDownloader.canInstallPackages(context) -> onNeedPermission(info)
            ApkDownloader.enqueue(context, info.apkUrlFor(BuildConfig.FLAVOR == "pure"), info.tagName) == -1L ->
                openBrowser(context, info)
        }
    }

    fun openBrowser(context: Context, info: ReleaseInfo) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(info.htmlUrl.ifBlank { info.apkUrlFor(false) }))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

/** 「安装未知应用」授权引导弹窗；拒绝时回退浏览器下载页 */
@Composable
fun InstallPermissionDialog(
    info: ReleaseInfo?,
    onDismiss: () -> Unit,
) {
    if (info == null) return
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("允许安装更新") },
        text = {
            Text(
                "应用内下载更新包需要系统的「安装未知应用」授权：\n" +
                    "点击「去设置」后，找到上大课表并允许从该应用安装即可（只需设置一次）。",
                modifier = Modifier,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                ApkDownloader.requestInstallPermission(context)
                onDismiss()
            }) { Text("去设置") }
        },
        dismissButton = {
            TextButton(onClick = {
                UpdateActions.openBrowser(context, info)
                onDismiss()
            }) { Text("用浏览器下载") }
        },
    )
}
