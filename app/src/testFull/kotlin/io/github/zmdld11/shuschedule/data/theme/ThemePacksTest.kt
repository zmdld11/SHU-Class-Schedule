package io.github.zmdld11.shuschedule.data.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePacksTest {

    private val validJson = """
    {
      "id": "demo-pack",
      "name": "示例主题",
      "description": "单元测试用",
      "version": 1,
      "author": "tester",
      "fixedDark": true,
      "colors": { "primary": "#FFD900", "onPrimary": "#252100", "surface": "#1C2226" },
      "courseColors": [["#354750", "#BDEBFA"], ["#514921", "#FFE781"]],
      "shape": { "type": "cut", "radius": 7 },
      "widget": { "rootBg": "#F0151A1D", "itemBg": "#2EFFFFFF", "textPrimary": "#F0F2F3", "textSecondary": "#FFD900" }
    }
    """.trimIndent()

    @Test
    fun parsesValidPack() {
        val pack = ThemePacks.parse(validJson).getOrThrow()
        assertEquals("demo-pack", pack.id)
        assertEquals("示例主题", pack.name)
        assertTrue(pack.fixedDark)
        assertEquals(2, pack.courseColors.size)
        assertEquals("cut", pack.shape.type)
        assertEquals(0xFFFFD900L, ThemePacks.parseColor(pack.colors.primary!!))
    }

    @Test
    fun rejectsInvalidId() {
        val bad = validJson.replace("\"demo-pack\"", "\"Demo Pack!\"")
        val message = ThemePacks.parse(bad).exceptionOrNull()!!.message!!
        assertTrue(message.contains("ID"))
    }

    @Test
    fun rejectsBlankName() {
        val bad = validJson.replace("\"示例主题\"", "\"  \"")
        assertTrue(ThemePacks.parse(bad).isFailure)
    }

    @Test
    fun rejectsMalformedColors() {
        val bad = validJson.replace("#FFD900", "#FFD90")
        assertTrue(ThemePacks.parse(bad).exceptionOrNull()!!.message!!.contains("颜色"))
    }

    @Test
    fun rejectsBadShapeType() {
        val bad = validJson.replace("\"cut\"", "\"diamond\"")
        assertTrue(ThemePacks.parse(bad).exceptionOrNull()!!.message!!.contains("shape"))
    }

    @Test
    fun rejectsUnpairedCourseColors() {
        val bad = validJson.replace("[\"#354750\", \"#BDEBFA\"]", "[\"#354750\"]")
        assertTrue(ThemePacks.parse(bad).isFailure)
    }

    @Test
    fun unknownFieldsIgnored() {
        val extra = validJson.dropLast(1) + ",\"futureField\": 42}"
        assertTrue(ThemePacks.parse(extra).isSuccess)
    }

    @Test
    fun colorParsingFormats() {
        assertEquals(0xFF1E5AA8L, ThemePacks.parseColor("#1E5AA8"))        // RRGGBB → 不透明
        assertEquals(0xE61E3A5FL, ThemePacks.parseColor("#E61E3A5F"))     // AARRGGBB 原样
    }
}
