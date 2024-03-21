package com.legendsayantan.eminentinfo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.legendsayantan.eminentinfo.NoticeView
import com.legendsayantan.eminentinfo.utils.AppStorage
import com.legendsayantan.eminentinfo.utils.Misc
import com.legendsayantan.eminentinfo.utils.Misc.Companion.combineHashMaps
import com.legendsayantan.eminentinfo.utils.Misc.Companion.dataAge
import com.legendsayantan.eminentinfo.utils.Misc.Companion.sendNotification
import com.legendsayantan.eminentinfo.utils.Scrapers
import java.util.Calendar
import java.util.Date
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
                        val cachedNotices = appStorage.getNotices(acc.ID)
                        Scrapers(context).getNews(
                            acc,
                            dataAge(cachedNotices).coerceAtMost(15)
                        ) { notices ->
                            if (!notices.isNullOrEmpty()) {
                                appStorage.saveNotices(
                                    acc.ID,
                                    combineHashMaps(notices, cachedNotices).apply {
                                        entries.removeIf { it.key < (System.currentTimeMillis() - 1296000000) }
                                    })
                                notices.filter {
                                    Calendar.getInstance().apply { timeInMillis = it.key }
                                        .get(Calendar.DAY_OF_YEAR) == Calendar.getInstance()
                                        .apply { timeInMillis = System.currentTimeMillis() }
                                        .get(Calendar.DAY_OF_YEAR)
                                }.forEach { notice ->
                                    if (!sentNotices.contains(notice.value)) {
                                        sentNotices.add(notice.value)
                                        context.sendNotification(
                                            "Eminent published a Notice today",
                                            Misc.extractNoticeName(notice.value) ?: "Click to view",
                                            ("${abs(notice.value.hashCode() / 10)}1").toInt(),
                                            intent = Intent(context, NoticeView::class.java).apply {
                                                putExtra("date", notice.key)
                                                putExtra("url", notice.value)
                                            }
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
