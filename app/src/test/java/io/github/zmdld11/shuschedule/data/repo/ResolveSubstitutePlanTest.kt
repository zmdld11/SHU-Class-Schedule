package io.github.zmdld11.shuschedule.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveSubstitutePlanTest {

    @Test
    fun `方向0_本日按所选日期上课_锚点为长按日`() {
        val plan = resolveSubstitutePlan(0, week = 1, weekday = 7, pickedWeek = 4, pickedWeekday = 2)
        assertEquals(DayOverridePlan(anchorWeek = 1, anchorWeekday = 7, sourceWeek = 4, sourceWeekday = 2), plan)
    }

    @Test
    fun `方向1_所选日期按本日上课_锚点为所选日期`() {
        // 周末列隐藏时：长按周二列，反向设置周日补班
        val plan = resolveSubstitutePlan(1, week = 4, weekday = 2, pickedWeek = 1, pickedWeekday = 7)
        assertEquals(DayOverridePlan(anchorWeek = 1, anchorWeekday = 7, sourceWeek = 4, sourceWeekday = 2), plan)
    }

    @Test
    fun `两方向对称_同一对日期正反设置产生同一条记录`() {
        val forward = resolveSubstitutePlan(0, 1, 7, 4, 2)!!
        val backward = resolveSubstitutePlan(1, 4, 2, 1, 7)!!
        assertEquals(forward, backward)
    }

    @Test
    fun `所选日期就是长按日本身_返回null`() {
        assertNull(resolveSubstitutePlan(0, 3, 5, 3, 5))
        assertNull(resolveSubstitutePlan(1, 3, 5, 3, 5))
    }

    @Test
    fun `同周不同天_合法`() {
        val plan = resolveSubstitutePlan(1, week = 2, weekday = 3, pickedWeek = 2, pickedWeekday = 6)
        assertEquals(DayOverridePlan(2, 6, 2, 3), plan)
    }
}
