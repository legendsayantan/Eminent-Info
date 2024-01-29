package com.legendsayantan.eminentinfo.utils

import android.content.Context
import android.os.Handler
import com.legendsayantan.eminentinfo.data.Account
import com.legendsayantan.eminentinfo.data.AccountAttendance
import com.legendsayantan.eminentinfo.data.Birthday
import com.legendsayantan.eminentinfo.data.DaySlots
import com.legendsayantan.eminentinfo.data.PeriodSlot
import com.legendsayantan.eminentinfo.data.SubjectAttendance
import com.legendsayantan.eminentinfo.data.TimeTable
import com.legendsayantan.eminentinfo.utils.Misc.Companion.beautifyCase
import com.legendsayantan.eminentinfo.utils.Misc.Companion.dateAsUnix
import com.legendsayantan.eminentinfo.utils.Misc.Companion.dateDifference
import com.legendsayantan.eminentinfo.utils.Misc.Companion.extractIntegers
import com.legendsayantan.eminentinfo.utils.Misc.Companion.getDayIndex
import com.legendsayantan.eminentinfo.utils.Misc.Companion.timeAsUnix
import com.legendsayantan.eminentinfo.utils.Misc.Companion.unParenthesis
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.collections.HashMap

/**
 * @author legendsayantan
 */
class Scrapers(val context: Context) {

    fun getBaseUrl(ID: String): String {
        return if (ID.contains("ECPT")) "https://ecpt.fedena.com" else "https://ecmt.fedena.com"
    }

