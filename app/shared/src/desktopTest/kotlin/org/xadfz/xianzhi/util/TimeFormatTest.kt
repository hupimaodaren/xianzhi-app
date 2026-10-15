package org.xadfz.xianzhi.util

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeFormatTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        java.util.Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    private val now = at(2026, 10, 15, 12, 0)

    @Test
    fun justNowWithinOneMinute() {
        assertEquals("刚刚", formatHistoryTime(now, now - 30_000L))
        assertEquals("刚刚", formatHistoryTime(now, now))
    }

    @Test
    fun futureTreatedAsJustNow() {
        // 时钟误差导致记录时间在未来：按「刚刚」处理，不出现负数
        assertEquals("刚刚", formatHistoryTime(now, now + 5_000L))
    }

    @Test
    fun minutesAgo() {
        assertEquals("5 分钟前", formatHistoryTime(now, now - 5 * 60_000L))
        assertEquals("59 分钟前", formatHistoryTime(now, now - 59 * 60_000L))
    }

    @Test
    fun hoursAgo() {
        assertEquals("1 小时前", formatHistoryTime(now, now - 60 * 60_000L))
        assertEquals("3 小时前", formatHistoryTime(now, now - 3 * 3_600_000L))
        assertEquals("23 小时前", formatHistoryTime(now, now - 23 * 3_600_000L))
    }

    @Test
    fun sameYearShowsMonthDay() {
        assertEquals("3月2日", formatHistoryTime(now, at(2026, 3, 2, 9, 30)))
        // 同年但超过 24 小时：显示月日
        assertEquals("10月13日", formatHistoryTime(now, at(2026, 10, 13, 11, 0)))
    }

    @Test
    fun crossYearShowsFullDate() {
        // 跨年且超过 24 小时：带年份
        assertEquals("2025年12月30日", formatHistoryTime(at(2026, 1, 1, 12, 0), at(2025, 12, 30, 23, 0)))
    }

    @Test
    fun crossYearWithin24HoursStillRelative() {
        // 跨年但不足 24 小时：仍按相对时间显示
        assertEquals("2 小时前", formatHistoryTime(at(2026, 1, 1, 1, 0), at(2025, 12, 31, 23, 0)))
    }
}
