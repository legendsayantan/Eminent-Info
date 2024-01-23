package com.legendsayantan.eminentinfo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.legendsayantan.eminentinfo.utils.AppStorage
import com.legendsayantan.eminentinfo.utils.Misc.Companion.sendNotification
import com.legendsayantan.eminentinfo.utils.Scrapers
import com.rajat.pdfviewer.PdfViewerActivity
import com.rajat.pdfviewer.util.saveTo
import java.text.SimpleDateFormat
import java.util.Calendar

class BirthdayNotice : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val appStorage = AppStorage(context)
            appStorage.getAllAccounts().forEach { acc ->
                appStorage.getNotificationSettings(acc.ID).let { settings ->
                    if (settings[1]) {
                        //check birthday
                        val c = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        Scrapers(context).getBirthdays(
                            acc,
                            c
                        ) { birthdays ->
                            if (!birthdays.isNullOrEmpty()) {
                                val filtered = birthdays.filter {
                                    it.batch.contains(acc.batch.trim(),true) && it.batch.contains(acc.course.trim(),true)
                                }
                                if (filtered.isNotEmpty()) {
                                    context.sendNotification(
                                        "Birthdays on ${SimpleDateFormat("DD MMM YYYY").format(c.time)} :",
                                        filtered.joinToString("\n") { it.name },
                                        1
                                    )
                                }
                            }
                        }
                    }
                    if (settings[2]) {
                        //check notices
                        Scrapers(context).getNews(acc, 0) { notices ->
                            if (!notices.isNullOrEmpty()) {
                                notices.forEach { notice ->
                                    context.sendNotification(
                                        "Notice from Eminent",
                                        "Click to open",
                                        2,
                                        PdfViewerActivity.launchPdfFromUrl(
                                            context,
                                            notice.value,
                                            "Notice - ${SimpleDateFormat("DD/MM/YYYY").format(notice.key)}",
                                            saveTo = saveTo.ASK_EVERYTIME,
                                            enableDownload = true
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
