package com.legendsayantan.eminentinfo.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.legendsayantan.eminentinfo.data.PeriodSlot
import com.legendsayantan.eminentinfo.utils.AppStorage
import com.legendsayantan.eminentinfo.utils.Misc.Companion.sendNotification
import com.rajat.pdfviewer.BuildConfig
import java.util.Calendar

class PhaseStarter : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val storage = AppStorage(context)
        val activeSlotsCollection =
            storage.getAllAccounts().filter { acc ->
                storage.getNotificationSettings(acc.ID).let { it.isNotEmpty() && it[0] }
            }.map {
                PhaseNotifier.getActiveSlots(
                    storage.getTimeTable(it.ID)
                )
            }
        val currentSlotsCollection = activeSlotsCollection.filter { it.isNotEmpty() }.map { it[0] }
        if (currentSlotsCollection.isNotEmpty()) {
            context.setEnablePhaseNoti(true, currentSlotsCollection.minOf { it.startTime })
            if (0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
                context.sendNotification("Debug Notification", "scheduling phasestarter", 0)
            return
        } else if (0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
            context.sendNotification("Debug Notification", "no periods now", 0)
    }

    companion object {
        fun Context.setEnablePhaseNoti(enable: Boolean, time: Long = -1L) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val periodIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(this, PhaseNotifier::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            if (enable) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 1)
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    if (time == -1L) calendar.timeInMillis else time,
                    PeriodSlot.defaultDuration,
                    periodIntent
                )
            } else {
                alarmManager.cancel(periodIntent)
            }
        }
    }
}