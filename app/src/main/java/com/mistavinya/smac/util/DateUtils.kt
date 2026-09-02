package com.mistavinya.smac.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    fun formatRelativeTime(timestamp: Long): String {
        val now = LocalDateTime.now()
        val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())

        val diffMinutes = ChronoUnit.MINUTES.between(time, now)
        val diffHours = ChronoUnit.HOURS.between(time, now)

        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 && time.dayOfMonth == now.dayOfMonth -> "${diffHours}h ago"
            time.toLocalDate() == now.toLocalDate() -> "Today, ${time.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            time.toLocalDate() == now.minusDays(1).toLocalDate() -> "Yesterday, ${time.format(DateTimeFormatter.ofPattern("h:mm a"))}"
            else -> time.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"))
        }
    }

    fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    fun getCurrentTime(): String {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }
}
