package com.appetizers.spotra.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

const val WATERLOO_TIME_ZONE = "America/Toronto"

data class DailyOperatingHours(
    val opens: LocalTime? = null,
    val closes: LocalTime? = null,
    val open24Hours: Boolean = false,
)

data class WeeklyOperatingHours(
    val days: Map<DayOfWeek, DailyOperatingHours>,
    val timeZone: String = WATERLOO_TIME_ZONE,
) {
    fun statusLabelAt(instant: Instant = Instant.now()): String {
        val zone = runCatching { ZoneId.of(timeZone) }
            .getOrElse { ZoneId.of(WATERLOO_TIME_ZONE) }
        val now = instant.atZone(zone)
        val intervals = (-1L..7L).mapNotNull { dayOffset ->
            intervalFor(now.toLocalDate().plusDays(dayOffset), zone)
        }

        val current = intervals.firstOrNull { !now.isBefore(it.startsAt) && now.isBefore(it.endsAt) }
        if (current != null) {
            return if (current.open24Hours) "Open 24 hours" else "Open until ${current.endsAt.toDisplayTime()}"
        }

        val next = intervals
            .asSequence()
            .filter { it.startsAt.isAfter(now) }
            .minByOrNull { it.startsAt }
            ?: return "Closed"

        val dayPrefix = when (next.startsAt.toLocalDate()) {
            now.toLocalDate() -> ""
            now.toLocalDate().plusDays(1) -> "tomorrow "
            else -> "${next.startsAt.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} "
        }
        return "Closed • Opens ${dayPrefix}at ${next.startsAt.toDisplayTime()}"
    }

    private fun intervalFor(date: LocalDate, zone: ZoneId): OpenInterval? {
        val hours = days[date.dayOfWeek] ?: return null
        if (hours.open24Hours) {
            return OpenInterval(
                startsAt = date.atStartOfDay(zone),
                endsAt = date.plusDays(1).atStartOfDay(zone),
                open24Hours = true,
            )
        }

        val opens = hours.opens ?: return null
        val closes = hours.closes ?: return null
        val startsAt = date.atTime(opens).atZone(zone)
        val closeDate = if (closes <= opens) date.plusDays(1) else date
        return OpenInterval(
            startsAt = startsAt,
            endsAt = closeDate.atTime(closes).atZone(zone),
            open24Hours = false,
        )
    }
}

private data class OpenInterval(
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime,
    val open24Hours: Boolean,
)

private val fullTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val hourTimeFormatter = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH)

private fun ZonedDateTime.toDisplayTime(): String =
    format(if (minute == 0) hourTimeFormatter else fullTimeFormatter)