    fun retrieveSessionKey(username: String, password: String, onSuccess: (Account?) -> Unit) {
        Thread {
            try {
                // Replace this URL with the actual URL of the login page
                val response: Connection.Response = Jsoup.connect(getBaseUrl(username))
                    .method(Connection.Method.POST)
                    .execute()

                val loginPage = response.parse()
                //set form data and submit
                val cookies = response.cookies()
                val formData = mapOf(
                    "authenticity_token" to extractAuthToken(loginPage.outerHtml()),
                    "user[username]" to username,
                    "user[password]" to password,
                    "commit" to "Login"
                )

                // Submit the form with the provided data
                val loginResponse: Connection.Response = Jsoup.connect(getBaseUrl(username))
                    .data(formData)
                    .method(Connection.Method.POST)
                    .cookies(cookies)  // Use cookies from the previous response
                    .execute()

                // Print the response or perform further actions on the logged-in page
                val loggedInPage = loginResponse.parse()
                val csrfToken =
                    loggedInPage.getElementsByTag("meta").find { it.attr("name") == "csrf-token" }
                        ?.attr("content") ?: ""
                val quickLinks = "${getBaseUrl(username)}/user/show_quick_links"
                val accessorResponse: Connection.Response = Jsoup.connect(quickLinks)
                    .cookies(response.cookies())
                    .referrer(loginResponse.url().toString())
                    .header("Origin", getBaseUrl(username))
                    .header("X-Csrf-Token", csrfToken)
                    .method(Connection.Method.POST)
                    .execute()
                val accessElement = accessorResponse.parse().getElementsByTag("a")
                    .find { it.attr("href").contains("/student/") }
                val accessor = accessElement?.attr("href")
                    ?.extractIntegers()
                    ?.get(0)
                    ?: 0

                val nameDisplay =
                    loggedInPage.getElementById("switch-student")?.getElementsByTag("a")
                        ?.find { it.attr("href").contains("profile") }
                //extract name
                Handler(context.mainLooper).post {
                    val name = nameDisplay?.text()?.beautifyCase()
                    if (name != null) {
                        onSuccess(
                            Account(
                                name,
                                username,
                                cookies["_fedena_session_"] ?: "",
                                accessor,
                                csrfToken
                            )
                        )
                    } else onSuccess(null)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun retrieveTimetable(account: Account, callback: (TimeTable?) -> Unit) {
        val usedUrl = "${getBaseUrl(account.ID)}/timetable/student_view/${account.accessor}"
        Thread {
            try {
                val response: Connection.Response = Jsoup.connect(usedUrl)
                    .header("Origin", getBaseUrl(account.ID))
                    .header("X-csrf-token", account.csrfToken)
                    .method(Connection.Method.GET)
                    .cookie("_fedena_session_", account.sessionKey)
                    .execute()
                val doc = response.parse()
                val days = doc.getElementById("table-days")?.getElementsByTag("tr")
                val table = doc.getElementById("table")
                var timeTable = TimeTable(Array(7) { _ -> DaySlots(arrayListOf()) }, hashMapOf())

                table?.getElementsByTag("tr")?.forEachIndexed { index, tr ->
                    val slots = arrayListOf<PeriodSlot>()
                    tr.getElementsByTag("td").forEach { td ->
                        slots.add(
                            td.getElementsByClass("class_timings").let { timing ->
                                if (timing.size > 0) {
                                    val temp = td.getElementsByClass("class_timing_tooltip")[0]
                                    PeriodSlot(
                                        timeAsUnix(
                                            timing[0].text().split("-")[0].trim()
                                        ),
                                        temp.getElementsByClass(
                                            "sub-line"
                                        )[0].text().trim().beautifyCase(),
                                        temp.getElementsByClass("emp-line").joinToString {
                                            it.text().trim().unParenthesis().beautifyCase()
                                        }
                                    )
                                } else if (td.getElementsByClass("blank_timings").size > 0) {
                                    PeriodSlot(
                                        timeAsUnix(
                                            td.getElementsByClass("blank_timings")[0].text()
                                                .split("-")[0].trim()
                                        ),
                                        td.getElementsByClass("subject1")[0].text().trim()
                                            .beautifyCase(),
                                        td.getElementsByClass("employee")[0].text().trim()
                                            .beautifyCase()
                                    )

                                } else PeriodSlot(
                                    slots.last().startTime + PeriodSlot.defaultDuration,
                                    "Break",
                                    ""
                                )
                            }

                        )
                    }
                    val dayIndex = days?.get(index)?.let { getDayIndex(it.text()) }
                    timeTable.daySlots[dayIndex!!] = DaySlots(slots)
                }
                retrieveHolidays(account) {
                    timeTable.holidays = it
                    callback(TimeTable.optimiseTable(timeTable))
                }
            } catch (e: IOException) {
                callback(null)
                e.printStackTrace()
            }
        }.start()
    }

    fun retrieveHolidays(acc: Account, callback: (HashMap<Long, String>) -> Unit) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val holidays = hashMapOf<Long, String>()
        Thread {
            for (i in 0..14) {
                val usedUrl = "${getBaseUrl(acc.ID)}/calendar/show_holiday_event_tooltip/${
                    SimpleDateFormat("yyyy-MM-dd").format(calendar.timeInMillis)
                }"
                try {
                    val response: Connection.Response = Jsoup.connect(usedUrl)
                        .header("Origin", getBaseUrl(acc.ID))
                        .method(Connection.Method.GET)
                        .cookie("_fedena_session_", acc.sessionKey)
                        .execute()

                    val html = response.body().split("\")")[0]
                        .replace("Element.update(\"tooltip_header\", \"", "")
                        .replace("\\n", "")
                        .replace("\\", "")

                    if (html.isNotEmpty()) {
                        val element = Element("div")
                        element.html(html)

                        holidays[calendar.timeInMillis] =
                            element.getElementsByClass("desc")[0].text().beautifyCase()
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            callback(holidays)
        }.start()
    }

    fun retrieveAttendance(
        acc: Account,
        fullReport: Boolean = true,
        callback: (AccountAttendance?) -> Unit
    ) {
        val usedUrl = "${getBaseUrl(acc.ID)}/student_attendance/student/${acc.accessor}"
        Thread {
            try {
                val response: Connection.Response = Jsoup.connect(usedUrl)
                    .header("Origin", getBaseUrl(acc.ID))
                    .method(Connection.Method.GET)
                    .cookie("_fedena_session_", acc.sessionKey)
                    .execute()
                val doc = response.parse()
                val subjectSelector = doc.getElementById("advance_search_subject_id")
                val accountAttendance = AccountAttendance(
                    arrayListOf(),
                    hashMapOf(),
                    System.currentTimeMillis()
                )
                subjectSelector?.children()?.forEach { option ->
                    val c = Calendar.getInstance()
                    c.add(Calendar.MONTH, 1)
                    val subAttendance = SubjectAttendance(option.text().beautifyCase(), hashMapOf())
                    for (i in 0..6) {
                        var formData: Map<String, String?>
                        if (i == 0) {
                            formData = mapOf(
                                "authenticity_token" to extractAuthToken(doc.html()),
                                "advance_search[subject_id]" to option.`val`(),
                                "advance_search[mode]" to "Overall",
                                "commit" to "► OK"
                            )
                        } else {
                            c.add(Calendar.MONTH, -1)
                            formData = mapOf(
                                "authenticity_token" to extractAuthToken(doc.html()),
                                "advance_search[subject_id]" to option.`val`(),
                                "advance_search[mode]" to "Monthly",
                                "advance_search[month]" to (c.get(Calendar.MONTH) + 1).toString(),
                                "advance_search[year]" to c.get(Calendar.YEAR).toString(),
                                "commit" to "► OK"
                            )
                        }

                        // Submit the form with the provided data
                        val formResponse: Connection.Response = Jsoup.connect(usedUrl)
                            .data(formData)
                            .header("X-Csrf-Token", acc.csrfToken)
                            .method(Connection.Method.POST)
                            .cookie(
                                "_fedena_session_",
                                acc.sessionKey
                            )  // Use cookies from the previous response
                            .execute()


                        val html = formResponse.body().split("\")")[0]
                            .replace("Element.update(\"report\", \"", "")
                            .replace("\\n", "")
                            .replace("\\", "")

                        val layout = Element("div")
                        layout.html(html)

                        if (option.text().contains("all", true) && i == 0) {
                            val table =
                                layout.getElementById("leave_reports")?.getElementById("listing")
                            val absences = table?.getElementsByTag("tr")?.let { it.subList(2, it.size) }
                            absences?.forEachIndexed { index, element ->
                                val date = dateAsUnix(
                                    element.getElementsByClass("col-3")[0].text().trim()
                                ) + index
                                val subject = element.getElementsByClass("col-3")[2].text()
                                if (dateDifference(System.currentTimeMillis(),date) < 7 &&
                                    accountAttendance.absence.keys.find {
                                        dateDifference(it, date) == 0 &&
                                                accountAttendance.absence[it] == subject
                                    } == null
                                ) accountAttendance.absence[date] = subject
                            }
                            if (!fullReport) return@forEach
                        }
                        val attended =
                            layout.getElementsByClass("col-20").last()?.text()?.replace("%", "")
                                ?.toFloatOrNull()
                        subAttendance.attend[if (i == 0) 0 else (c.get(Calendar.YEAR) * 12 + c.get(
                            Calendar.MONTH
                        ))] = attended ?: 0f
                    }
                    accountAttendance.subjects.add(subAttendance)
                }
                callback(accountAttendance)
            }catch (e:Exception){
                callback(null)
            }
        }.start()

    }

    fun getBirthdays(
        account: Account,
        calendar: Calendar,
        callback: (ArrayList<Birthday>?) -> Unit
    ) {
        val usedUrl = "${getBaseUrl(account.ID)}/data_palettes/update_palette"
        Thread {
            try {
                val formData = mapOf(
                    "palette[cur_date]" to "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${
                        calendar.get(
                            Calendar.DAY_OF_MONTH
                        )
                    }",
                    "palette[palette_name]" to "birthdays"
                )
                val response: Connection.Response = Jsoup.connect(usedUrl)
                    .data(formData)
                    .cookie("_fedena_session_", account.sessionKey)
                    .referrer(getBaseUrl(account.ID) + "/data_palettes")
                    .header("Origin", getBaseUrl(account.ID))
                    .header("X-Csrf-Token", account.csrfToken)
                    .method(Connection.Method.POST)
                    .execute()
                val list = arrayListOf<Birthday>()
                val doc = response.parse()
                doc.getElementsByClass("birthday-subcontent").forEach {
                    list.add(
                        Birthday(
                            it.getElementsByClass("subcontent-header")[0].text().beautifyCase(),
                            it.getElementsByClass("subcontent-info")[0].text()
                                .replace("Batch :", "")
                                .replace("Semester", "Sem")
                                .replace(" - ", " · ")
                                .trim(),
                            it.getElementsByTag("img")[0].attr("src")
                        )
                    )
                }
                callback(list)
            } catch (e: IOException) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    fun getNews(
        account: Account, pastDays: Int = 7,
        callback: (HashMap<Long, String>?) -> Unit
    ) {
        val usedUrl = "${getBaseUrl(account.ID)}/data_palettes/update_palette"
        Thread {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR, 0)
                calendar.set(Calendar.MINUTE, 0)
                val list = hashMapOf<Long, String>()
                for (i in 0..pastDays) {
                    val formData = mapOf(
                        "palette[cur_date]" to "${calendar.get(Calendar.YEAR)}-${
                            calendar.get(
                                Calendar.MONTH
                            ) + 1
                        }-${
                            calendar.get(
                                Calendar.DAY_OF_MONTH
                            )
                        }",
                        "palette[palette_name]" to "news"
                    )
                    val response: Connection.Response = Jsoup.connect(usedUrl)
                        .data(formData)
                        .cookie("_fedena_session_", account.sessionKey)
                        .referrer(getBaseUrl(account.ID) + "/data_palettes")
                        .header("Origin", getBaseUrl(account.ID))
                        .header("X-Csrf-Token", account.csrfToken)
                        .method(Connection.Method.POST)
                        .execute()
                    val doc = response.parse()
                    doc.getElementsByClass("portlet-subcontent").forEach {
                        if (it.getElementsByTag("a").size > 0) {
                            val loadNotice =
                                getBaseUrl(account.ID) + it.getElementsByTag("a")[0]?.attr("href")
                            val notice = Jsoup.connect(loadNotice)
                                .cookie("_fedena_session_", account.sessionKey)
                                .referrer(getBaseUrl(account.ID) + "/data_palettes")
                                .header("Origin", getBaseUrl(account.ID))
                                .method(Connection.Method.GET)
                                .execute()
                            list[calendar.timeInMillis] =
                                notice.parse().getElementById("attachments_list")
                                    ?.getElementsByTag("a")
                                    ?.get(0)
                                    ?.attr("href") ?: ""
                            calendar.add(Calendar.MINUTE, 1)
                        }
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                callback(list)
            } catch (e: IOException) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    fun getMoreInfo(account: Account, callback: (String?) -> Unit) {
        val usedUrl = "${getBaseUrl(account.ID)}/student/profile/${account.accessor}"
        Thread {
            try {
                val response: Connection.Response = Jsoup.connect(usedUrl)
                    .cookie("_fedena_session_", account.sessionKey)
                    .referrer(getBaseUrl(account.ID) + "/data_palettes")
                    .header("Origin", getBaseUrl(account.ID))
                    .method(Connection.Method.GET)
                    .execute()
                val doc = response.parse()
                val info = doc.getElementById("student_main_info")?.getElementsByTag("h4")
                callback(info?.subList(0, 2)?.joinToString { it.text() + "\n" }
                    ?.replace("Course :", "")?.replace("Batch :", "")?.trim())
            } catch (e: IOException) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    fun logOut(account: Account, callback: (Boolean) -> Unit) {
        val usedUrl = "${getBaseUrl(account.ID)}/user/logout"
        Thread {
            try {
                val response: Connection.Response = Jsoup.connect(usedUrl)
                    .cookie("_fedena_session_", account.sessionKey)
                    .referrer(getBaseUrl(account.ID) + "/data_palettes")
                    .header("Origin", getBaseUrl(account.ID))
                    .method(Connection.Method.GET)
                    .execute()
                callback(response.statusCode() == 200)
            } catch (e: IOException) {
                e.printStackTrace()
                callback(false)
            }
        }.start()
    }

    private fun extractAuthToken(htmlContent: String): String? {
        val regex =
            Regex("""<input\s+name="authenticity_token"\s+type="hidden"\s+value="([^"]+)"""")
        val matchResult = regex.find(htmlContent)
        return matchResult?.groupValues?.get(1)
    }
}