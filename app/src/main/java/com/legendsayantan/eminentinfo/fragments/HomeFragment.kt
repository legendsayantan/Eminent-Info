package com.legendsayantan.eminentinfo.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.legendsayantan.eminentinfo.MainActivity
import com.legendsayantan.eminentinfo.R
import com.legendsayantan.eminentinfo.adapters.BirthdayListAdapter
import com.legendsayantan.eminentinfo.adapters.ViewPagerAdapter
import com.legendsayantan.eminentinfo.data.Account
import com.legendsayantan.eminentinfo.utils.Misc.Companion.beautifyCase
import com.legendsayantan.eminentinfo.utils.Misc.Companion.generateColor
import com.legendsayantan.eminentinfo.utils.Misc.Companion.relativeTime
import com.legendsayantan.eminentinfo.utils.Misc.Companion.shortMonth
import com.legendsayantan.eminentinfo.utils.Scrapers
import com.rajat.pdfviewer.PdfViewerActivity
import com.rajat.pdfviewer.util.saveTo
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Timer
import kotlin.concurrent.timerTask
import kotlin.math.abs

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    val scrapers by lazy { Scrapers(activity()) }
    val storage by lazy { activity().appStorage }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun getContext(): Context {
        return super.getContext() ?: contextCache
    }

    private lateinit var contextCache: Context

    private fun activity(): MainActivity {
        return (super.getActivity() ?: activityCache) as MainActivity
    }

    private lateinit var activityCache: Activity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contextCache = requireContext()
        activityCache = requireActivity()
        val acc = storage.getActiveAccount()
        val nameView = view.findViewById<TextView>(R.id.name)
        val infoView = view.findViewById<LinearLayout>(R.id.accInfo)
        view.findViewById<TextView>(R.id.ID).text = acc.ID
        nameView.text = acc.name
        infoView.visibility = View.GONE
        nameView.setOnClickListener {
            if (infoView.visibility == View.VISIBLE) {
                infoView.visibility = View.GONE
            } else infoView.visibility = View.VISIBLE
        }

        initialiseBirthdays(acc)
        initialiseNotice(acc)
        initialiseAttendance(acc)

    }

    override fun onResume() {
        super.onResume()
        initialiseTimeTable(storage.getActiveAccount())
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initialiseTimeTable(acc: Account, collapsed: Boolean = true) {
        val refershBtn = requireView().findViewById<ImageView>(R.id.timetableRefresh)
        val collapseBtn = requireView().findViewById<ImageView>(R.id.timetableCollapse)
        val container = requireView().findViewById<LinearLayout>(R.id.timetableContainer)
        val heading = requireView().findViewById<TextView>(R.id.timetableHeading)
        val table = storage.getTimeTable(acc.ID)
        container.removeAllViews()
        collapseBtn.rotation = if (collapsed) 90f else 0f
        heading.text = if (collapsed) "Today :" else "Timetable :"
        try {
            if (collapsed) {
                val viewPager = ViewPager2(context)
                val adapter = ViewPagerAdapter(activity())
                var todaySlots =
                    table.daySlots[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
                val now = Calendar.getInstance()
                    .apply {
                        set(Calendar.DATE, 1);
                        set(Calendar.MONTH, 0);
                        set(Calendar.YEAR, 1970)
                    }.timeInMillis
                if ((todaySlots.periods.last().startTime + (todaySlots.periods[0].duration * 2)) < now) {
                    todaySlots =
                        table.daySlots[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % 7]
                    heading.text = "Tomorrow :"
                }
                val timeSlot =
                    todaySlots.periods.indexOfFirst { abs(now - it.startTime) < it.duration }
                todaySlots.periods.forEachIndexed { index, periodSlot ->
                    adapter.addFragment(
                        SlotFragment.newInstance(
                            periodSlot.subject,
                            periodSlot.host,
                            if (timeSlot >= 0 && (abs(timeSlot - index) <= 1)) {
                                relativeTime(periodSlot.startTime, now, periodSlot.duration)
                            } else {
                                val c = Calendar.getInstance()
                                c.timeInMillis = periodSlot.startTime
                                (if (c.get(Calendar.HOUR) == 0) "0" else "") +
                                        c.get(Calendar.HOUR).toString() + ":" +
                                        (if (c.get(Calendar.MINUTE) < 10) "0" else "") +
                                        c.get(Calendar.MINUTE).toString() + " " +
                                        (if (c.get(Calendar.AM_PM) == 0) "AM" else "PM")
                            }
                        )
                    )
                }
                container.addView(viewPager)
                viewPager.isUserInputEnabled = true
                viewPager.adapter = adapter
                if (timeSlot > 0) Timer().schedule(timerTask {
                    activity().runOnUiThread { viewPager.setCurrentItem(timeSlot, true) }
                }, 500)
            } else {
                val viewPagers = List(7) { i -> ViewPager2(context) }
                var lastSearch = ""
                val onLongClick: (String) -> Unit = { subToFind ->
                    if (lastSearch == subToFind) {
                        viewPagers.forEach { it.animate().alpha(1f) }
                        lastSearch = ""
                    } else {
                        lastSearch = subToFind
                        table.daySlots.forEachIndexed { index, daySlots ->
                            val findex = daySlots.periods.indexOfFirst { it.subject == subToFind }
                            if (findex >= 0) {
                                viewPagers[index].animate().alpha(1f)
                                viewPagers[index].setCurrentItem(findex, true)
                            } else {
                                viewPagers[index].animate().alpha(0.25f)
                            }
                        }
                    }

                }
                viewPagers.forEachIndexed { index, viewPager ->
                    val adapter = ViewPagerAdapter(activity())
                    val todaySlots = table.daySlots[index]
                    todaySlots.periods.forEachIndexed { index, periodSlot ->
                        val c = Calendar.getInstance()
                        c.timeInMillis = periodSlot.startTime
                        adapter.addFragment(
                            SlotFragment.newInstance(
                                periodSlot.subject,
                                periodSlot.host,
                                (if (c.get(Calendar.HOUR) == 0) "0" else "") +
                                        c.get(Calendar.HOUR).toString() + ":" +
                                        (if (c.get(Calendar.MINUTE) < 10) "0" else "") +
                                        c.get(Calendar.MINUTE).toString() + " " +
                                        (if (c.get(Calendar.AM_PM) == 0) "AM" else "PM")
                            ).apply {
                                longclick = onLongClick
                            }
                        )
                    }
                    val textView = TextView(context)
                    textView.text = DayOfWeek.values()[index].name.beautifyCase()
                    textView.setPadding(0, 5, 0, 0)
                    textView.gravity = Gravity.CENTER
                    container.addView(viewPager)
                    if (index + 1 != viewPagers.size) container.addView(textView)
                    viewPager.isUserInputEnabled = true
                    viewPager.adapter = adapter
                }
            }
            container.setPadding(10, 10, 10, 10)
        } catch (_: Exception) {
        }
        refershBtn.setOnClickListener {
            refershBtn.animate().rotation(360f).setDuration(1000).start()
            scrapers.retrieveTimetable(acc) {
                activity().runOnUiThread {
                    if (it != null) {
                        storage.saveTimeTable(acc.ID, it)
                        initialiseTimeTable(acc, collapsed)
                    } else {
                        Toast.makeText(context, "Failed to reload.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        collapseBtn.setOnClickListener {
            initialiseTimeTable(acc, !collapsed)
        }
    }

    private fun initialiseBirthdays(acc: Account, collapsed: Boolean = true) {
        val openBtn = requireView().findViewById<ImageView>(R.id.birthdayLoad)
        val listView = requireView().findViewById<ListView>(R.id.birthdayList)
        val loaderView = requireView().findViewById<TextView>(R.id.loadingBirthday)
        loaderView.visibility = View.GONE
        openBtn.rotation = if (collapsed) 90f else 0f
        openBtn.setOnClickListener {
            if (collapsed) {
                loaderView.visibility = View.VISIBLE
                scrapers.getBirthdays(acc, Calendar.getInstance()) {
                    activity().runOnUiThread {
                        if (!it.isNullOrEmpty()) {
                            val adapter = BirthdayListAdapter(context, it)
                            listView.adapter = adapter
                            updateItems(listView, adapter)
                            initialiseBirthdays(acc, !collapsed)
                        } else {
                            Toast.makeText(context, "Failed to load.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                listView.adapter = null
                updateItems(listView, BirthdayListAdapter(context, listOf()))
                initialiseBirthdays(acc, !collapsed)
            }
        }
    }

    private fun initialiseNotice(acc: Account, collapsed: Boolean = true) {
        val openBtn = requireView().findViewById<ImageView>(R.id.noticeLoad)
        val table = requireView().findViewById<TableLayout>(R.id.noticeTable)
        val loaderView = requireView().findViewById<TextView>(R.id.loadingNotice)
        loaderView.visibility = View.GONE
        openBtn.rotation = if (collapsed) 90f else 0f
        openBtn.setOnClickListener {
            if (collapsed) {
                loaderView.visibility = View.VISIBLE
                scrapers.getNews(acc) {
                    activity().runOnUiThread {
                        if (!it.isNullOrEmpty()) {
                            val tableData = it.entries.sortedByDescending { it.key }
                                .groupBy { SimpleDateFormat("DD/MM/YYYY").format(it.key) }
                            tableData.forEach { map->
                                val row = TableRow(context)
                                val date = TextView(context)
                                date.text = map.key
                                date.setPadding(25, 20, 25, 20)
                                date.textSize = 16f
                                date.setTextColor(resources.getColor(R.color.green,null))
                                row.addView(date)
                                map.value.forEachIndexed { index, mutableEntry ->
                                    val news = MaterialButton(context)
                                    news.strokeColor = ColorStateList.valueOf(resources.getColor(R.color.mid,null))
                                    news.strokeWidth = 2
                                    news.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.transparent,null))
                                    news.text = "Notice ${index + 1}"
                                    news.textSize = 16f
                                    news.setTextColor(resources.getColor(R.color.white,null))
                                    news.setPadding(15, 0, 15, 0)
                                    news.layoutParams = TableRow.LayoutParams(
                                        TableRow.LayoutParams.WRAP_CONTENT,
                                        100
                                    ).apply {
                                        marginStart = 10
                                        marginEnd = 20
                                    }
                                    news.setOnClickListener {
                                        startActivity(
                                            PdfViewerActivity.launchPdfFromUrl(
                                                context = context,
                                                pdfUrl = mutableEntry.value,
                                                pdfTitle = "Notice ${index + 1} - ${map.key}",
                                                saveTo = saveTo.ASK_EVERYTIME,
                                                enableDownload = true
                                            )
                                        )
                                    }
                                    row.addView(news)
                                }
                                table.addView(row)
                            }
                            initialiseNotice(acc, !collapsed)
                        } else {
                            Toast.makeText(context, "Failed to load.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                table.removeAllViews()
                initialiseBirthdays(acc, !collapsed)
            }
        }
    }

    private fun initialiseAttendance(acc: Account) {
        val refreshBtn = requireView().findViewById<ImageView>(R.id.attendanceRefresh)
        val attendanceTable = requireView().findViewById<TableLayout>(R.id.attendanceTable)
        attendanceTable.removeAllViews()
        try {
            val headingRow = TableRow(context)
            headingRow.addView(TextView(context).apply {
                text = "Last updated ${
                    SimpleDateFormat("DD/MM HH:mm").format(
                        Calendar.getInstance()
                            .apply { timeInMillis = storage.getAttendance(acc.ID).lastUpdated }.time
                    )
                }"
                alpha = 0.5f
                setPadding(10, 10, 20, 10)
            })
            val discarded = arrayListOf<Int>()
            storage.getAttendance(acc.ID).subjects[0].attend.entries.sortedBy { it.key }.forEach {
                if (it.value == 0f) {
                    discarded.add(it.key)
                } else {
                    val heading = TextView(context)
                    heading.text =
                        if (it.key == 0) "Overall" else shortMonth((it.key % 12) + 1) + " " + (it.key / 12) % 100
                    heading.setPadding(10, 10, 20, 10)
                    heading.gravity = Gravity.END
                    heading.textSize = 16f
                    headingRow.addView(heading)
                }
            }
            headingRow.addView(TextView(context).apply { text = "\t\t" })
            attendanceTable.addView(headingRow)
            storage.getAttendance(acc.ID).subjects.forEach { sub ->
                val row = TableRow(context)
                val name = TextView(context)
                name.setPadding(10, 10, 20, 10)
                name.text = sub.name
                name.textSize = 15f
                row.addView(name)
                sub.attend.entries.filter { !discarded.contains(it.key) }.sortedBy { it.key }
                    .forEach {
                        val text = TextView(context)
                        text.text = "${it.value}%"
                        text.setPadding(10, 10, 20, 10)
                        text.gravity = Gravity.END
                        if (it.value > 0) {
                            text.setTextColor(generateColor(it.value))
                        }
                        row.addView(text)
                    }
                attendanceTable.addView(row)
            }
        } catch (_: Exception) {
        }
        refreshBtn.setOnClickListener {
            refreshBtn.animate().rotation(360f).setDuration(1000).start()
            scrapers.retrieveAttendance(acc) {
                activity().runOnUiThread {
                    if (it != null) {
                        storage.saveAttendance(acc.ID, it)
                        initialiseAttendance(acc)
                    } else {
                        Toast.makeText(context, "Failed to reload.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateItems(listView: ListView, listAdapter: BaseAdapter) {
        var totalHeight = 0
        val desiredWidth =
            View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.AT_MOST)
        listAdapter.notifyDataSetChanged()
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, listView)
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
            totalHeight += listItem.measuredHeight
        }
        val params = listView.layoutParams
        params.height = totalHeight + (15 * (listAdapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}