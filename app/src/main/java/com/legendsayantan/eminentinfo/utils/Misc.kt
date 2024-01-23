package com.legendsayantan.eminentinfo.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * @author legendsayantan
 */
class Misc {
    companion object {
        fun String.extractIntegers(): List<Int> {
            val regex = "\\d+".toRegex()
            val matches = regex.findAll(this)
            return matches.map { it.value.toInt() }.toList()
        }

        fun getDayIndex(day: String): Int {
            return when (day.lowercase()) {
                "sun" -> 0
                "mon" -> 1
                "tue" -> 2
                "wed" -> 3
                "thu" -> 4
                "fri" -> 5
                "sat" -> 6
                else -> -1 // or throw an exception for invalid input
            }
        }

        fun timeAsUnix(timeString: String): Long {
            val sdf = SimpleDateFormat("hh:mm a")
            return try {
                // Parse the time string
                val date = sdf.parse(timeString)
                // Convert the Date object to Unix time (milliseconds since January 1, 1970)
                date?.time ?: -1L
            } catch (e: Exception) {
                // Handle parsing errors
                e.printStackTrace()
                -1L
            }
        }
        fun shortMonth(monthNumber: Int): String {
            val monthAbbreviations = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            return monthAbbreviations[monthNumber - 1]
        }

        fun relativeTime(startTime: Long, currentTime:Long, duration: Long): String {
            val elapsedTime = currentTime - startTime
            val remainingTime = startTime + duration - currentTime

            val absoluteElapsedTime = abs(elapsedTime)
            val absoluteRemainingTime = abs(remainingTime)

            val isEventInProgress = elapsedTime in 0..< duration
            val isEventEndingSoon = absoluteRemainingTime <= duration/3
            val isEventCompleted = elapsedTime >= duration


            return when {
                isEventInProgress -> "Started ${formatTime(absoluteElapsedTime)} ago"
                isEventCompleted -> "Ended ${formatTime(absoluteRemainingTime)} ago"
                isEventEndingSoon -> "Ends in ${formatTime(absoluteRemainingTime)}"
                else -> "Starts in ${formatTime(absoluteElapsedTime)}"
            }
        }
        fun formatTime(timeInMillis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60

            return "${if (hours > 0) "${hours}h " else ""}${minutes}m"
        }

        fun generateColor(value: Float): Int {
            // Ensure value is within the range [0, 100]
            val adjustedValue = when {
                value < 0f -> 0f
                value > 100f -> 100f
                else -> value
            }

            // Calculate hue based on the adjusted value
            val hue = (adjustedValue * 1f)-30 // Adjust multiplier as needed

            // Create a color using HSL values
            return Color.HSVToColor(floatArrayOf(hue, 0.75f, 1f))
        }

        fun String.beautifyCase(): String {
            return split(" ").joinToString(" ") { part ->
                part.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
        fun millisecondsSinceMidnight(): Long {
            val calendar = Calendar.getInstance()
            val now = System.currentTimeMillis()

            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            return now - calendar.timeInMillis
        }

        fun Context.launchUrlInBrowser(url: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
}