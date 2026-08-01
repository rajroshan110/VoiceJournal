package dev.voicejournal.util

import android.content.Context
import android.text.format.DateFormat
import dev.voicejournal.domain.model.TimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    fun formatTime(timestamp: Long, timeFormat: TimeFormat, context: Context): String {
        val date = Date(timestamp)
        return when (timeFormat) {
            TimeFormat.SYSTEM_DEFAULT -> DateFormat.getTimeFormat(context).format(date)
            TimeFormat.TWELVE_HOUR -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            TimeFormat.TWENTY_FOUR_HOUR -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
    }

    fun formatDateWithTime(timestamp: Long, timeFormat: TimeFormat, context: Context): String {
        val date = Date(timestamp)
        val mediumDate = DateFormat.getMediumDateFormat(context).format(date)
        val timeStr = formatTime(timestamp, timeFormat, context)
        return "$mediumDate · $timeStr"
    }

    fun formatHeaderDate(timestamp: Long): String {
        val date = Date(timestamp)
        return SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(date)
    }

    fun formatHeaderDateTime(timestamp: Long, timeFormat: TimeFormat, context: Context): String {
        val date = Date(timestamp)
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        val timeStr = formatTime(timestamp, timeFormat, context)
        return "$dateStr · $timeStr"
    }
}
