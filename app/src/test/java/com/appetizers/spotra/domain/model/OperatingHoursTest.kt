package com.appetizers.spotra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

class OperatingHoursTest {
    @Test
    fun `shows closing time while open`() {
        val hours = weekly(
            DayOfWeek.MONDAY to day("08:00", "23:00")
        )

        assertEquals(
            "Open until 11 PM",
            hours.statusLabelAt(Instant.parse("2026-01-05T15:00:00Z"))
        )
    }

    @Test
    fun `shows today's next opening time before opening`() {
        val hours = weekly(
            DayOfWeek.MONDAY to day("08:00", "23:00")
        )

        assertEquals(
            "Closed • Opens at 8 AM",
            hours.statusLabelAt(Instant.parse("2026-01-05T12:00:00Z"))
        )
    }

    @Test
    fun `uses Toronto daylight saving time`() {
        val hours = weekly(
            DayOfWeek.SUNDAY to day("08:00", "23:00")
        )

        assertEquals(
            "Open until 11 PM",
            hours.statusLabelAt(Instant.parse("2026-07-06T02:00:00Z"))
        )
    }

    @Test
    fun `supports overnight operating hours`() {
        val hours = weekly(
            DayOfWeek.MONDAY to day("20:00", "02:00")
        )

        assertEquals(
            "Open until 2 AM",
            hours.statusLabelAt(Instant.parse("2026-01-06T06:00:00Z"))
        )
    }

    @Test
    fun `supports open 24 hours`() {
        val hours = weekly(
            DayOfWeek.MONDAY to DailyOperatingHours(open24Hours = true)
        )

        assertEquals(
            "Open 24 hours",
            hours.statusLabelAt(Instant.parse("2026-01-05T18:00:00Z"))
        )
    }

    private fun weekly(vararg days: Pair<DayOfWeek, DailyOperatingHours>) =
        WeeklyOperatingHours(days = days.toMap())

    private fun day(opens: String, closes: String) = DailyOperatingHours(
        opens = LocalTime.parse(opens),
        closes = LocalTime.parse(closes),
    )
}

