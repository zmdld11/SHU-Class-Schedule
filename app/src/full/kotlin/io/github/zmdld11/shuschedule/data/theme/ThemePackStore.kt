package io.github.zmdld11.shuschedule.data.theme

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipFile

/** 主题包落盘：filesDir/themes/<id>/{theme.json, background.*}；导入=校验后解压，删除=整目录移除 */
object ThemePackStore {

    private const val THEME_JSON = "theme.json"

    fun themesDir(context: Context): File = File(context.filesDir, "themes").apply { mkdirs() }

    fun packDir(context: Context, id: String): File = File(themesDir(context), id)

    /** 启动时扫描已导入主题包并注册；损坏的目录直接清理 */
    fun loadAll(context: Context): List<DynamicThemeDefinition> {
        val loaded = mutableListOf<DynamicThemeDefinition>()
        themesDir(context).listFiles { f -> f.isDirectory }?.forEach { dir ->
            val jsonFile = File(dir, THEME_JSON)
            if (!jsonFile.exists()) {
                dir.deleteRecursively()
                return@forEach
            }
            runCatching {
                val pack = ThemePacks.parse(jsonFile.readText()).getOrThrow()
                val background = dir.listFiles()?.firstOrNull { it.name.startsWith("background.") }?.absolutePath
                loaded += DynamicThemeDefinition(pack, background)
            }.onFailure { dir.deleteRecursively() }
        }
        return loaded
    }

    /** 读取单个已落盘的主题包；损坏返回 null */
    fun loadOne(context: Context, id: String): DynamicThemeDefinition? {
        val dir = packDir(context, id)
        val jsonFile = File(dir, THEME_JSON)
        if (!jsonFile.exists()) return null
        return runCatching {
            val pack = ThemePacks.parse(jsonFile.readText()).getOrThrow()
            val background = dir.listFiles()?.firstOrNull { it.name.startsWith("background.") }?.absolutePath
            DynamicThemeDefinition(pack, background)
        }.getOrNull()
    }

    /** 从 SAF 选中的 .shutheme 导入：校验通过才落盘（返回解析后的包数据）；同 ID 重新导入=覆盖更新。
     *  只接受 theme.json 与 background.* 两类条目（主题=纯数据）。 */
    fun import(context: Context, uri: Uri): Result<ThemePack> = runCatching {
        val temp = File(context.cacheDir, "theme-import-${System.currentTimeMillis()}.zip")
        val staging = File(context.cacheDir, "theme-staging-${System.currentTimeMillis()}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalArgumentException("无法读取所选文件")

            staging.mkdirs()
            val pack = ZipFile(temp).use { zip ->
                val jsonEntry = zip.getEntry(THEME_JSON)
                    ?: throw IllegalArgumentException("包内缺少 $THEME_JSON")
                val parsed = ThemePacks.parse(zip.getInputStream(jsonEntry).bufferedReader().readText())
                    .getOrElse { throw IllegalArgumentException("theme.json 校验失败：${it.message}") }

                zip.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .filter { it.name == THEME_JSON || it.name.startsWith("background.") }
                    .forEach { entry ->
                        File(staging, entry.name).outputStream().use { out ->
                            zip.getInputStream(entry).copyTo(out)
                        }
                    }
                parsed
            }

            val dir = packDir(context, pack.id)
            dir.deleteRecursively()
            staging.copyRecursively(dir, overwrite = true)
            pack
        } finally {
            temp.delete()
            staging.deleteRecursively()
        }
    }

    fun delete(context: Context, id: String) {
        packDir(context, id).deleteRecursively()
    }
}
