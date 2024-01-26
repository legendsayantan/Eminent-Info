package com.legendsayantan.eminentinfo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.legendsayantan.eminentinfo.utils.AppStorage
import com.legendsayantan.eminentinfo.utils.Misc.Companion.dateDifference
import com.legendsayantan.eminentinfo.utils.Misc.Companion.sendNotification
import com.legendsayantan.eminentinfo.utils.Scrapers
import java.text.SimpleDateFormat
import java.time.Instant
import kotlin.math.abs

class AbsenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val storage = AppStorage(context)
        storage.getAllAccounts().forEach { acc ->
            storage.getNotificationSettings(acc.ID).let { settings ->
                if (settings.isEmpty()) return@forEach
                if (settings[3]) {
                    //check absence
                    Scrapers(context).retrieveAttendance(acc) { att ->
                        if (att != null) {
                            storage.saveAttendance(acc.ID,att)
                            val absent =
                                att.absence.filter { dateDifference(System.currentTimeMillis(),it.key)==0}
                            if (absent.isNotEmpty()) {
                                context.sendNotification(
                                    "${acc.name} was marked as absent today in the following :",
                                    absent.entries.joinToString { it.value },
                                    ("${abs((acc.name).hashCode() / 10)}3").toInt()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}