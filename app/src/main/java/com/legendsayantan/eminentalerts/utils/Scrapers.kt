package com.legendsayantan.eminentalerts.utils

import android.app.Activity
import com.legendsayantan.eminentalerts.data.Account
import com.legendsayantan.eminentalerts.data.AccountAttendance
import com.legendsayantan.eminentalerts.data.DaySlots
import com.legendsayantan.eminentalerts.data.PeriodSlot
import com.legendsayantan.eminentalerts.data.SubjectAttendance
import com.legendsayantan.eminentalerts.data.TimeTable
import com.legendsayantan.eminentalerts.utils.Misc.Companion.beautifyCase
import com.legendsayantan.eminentalerts.utils.Misc.Companion.extractIntegers
import com.legendsayantan.eminentalerts.utils.Misc.Companion.getDayIndex
import com.legendsayantan.eminentalerts.utils.Misc.Companion.timeAsUnix
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * @author legendsayantan
 */
class Scrapers(val activity: Activity) {

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
                    .header("X-csrf-token", csrfToken)
                    .method(Connection.Method.POST)
                    .execute()
                val accessor = accessorResponse.parse().getElementsByTag("a").first()?.attr("href")
                    ?.extractIntegers()
                    ?.get(0)
                    ?: 0

                val nameDisplay =
                    loggedInPage.getElementById("switch-student")?.getElementsByTag("a")
                        ?.find { it.attr("href").contains("profile") }
                //extract name
                activity.runOnUiThread {
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
                var timeTable = TimeTable(Array(7) { _ -> DaySlots(arrayListOf()) })

                table?.getElementsByTag("tr")?.forEachIndexed { index, tr ->
                    val slots = arrayListOf<PeriodSlot>()
                    tr.getElementsByTag("td").forEach {
                        slots.add(
                            it.getElementsByClass("class_timings").let { timing ->
                                if (timing.size > 0) {
                                    PeriodSlot(
                                        timeAsUnix(
                                            timing[0].text().split("-")[0].trim()
                                        ),
                                        it.getElementsByClass("class_timing_tooltip")[0].getElementsByClass(
                                            "sub-line"
                                        )[0].text().trim().beautifyCase(),
                                        it.getElementsByClass("employee")[0].text().trim()
                                            .beautifyCase()
                                    )
                                } else if (it.getElementsByClass("blank_timings").size > 0) {
                                    PeriodSlot(
                                        timeAsUnix(
                                            it.getElementsByClass("blank_timings")[0].text()
                                                .split("-")[0].trim()
                                        ),
                                        it.getElementsByClass("subject1")[0].text().trim()
                                            .beautifyCase(),
                                        it.getElementsByClass("employee")[0].text().trim()
                                            .beautifyCase()
                                    )

                                } else PeriodSlot(
                                    slots.last().startTime + PeriodSlot.duration,
                                    "Break",
                                    ""
                                )
                            }

                        )
                    }
                    val dayIndex = days?.get(index)?.let { getDayIndex(it.text()) }
                    timeTable.daySlots[dayIndex!!] = DaySlots(slots)
                }
                callback(timeTable)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun retrieveAttendance(acc: Account, callback: (AccountAttendance?) -> Unit) {
        val usedUrl = "${getBaseUrl(acc.ID)}/student_attendance/student/${acc.accessor}"
        Thread {
            val response: Connection.Response = Jsoup.connect(usedUrl)
                .header("Origin", getBaseUrl(acc.ID))
                .method(Connection.Method.GET)
                .cookie("_fedena_session_", acc.sessionKey)
                .execute()
            val doc = response.parse()
            val subjectSelector = doc.getElementById("advance_search_subject_id")
            val accountAttendance = AccountAttendance(arrayListOf())
            subjectSelector?.children()?.forEach { option ->
                val c = Calendar.getInstance()
                c.add(Calendar.MONTH, 1)
                val subAttendance = SubjectAttendance(option.text().beautifyCase(), hashMapOf())
                for (i in 0..6) {
                    var formData = mapOf<String,String?>()
                    if(i==0){
                        formData = mapOf(
                            "authenticity_token" to extractAuthToken(doc.html()),
                            "advance_search[subject_id]" to option.`val`(),
                            "advance_search[mode]" to "Overall",
                            "commit" to "► OK"
                        )
                    }else{
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
                        .replace("Element.update(\"report\", \"","")
                        .replace("\\n", "")
                        .replace("\\","")

                    val layout = Element("div")
                    layout.html(html)

                    val attended = layout.getElementsByClass("col-20").last()?.text()?.replace("%","")?.toFloatOrNull()
                    subAttendance.attend[if(i==0) 0 else (c.get(Calendar.YEAR)*12+c.get(Calendar.MONTH))] = attended?:0f
                }
                accountAttendance.subjects.add(subAttendance)
            }
            callback(accountAttendance)

        }.start()

    }

    fun getAccessor(account: Account) {
        val usedUrl = "${getBaseUrl(account.ID)}/user/show_quick_links"
        Thread {
            try {
                val response: Connection.Response = Jsoup.connect(usedUrl)
                    .cookie("_fedena_session_", account.sessionKey)
                    .referrer(getBaseUrl(account.ID) + "/data_palettes")
                    .header("Origin", getBaseUrl(account.ID))
                    .method(Connection.Method.POST)
                    .execute()
                println(response.body())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun extractAuthToken(htmlContent: String): String? {
        val regex =
            Regex("""<input\s+name="authenticity_token"\s+type="hidden"\s+value="([^"]+)"""")
        val matchResult = regex.find(htmlContent)
        return matchResult?.groupValues?.get(1)
    }

    companion object {

    }
}