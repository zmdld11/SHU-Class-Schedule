package io.github.zmdld11.shuschedule.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeSlotDefaultsTest {

    @Test
    fun `12 slots numbered 1 to 12`() {
        assertEquals(12, TimeSlotDefaults.all.size)
        assertEquals((1..12).toList(), TimeSlotDefaults.all.map { it.node })
    }

    @Test
    fun `every slot lasts 45 minutes`() {
        TimeSlotDefaults.all.forEach { slot ->
            val start = slot.startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val end = slot.endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            assertEquals("node ${slot.node}", 45, end - start)
        }
        assertTrue(TimeSlotDefaults.all.all { it.startTime < it.endTime })
    }

    @Test
    fun `晚间 11-12 节为实证值`() {
        assertEquals("20:00", TimeSlotDefaults.all[10].startTime)
        assertEquals("20:45", TimeSlotDefaults.all[10].endTime)
        assertEquals("20:55", TimeSlotDefaults.all[11].startTime)
        assertEquals("21:40", TimeSlotDefaults.all[11].endTime)
    }
}
