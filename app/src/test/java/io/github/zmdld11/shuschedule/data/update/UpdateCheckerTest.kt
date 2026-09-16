package io.github.zmdld11.shuschedule.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private val releaseJson = """
    {
      "tag_name": "v0.5.0",
      "name": "v0.5.0",
      "body": "## v0.5.0\n- 主题商店\n- 双包分发",
      "html_url": "https://github.com/zmdld11/SHU-Class-Schedule/releases/tag/v0.5.0",
      "assets": [
        {"name": "shu-schedule-v0.5.0.apk", "browser_download_url": "https://example.com/full.apk"},
        {"name": "shu-schedule-pure-v0.5.0.apk", "browser_download_url": "https://example.com/pure.apk"},
        {"name": "checksums.txt", "browser_download_url": "https://example.com/b.txt"}
      ]
    }
    """.trimIndent()

    @Test
    fun parsesLatestReleaseWithAllApkAssets() {
        val info = UpdateChecker.parseLatest(releaseJson)!!
        assertEquals("v0.5.0", info.tagName)
        assertEquals("0.5.0", info.versionName)
        assertEquals(2, info.apkAssets.size) // txt 资源不进列表
        assertTrue(info.body.contains("主题商店"))
        assertTrue(info.htmlUrl.contains("releases/tag/v0.5.0"))
    }

    @Test
    fun apkSelectionMatchesFlavor() {
        val info = UpdateChecker.parseLatest(releaseJson)!!
        assertEquals("https://example.com/full.apk", info.apkUrlFor(pureFlavor = false))
        assertEquals("https://example.com/pure.apk", info.apkUrlFor(pureFlavor = true))
    }

    @Test
    fun apkSelectionFallsBackWhenSingleAsset() {
        val single = UpdateChecker.parseLatest(
            releaseJson.replace(
                """{"name": "shu-schedule-pure-v0.5.0.apk", "browser_download_url": "https://example.com/pure.apk"},""",
                "",
            ),
        )!!
        assertEquals("https://example.com/full.apk", single.apkUrlFor(pureFlavor = true)) // 无 pure 资源时兜底第一个
    }

    @Test
    fun malformedReturnsNull() {
        assertNull(UpdateChecker.parseLatest("not json"))
        assertNull(UpdateChecker.parseLatest("""{"assets":[]}""")) // 缺 tag_name
    }

    @Test
    fun versionComparison() {
        assertTrue(UpdateChecker.isNewer("0.3.4", "0.3.5"))
        assertTrue(UpdateChecker.isNewer("0.3.9", "0.3.10"))
        assertTrue(UpdateChecker.isNewer("0.3", "0.3.1"))
        assertFalse(UpdateChecker.isNewer("0.3.5", "0.3.5"))
        assertFalse(UpdateChecker.isNewer("0.4.0", "0.3.9"))
    }

    @Test
    fun versionComparisonToleratesFlavorSuffix() {
        // 纯净版 versionName 带 "-pure" 后缀，比较时非数字段按 0 处理
        assertFalse(UpdateChecker.isNewer("0.5.0-pure", "0.5.0"))
        assertTrue(UpdateChecker.isNewer("0.5.0-pure", "0.5.1"))
        assertTrue(UpdateChecker.isNewer("0.4.9", "0.5.0"))
    }
}
