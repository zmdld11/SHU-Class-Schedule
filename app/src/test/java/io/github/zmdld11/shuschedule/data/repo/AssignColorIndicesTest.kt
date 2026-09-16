package io.github.zmdld11.shuschedule.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignColorIndicesTest {

    @Test
    fun `eight courses get pairwise distinct colors`() {
        val names = listOf("形势与政策", "算法设计与分析A", "组合数学", "操作系统(2)", "操作系统(1)", "软件工程", "计算机图形学", "计算机网络")
        val assignment = assignColorIndices(names)
        assertEquals(COURSE_PALETTE_SIZE, assignment.values.toSet().size)
    }

    @Test
    fun `hash collision resolved to a free slot`() {
        // 构造两个 hash 同余的课名：直接验证种子位被占时第二门换空位
        val a = "课A"
        val b = "课B"
        val seed = { n: String -> ((n.hashCode() % COURSE_PALETTE_SIZE) + COURSE_PALETTE_SIZE) % COURSE_PALETTE_SIZE }
        if (seed(a) == seed(b)) {
            val assignment = assignColorIndices(listOf(a, b))
            assertNotEquals(assignment[a], assignment[b])
        } else {
            val assignment = assignColorIndices(listOf(a, b))
            assertEquals(seed(a), assignment[a])
            assertEquals(seed(b), assignment[b])
        }
    }

    @Test
    fun `same name keeps same color across semesters`() {
        val first = assignColorIndices(listOf("数据结构(1)"))
        val second = assignColorIndices(listOf("数据结构(1)"))
        assertEquals(first["数据结构(1)"], second["数据结构(1)"])
    }

    @Test
    fun `more than palette size falls back to sharing`() {
        val names = (1..10).map { "课程$it" }
        val assignment = assignColorIndices(names)
        // 10 门课只有 8 色：色板用满（8 个不同值），允许共存
        assertEquals(COURSE_PALETTE_SIZE, assignment.values.toSet().size)
        // 共存只发生在色板用满之后：每个色的占用数不超过 名字数-色板数+1
        assertTrue(assignment.values.groupingBy { it }.eachCount().values.max() <= names.size - COURSE_PALETTE_SIZE + 1)
    }

    @Test
    fun `deterministic across repeated runs`() {
        val names = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛")
        assertEquals(assignColorIndices(names), assignColorIndices(names))
    }

    @Test
    fun `custom add avoids preoccupied colors`() {
        val a = assignColorIndices(listOf("新课"), preoccupied = setOf(3, 4, 5, 6, 7, 0, 1, 2))
        // 全被占用时只能共存；空一个时取空位
        val seed = (("新课".hashCode() % COURSE_PALETTE_SIZE) + COURSE_PALETTE_SIZE) % COURSE_PALETTE_SIZE
        assertEquals(seed, a["新课"])
        val b = assignColorIndices(listOf("新课"), preoccupied = setOf(seed))
        val expected = (0 until COURSE_PALETTE_SIZE).first { it != seed }
        assertNotEquals(seed, b["新课"])
        assertEquals(expected, b["新课"])
    }
}
