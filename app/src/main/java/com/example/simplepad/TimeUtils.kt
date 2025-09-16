package com.example.simplepad

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val m = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$m min ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val h = TimeUnit.MILLISECONDS.toHours(diff)
                "$h hr ago"
            }
            else -> {
                val fmt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                fmt.format(Date(timestamp))
            }
        }
    }
}
