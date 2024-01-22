package com.legendsayantan.eminentalerts.utils

import com.legendsayantan.eminentalerts.data.PeriodSlot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

        fun relativeTime(t: Long,now:Long): String {
            var time = t
            return if (time < now) {
                time += PeriodSlot.duration
                if (time > now) "Now"
                else {
                    val c = Calendar.getInstance()
                    c.timeInMillis = now - time
                    (
                        if (c.get(Calendar.HOUR_OF_DAY) > 0) c.get(Calendar.HOUR_OF_DAY).toString() + "h "
                        else ""
                    ).toString() + c.get(Calendar.MINUTE).toString() + "min ago"
                }
            }else{
                val c = Calendar.getInstance()
                c.timeInMillis = time-now
                "In " + {
                    if (c.get(Calendar.HOUR_OF_DAY) > 0) c.get(Calendar.HOUR_OF_DAY).toString() + "h "
                    else ""
                }.toString() + c.get(Calendar.MINUTE).toString() + "min"
            }
            return ""
        }

        fun String.beautifyCase(): String {
            return split(" ").joinToString(" ") { part ->
                part.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
    }
}