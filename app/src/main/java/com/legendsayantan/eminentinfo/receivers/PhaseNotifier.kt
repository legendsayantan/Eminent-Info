package com.legendsayantan.eminentinfo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.legendsayantan.eminentinfo.data.PeriodSlot
import com.legendsayantan.eminentinfo.data.TimeTable
import com.legendsayantan.eminentinfo.receivers.PhaseStarter.Companion.setEnablePhaseNoti
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
                PhaseNotifier.getActiveSlots(
                    storage.getTimeTable(it.ID)
                )
            }
        val currentSlotsCollection = activeSlotsCollection.filter { it.isNotEmpty() }.map { it[0] }
        val nextSlotsCollection = activeSlotsCollection.filter { it.size>1 }.map { it[1] }
        if(currentSlotsCollection.isNotEmpty()){
            val now = currentSlotsCollection.sortedBy { it.startTime }[0]
            val next = nextSlotsCollection.let { slot -> if(slot.isNotEmpty()) slot.sortedBy { it.startTime }[0] else null }
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
        }else {
            if(0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
                context.sendNotification("Debug Notification","no periods now",0)
            context.setEnablePhaseNoti(false)
        }
    }
    companion object{
        fun getActiveSlots(timeTable: TimeTable):ArrayList<PeriodSlot>{
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
            if(today==0) return arrayListOf()
            val todaySlots = timeTable.daySlots[today]
            val millisToday = Calendar.getInstance()
                .apply {
                    set(Calendar.DATE, 1);
                    set(Calendar.MONTH, 0);
                    set(Calendar.YEAR, 1970)
                }.timeInMillis
            val slotIndex =
                todaySlots.periods.indexOfFirst { abs(millisToday - it.startTime) < it.duration }
            if (slotIndex < 0) return arrayListOf()
            val list = arrayListOf(todaySlots.periods[slotIndex])
            if (slotIndex < todaySlots.periods.size - 1) list.add(todaySlots.periods[slotIndex + 1])
            return list
        }
    }
}