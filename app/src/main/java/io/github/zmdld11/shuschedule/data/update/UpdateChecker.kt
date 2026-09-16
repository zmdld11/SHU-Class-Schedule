package io.github.zmdld11.shuschedule.data.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** GitHub Releases 最新版信息解析与版本比较（纯函数，可单测） */
object UpdateChecker {

    data class ApkAsset(val name: String, val url: String)

    data class ReleaseInfo(
        val tagName: String,     // "v0.3.5"
        val versionName: String, // "0.3.5"
        val name: String,        // release 标题
        val body: String,        // 更新说明（changelog 正文）
        val apkAssets: List<ApkAsset>, // 全部 apk 资源（默认版/纯净版）
        val htmlUrl: String,     // release 页面
    ) {
        /** 按 flavor 匹配安装包：纯净版选文件名含 "pure" 的，默认版选不含的 */
        fun apkUrlFor(pureFlavor: Boolean): String =
            when {
                pureFlavor -> apkAssets.firstOrNull { it.name.contains("pure", ignoreCase = true) }
                else -> apkAssets.firstOrNull { !it.name.contains("pure", ignoreCase = true) }
            }?.url ?: apkAssets.firstOrNull()?.url.orEmpty()
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun parseLatest(jsonText: String): ReleaseInfo? = runCatching {
        val root = json.parseToJsonElement(jsonText).jsonObject
        val tagName = root["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
        val assets = root["assets"]?.jsonArray
            ?.mapNotNull { asset ->
                val obj = asset.jsonObject
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val url = obj["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                if (name.endsWith(".apk")) ApkAsset(name, url) else null
            }
            .orEmpty()
        ReleaseInfo(
            tagName = tagName,
            versionName = tagName.removePrefix("v"),
            name = root["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            body = root["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            apkAssets = assets,
            htmlUrl = root["html_url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }.getOrNull()

    /** 点分版本号逐段数值比较：latest > current 才为真（非数字段如 "0-pure" 按 0 处理） */
    fun isNewer(current: String, latest: String): Boolean {
        val a = current.split('.').map { it.toIntOrNull() ?: 0 }
        val b = latest.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }
}
