package com.legendsayantan.eminentinfo.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
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

        fun dateAsUnix(dateString: String): Long {
            val sdf = SimpleDateFormat("dd/MM/yyyy")
            return try {
                // Parse the time string
                val date = sdf.parse(dateString)
                // Convert the Date object to Unix time (milliseconds since January 1, 1970)
                date?.time ?: -1L
            } catch (e: Exception) {
                // Handle parsing errors
                e.printStackTrace()
                -1L
            }
        }

        fun shortMonth(monthNumber: Int): String {
            val monthAbbreviations = arrayOf(
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December"
            )
            return monthAbbreviations[monthNumber - 1]
        }

        fun relativeTime(startTime: Long, currentTime: Long, duration: Long): String {
            val elapsedTime = currentTime - startTime
            val remainingTime = startTime + duration - currentTime

            val absoluteElapsedTime = abs(elapsedTime)
            val absoluteRemainingTime = abs(remainingTime)

            val isEventInProgress = elapsedTime in 0..<duration
            val isEventEndingSoon = absoluteRemainingTime <= duration / 3
            val isEventCompleted = elapsedTime >= duration


            return when {
                isEventCompleted -> "Ended ${formatTime(absoluteRemainingTime)} ago"
                isEventEndingSoon -> "Ends in ${formatTime(absoluteRemainingTime)}"
                isEventInProgress -> "Started ${formatTime(absoluteElapsedTime)} ago"
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
            val hue = (adjustedValue * 1f) - 30 // Adjust multiplier as needed

            // Create a color using HSL values
            return Color.HSVToColor(floatArrayOf(hue, 0.75f, 1f))
        }

        fun String.beautifyCase(): String {
            return split(" ").joinToString(" ") { part ->
                part.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }

        fun String.unParenthesis(): String {
            val regex = Regex("\\([^)]*\\)")
            return replace(regex, "")
        }

        fun dateDifference(mainDate: Long, compareDate: Long): Int {
            val c1 = Calendar.getInstance().apply {
                timeInMillis = mainDate
            }
            val c2 = Calendar.getInstance().apply {
                timeInMillis = compareDate
            }
            return c1.get(Calendar.DAY_OF_YEAR) - c2.get(Calendar.DAY_OF_YEAR)
        }

        fun Context.launchUrlInBrowser(url: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        @SuppressLint("BatteryLife")
        fun Activity.requestIgnoreBatteryOptimizations() {
            if (!(getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                    packageName
                )
            ) {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse(
                            "package:$packageName"
                        )
                    )
                )
            }
        }

        @SuppressLint("MissingPermission")
        fun Context.sendNotification(
            title: String,
            message: String,
            id: Int,
            timeout: Long = 0,
            intent: Intent? = null
        ) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "$packageName.info",
                    "Info notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = "Eminent Info"
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                notificationManager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(this, "$packageName.info")
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(com.legendsayantan.eminentinfo.R.drawable.collegelogo)
                .setAutoCancel(true)
                .setSilent(true)
            if (intent != null) {
                val pendingIntent =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                notification.setContentIntent(pendingIntent)
            }
            if (timeout > 0) {
                notification.setTimeoutAfter(timeout)
            }
            notificationManager.notify(id, notification.build())
        }

        fun extractNoticeName(url: String): String? {
            val splits = Uri.decode(url)
                .split(".pdf")[0]
                    .split(".jpg")[0]
                        .split("/")
            splits[splits.size - 1].let {
                return if(it.contains("New_Doc") || it.length<5 ) null
                else it.replace("_notice", "",true)
                    .replace("notice", "",true)
                    .replace("_", " ")
                    .replace("semester","sem",true)
                    .replace("for","-",true)
                    .replace(" and",",",true)
                    .replace("\\(.*?\\)".toRegex(), "")
                    .beautifyCase()
            }
        }
        fun String.abbreviateNames(skip: Int = 0): String {
            val parts = split(", ")
            val abbreviatedParts = parts.take(skip.coerceAtMost(parts.size-1)) +
                    parts.drop(skip.coerceAtMost(parts.size-1)).map { it.split(" ")[0][0] + ". " + it.split(" ")[1] }
            return abbreviatedParts.joinToString(", ")
        }
    }
}