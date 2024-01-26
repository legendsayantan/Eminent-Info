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
import kotlin.math.abs

class BirthdayNotice : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val appStorage = AppStorage(context)
            val sentNotices = arrayListOf<String>()
            appStorage.getAllAccounts().forEach { acc ->
                appStorage.getNotificationSettings(acc.ID).let { settings ->
                    if (settings.isEmpty()) return@forEach
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
                                    it.batch.contains(acc.batch.trim(), true) && it.batch.contains(
                                        acc.course.trim(),
                                        true
                                    )
                                }
                                if (filtered.isNotEmpty()) {
                                    context.sendNotification(
                                        "Birthdays tomorrow :",
                                        filtered.joinToString("\n") { it.name },
                                        ("${abs((acc.batch + acc.course).hashCode() / 10)}0").toInt()
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
                                    if (!sentNotices.contains(notice.value)) {
                                        sentNotices.add(notice.value)
                                        context.sendNotification(
                                            "Eminent published a Notice today",
                                            "Click to open",
                                            ("${abs(notice.value.hashCode()/10)}1").toInt(),
                                            intent = PdfViewerActivity.launchPdfFromUrl(
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
}
