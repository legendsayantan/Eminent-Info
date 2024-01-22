package com.legendsayantan.eminentalerts.utils

import android.graphics.Color
import com.legendsayantan.eminentalerts.data.PeriodSlot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
            val pattern = "hh:mm a"
            val sdf = SimpleDateFormat(pattern)

            try {
                // Parse the time string
                val date = sdf.parse(timeString)
                // Convert the Date object to Unix time (milliseconds since January 1, 1970)
                return date?.time ?: -1L
            } catch (e: Exception) {
                // Handle parsing errors
                e.printStackTrace()
                return -1L
            }
        }
        fun shortMonth(monthNumber: Int): String {
            val monthAbbreviations = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            return monthAbbreviations[monthNumber - 1]
        }

        fun relativeTime(t: Long,now:Long): String {
            var time = t
            val c = Calendar.getInstance()
            c.timeInMillis = abs(now - time)
            return if (time < now) {
                time += PeriodSlot.duration
                if (time > now) {
                    if((now-t)>(PeriodSlot.duration/2)) {
                        c.timeInMillis = abs(now - time)
                        c.get(Calendar.MINUTE).toString()+" more minutes"
                    }else{
                        "Started "+c.get(Calendar.MINUTE).toString()+" min ago"
                    }
                }
                else {
                    (   "Finished "+
                        if (c.get(Calendar.HOUR_OF_DAY) > 0) c.get(Calendar.HOUR_OF_DAY).toString() + "h "
                        else ""
                    ) + c.get(Calendar.MINUTE).toString() + "min ago"
                }
            }else{
                "In " + (
                    if (c.get(Calendar.HOUR_OF_DAY) > 0) c.get(Calendar.HOUR_OF_DAY).toString() + "h "
                    else ""
                ).toString() + c.get(Calendar.MINUTE).toString() + "min"
            }
            return ""
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
    }
}