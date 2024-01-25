package com.legendsayantan.eminentinfo.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.legendsayantan.eminentinfo.utils.AppStorage
import com.legendsayantan.eminentinfo.utils.Misc.Companion.relativeTime
import com.legendsayantan.eminentinfo.utils.Misc.Companion.sendNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.abs

class PhaseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val storage = AppStorage(context)
        val batchesDone = arrayListOf<String>()
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val periodIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, PhaseReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        if (today == 0) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis,
                periodIntent
            )
            return
        }
        storage.getAllAccounts().forEach { acc ->
            if (batchesDone.contains(acc.batch + acc.course)) return@forEach
            else batchesDone.add(acc.batch + acc.course)
            val todaySlots = storage.getTimeTable(acc.ID).daySlots[today]
            val millisToday = Calendar.getInstance()
                .apply {
                    set(Calendar.DATE, 1);
                    set(Calendar.MONTH, 0);
                    set(Calendar.YEAR, 1970)
                }.timeInMillis
            val slotIndex =
                todaySlots.periods.indexOfFirst { abs(millisToday - it.startTime) < it.duration }
            val nextSlot =
                if (slotIndex < todaySlots.periods.size - 1) todaySlots.periods[slotIndex + 1] else null
            todaySlots.periods[slotIndex].let { now ->
                if (now.subject.split("(")[0].isNotEmpty()) {
                    context.sendNotification(
                        "Now : ${now.subject.split("(")[0].trim()} ${now.host.let { if(it.isNotEmpty()) "- $it" else "" }}",
                        nextSlot?.let {
                            "Next at " + SimpleDateFormat("hh:mm").format(it.startTime) + " : " +
                                    it.subject.split("(")[0].trim() + " - " + nextSlot.host
                        } ?: "Next : None",
                        "${abs(todaySlots.hashCode() / 10)}2".toInt(),
                        timeout = now.duration
                    )
                }
            }


            //re-schedule
            val unixtime = Calendar.getInstance().apply {
                val past = Calendar.getInstance().apply {
                    timeInMillis = nextSlot?.startTime
                        ?: storage.getTimeTable(acc.ID).daySlots[(today + 1) % 7].periods[0].startTime
                }
                set(Calendar.HOUR_OF_DAY, past.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, past.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                unixtime,
                periodIntent
            )
        }
    }
}