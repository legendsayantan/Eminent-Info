package com.legendsayantan.eminentinfo.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.legendsayantan.eminentinfo.data.DaySlots
import com.legendsayantan.eminentinfo.data.PeriodSlot
import com.legendsayantan.eminentinfo.data.TimeTable
import com.legendsayantan.eminentinfo.utils.AppStorage
import com.legendsayantan.eminentinfo.utils.Misc.Companion.sendNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.abs

class PhaseNotifier : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val storage = AppStorage(context)
        val activeSlotsCollection =
            storage.getAllAccounts().filter { acc ->
                storage.getNotificationSettings(acc.ID).let { it.isNotEmpty() && it[0] }
            }.map {
                getActiveSlots(
                    storage.getTimeTable(it.ID)
                )
            }
        val currentSlotsCollection =
            activeSlotsCollection.filter { it.first != null }.map { it.first!! }
        val nextSlotsCollection =
            activeSlotsCollection.filter { it.second != null }.map { it.second!! }
        if (currentSlotsCollection.isNotEmpty()) {
            val now = currentSlotsCollection.sortedBy { it.startTime }[0]
            val next =
                nextSlotsCollection.let { slot -> if (slot.isNotEmpty()) slot.sortedBy { it.startTime }[0] else null }
            if (now.subject.split("(")[0].isNotEmpty()) {
                val sdf = SimpleDateFormat("hh:mm")
                context.sendNotification(
                    "Now : ${now.subject.split("(")[0].trim()} ${now.host.let { if (it.isNotEmpty()) "- $it" else "" }}",
                    next?.let {
                        "Next at " +
                                sdf.format(it.startTime) +
                                " : " +
                                it.subject.split("(")[0].trim() + " - " + next.host
                    } ?: ("Ends at " + sdf.format(now.startTime + now.duration)),
                    "${abs(now.subject.hashCode() / 10)}2".toInt(),
                    timeout = now.duration
                )
            }
        } else {
            if (0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
                context.sendNotification("Debug Notification", "no periods now", 0)
        }
        if (nextSlotsCollection.isNotEmpty()) {
            context.setEnablePhaseNoti(
                true,
                nextSlotsCollection.sortedBy { it.startTime }[0].startTime,
                currentSlotsCollection.isEmpty()
            )
        } else {
            if (0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
                context.sendNotification("Debug Notification", "no periods next", 0)
        }
    }

    companion object {
        fun getActiveSlots(timeTable: TimeTable): Pair<PeriodSlot?, PeriodSlot?> {
            if(timeTable.daySlots.isEmpty())return Pair(null,null)
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
            var current: PeriodSlot? = null
            var currentSlotIndex = -1
            var todaySlots: DaySlots? = null
            var next: PeriodSlot?
            if (today > 0) {
                //classday
                todaySlots = timeTable.daySlots[today]
                val millisToday = Calendar.getInstance()
                    .apply {
                        set(Calendar.DATE, 1);
                        set(Calendar.MONTH, 0);
                        set(Calendar.YEAR, 1970)
                    }.timeInMillis
                currentSlotIndex =
                    todaySlots.periods.indexOfFirst { abs(millisToday - it.startTime) < it.duration }
                if (currentSlotIndex > 0) {
                    //classes running
                    current = todaySlots.periods[currentSlotIndex]
                }
            }
            if (currentSlotIndex >= 0 && currentSlotIndex + 1 < todaySlots!!.periods.size) {
                //more classes left today
                next = timeTable.daySlots[today].periods[currentSlotIndex + 1]
            } else {
                //find the next day with classes
                var nextDay = (today + 1) % 7
                while (timeTable.daySlots[nextDay].periods.isEmpty()) {
                    nextDay = (nextDay + 1) % 7
                }
                next = timeTable.daySlots[nextDay].periods[0]
            }
            return Pair(current, next)
        }

        fun Context.setEnablePhaseNoti(enable: Boolean, time: Long, nextDay: Boolean = false) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val periodIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(this, PhaseNotifier::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            if (enable) {
                val absCalendar = Calendar.getInstance()
                absCalendar.timeInMillis = time
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.HOUR_OF_DAY, absCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, absCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, 1)
                if (nextDay) calendar.add(Calendar.DAY_OF_YEAR, 1)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    if (time == -1L) calendar.timeInMillis else time,
                    periodIntent
                )
            } else {
                alarmManager.cancel(periodIntent)
            }
        }
    }
}