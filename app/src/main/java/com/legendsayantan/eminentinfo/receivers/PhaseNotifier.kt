package com.legendsayantan.eminentinfo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.legendsayantan.eminentinfo.receivers.PhaseStarter.Companion.setEnablePhaseNoti
import com.legendsayantan.eminentinfo.utils.AppStorage
import com.legendsayantan.eminentinfo.utils.Misc.Companion.sendNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.abs

class PhaseNotifier : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val storage = AppStorage(context)
        val batchesDone = arrayListOf<String>()
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        if (today == 0) {
            context.setEnablePhaseNoti(false)
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
            if (slotIndex < 0) {
                context.setEnablePhaseNoti(false)
                return
            }
            val nextSlot =
                if (slotIndex < todaySlots.periods.size - 1) todaySlots.periods[slotIndex + 1] else null
            todaySlots.periods[slotIndex].let { now ->
                if (now.subject.split("(")[0].isNotEmpty()) {
                    val sdf = SimpleDateFormat("hh:mm")
                    context.sendNotification(
                        "Now : ${now.subject.split("(")[0].trim()} ${now.host.let { if (it.isNotEmpty()) "- $it" else "" }}",
                        nextSlot?.let {
                            "Next at " +
                                    sdf.format(it.startTime) +
                                    " : " +
                                    it.subject.split("(")[0].trim() + " - " + nextSlot.host
                        } ?: ("Ends at " + sdf.format(now.startTime + now.duration)),
                        "${abs(todaySlots.hashCode() / 10)}2".toInt(),
                        timeout = now.duration
                    )
                }
            }

        }
    }
}