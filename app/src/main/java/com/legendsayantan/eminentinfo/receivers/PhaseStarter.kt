package com.legendsayantan.eminentinfo.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.legendsayantan.eminentinfo.data.PeriodSlot
import java.util.Calendar

class PhaseStarter : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.setEnablePhaseNoti(true)
    }

    companion object{
        fun Context.setEnablePhaseNoti(enable: Boolean) {
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
                    calendar.timeInMillis,
                    PeriodSlot.defaultDuration,
                    periodIntent
                )
            } else {
                alarmManager.cancel(periodIntent)
            }
        }
    }
}