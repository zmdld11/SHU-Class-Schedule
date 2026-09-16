package io.github.zmdld11.shuschedule.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

/**
 * 应用内更新下载：走系统 DownloadManager（通知栏进度），下载完成点击通知即拉起安装器。
 * 无需自建服务器与 FileProvider——APK 直链来自 GitHub Releases，落盘在公共下载目录。
 */
object ApkDownloader {

    /** 是否已获得「安装未知应用」授权（Android 8+，需先在 Manifest 声明 REQUEST_INSTALL_PACKAGES） */
    fun canInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** 跳「允许安装未知应用」设置页（package: URI 直达本应用条目） */
    fun requestInstallPermission(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${context.packageName}".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * 入队下载；返回 DownloadManager 的 downloadId（失败返回 -1）。
     * 下载中/完成均有系统通知；完成后点击通知由系统直接发起 APK 安装。
     */
    fun enqueue(context: Context, url: String, versionName: String): Long {
        val safeUrl = url.trim()
        if (safeUrl.isBlank() || !safeUrl.startsWith("https://")) return -1L
        return runCatching {
            val request = DownloadManager.Request(safeUrl.toUri())
                .setTitle("上大课表 $versionName")
                .setDescription("更新安装包")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }.getOrDefault(-1L)
    }
}
