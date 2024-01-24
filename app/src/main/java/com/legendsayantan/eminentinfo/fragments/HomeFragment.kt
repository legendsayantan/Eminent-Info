package com.legendsayantan.eminentinfo.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.legendsayantan.eminentinfo.MainActivity
import com.legendsayantan.eminentinfo.R
import com.legendsayantan.eminentinfo.adapters.BirthdayListAdapter
import com.legendsayantan.eminentinfo.adapters.ViewPagerAdapter
import com.legendsayantan.eminentinfo.data.Account
import com.legendsayantan.eminentinfo.receivers.BirthdayNotice
import com.legendsayantan.eminentinfo.utils.Misc.Companion.beautifyCase
import com.legendsayantan.eminentinfo.utils.Misc.Companion.generateColor
import com.legendsayantan.eminentinfo.utils.Misc.Companion.relativeTime
import com.legendsayantan.eminentinfo.utils.Misc.Companion.requestIgnoreBatteryOptimizations
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
        infoView.setOnClickListener { manageAccount(acc) }

        initialiseBirthdays(acc)
        initialiseNotice(acc)
        initialiseAttendance(acc)
        initialiseNotifications(acc)
    }

    override fun onResume() {
        super.onResume()
        initialiseTimeTable(storage.getActiveAccount())
    }

    private fun manageAccount(acc: Account) {
        val cardView = MaterialCardView(context)
        cardView.strokeWidth = 5
        cardView.strokeColor = resources.getColor(R.color.mid, null)
        cardView.radius = 75f
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(50, 50, 50, 50)
        }
        val title = TextView(context)
        val list = ListView(context)
        title.text = "Switch Account"
        title.textSize = 18f
        title.setPadding(15, 15, 15, 15)
        title.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        title.gravity = Gravity.CENTER
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_list_item_1,
            storage.getAllAccounts().filter { it != acc }.map { it.name })
        list.adapter = adapter
        val addNew = MaterialButton(context)
        addNew.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mid, null))
        addNew.text = "Add New"
        addNew.setTextColor(resources.getColor(R.color.white, null))
        addNew.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            marginEnd = 25
            topMargin = 25
        }
        val logOut = MaterialButton(context)
        logOut.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.grey, null))
        logOut.text = "Log Out"
        logOut.setTextColor(resources.getColor(R.color.white, null))
        logOut.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            marginStart = 25
            topMargin = 25
        }
        val btnContainer = LinearLayout(context)
        btnContainer.orientation = LinearLayout.HORIZONTAL
        btnContainer.addView(addNew)
        btnContainer.addView(logOut)
        container.addView(title)
        container.addView(list)
        container.addView(btnContainer)
        cardView.addView(container)
        val dialog = MaterialAlertDialogBuilder(context).setView(cardView).create()
        dialog.show()
        list.setOnItemClickListener { _, _, position, _ ->
            storage.setActiveAccount(storage.getAllAccounts()[position])
            activity().reloadUI()
            dialog.dismiss()
        }
        addNew.setOnClickListener {
            activity().addNewAccount()
            dialog.dismiss()
        }
        logOut.setOnClickListener {
            scrapers.logOut(acc) {
                activity().runOnUiThread {
                    if (it) {
                        storage.deleteAccount(acc)
                        activity().reloadUI()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, "Failed to logout.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initialiseTimeTable(acc: Account, collapsed: Boolean = true) {
        val refreshBtn = requireView().findViewById<ImageView>(R.id.timetableRefresh)
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
                var dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val now = Calendar.getInstance()
                    .apply {
                        set(Calendar.DATE, 1);
                        set(Calendar.MONTH, 0);
                        set(Calendar.YEAR, 1970)
                    }.timeInMillis
                if ((todaySlots.periods.last().let { it.startTime + (it.duration * 2) }) < now) {
                    todaySlots =
                        table.daySlots[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) % 7]
                    dayOfYear = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR,1) }.get(Calendar.DAY_OF_YEAR)
                    heading.text = "Tomorrow :"
                }
                val hD = table.holidays.keys.find { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_YEAR) ==dayOfYear }
                if(hD!=null) {
                    val textView = TextView(context)
                    textView.text = "Holiday : ${table.holidays[hD]}"
                    textView.setPadding(0, 0, 0, 10)
                    textView.gravity = Gravity.CENTER
                    container.addView(textView)
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
        refreshBtn.setOnClickListener {
            refreshBtn.animate().rotation(360f).setDuration(1000).start()
            scrapers.retrieveTimetable(acc) {
                activity().runOnUiThread {
                    if (it != null) {
                        storage.saveTimeTable(acc.ID, it)
                        initialiseTimeTable(acc, collapsed)
                        scrapers.getMoreInfo(acc){
                            it?.split("\n,").let { part->
                                acc.course = part?.get(0)?.trim() ?: ""
                                acc.batch = part?.get(1)?.trim() ?: ""
                            }
                            activity().appStorage.saveAccount(acc)
                        }
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
                if(loaderView.visibility==View.VISIBLE)return@setOnClickListener
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
                if(loaderView.visibility==View.VISIBLE)return@setOnClickListener
                loaderView.visibility = View.VISIBLE
                scrapers.getNews(acc) {
                    activity().runOnUiThread {
                        if (!it.isNullOrEmpty()) {
                            val tableData = it.entries.sortedByDescending { it.key }
                                .groupBy { SimpleDateFormat("EEE, DD MMM").format(it.key) }
                            tableData.forEach { map ->
                                val row = TableRow(context)
                                val date = TextView(context)
                                date.text = map.key
                                date.setPadding(25, 20, 25, 20)
                                date.textSize = 16f
                                date.setTextColor(resources.getColor(R.color.green, null))
                                row.addView(date)
                                map.value.forEachIndexed { index, mutableEntry ->
                                    val news = MaterialButton(context)
                                    news.strokeColor = ColorStateList.valueOf(
                                        resources.getColor(
                                            R.color.mid,
                                            null
                                        )
                                    )
                                    news.strokeWidth = 2
                                    news.backgroundTintList = ColorStateList.valueOf(
                                        resources.getColor(
                                            R.color.transparent,
                                            null
                                        )
                                    )
                                    news.text = "Notice ${index + 1}"
                                    news.textSize = 16f
                                    news.setTextColor(resources.getColor(R.color.white, null))
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
                initialiseNotice(acc, !collapsed)
            }
        }
    }

    private fun initialiseAttendance(acc: Account) {
        val refreshBtn = requireView().findViewById<ImageView>(R.id.attendanceRefresh)
        val attendanceTable = requireView().findViewById<TableLayout>(R.id.attendanceTable)
        val loaderView = requireView().findViewById<TextView>(R.id.loadingAttendance)
        loaderView.visibility = View.GONE
        attendanceTable.removeAllViews()
        try {
            val headingRow = TableRow(context)
            headingRow.addView(TextView(context).apply {
                text = "Last updated ${
                    SimpleDateFormat("DD MMM HH:mm").format(
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
                        if (it.key == 0) "Overall" else shortMonth((it.key % 12) + 1)
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
            if(loaderView.visibility==View.VISIBLE)return@setOnClickListener
            refreshBtn.animate().rotation(360f).setDuration(1000).start()
            loaderView.visibility = View.VISIBLE
            scrapers.retrieveAttendance(acc) {
                activity().runOnUiThread {
                    if (it != null) {
                        storage.saveAttendance(acc.ID, it)
                        initialiseAttendance(acc)
                    } else {
                        Toast.makeText(context, "Failed to reload.", Toast.LENGTH_SHORT).show()
                    }
                    loaderView.visibility = View.GONE
                }
            }
        }
        initialiseAbsence(acc)
    }

    private fun initialiseAbsence(acc: Account) {
        val table = requireView().findViewById<TableLayout>(R.id.absenceTable)
        table.removeAllViews()
        val days =
            storage.getAttendance(acc.ID).absence.entries.sortedByDescending { it.key }
                .groupBy { SimpleDateFormat("EEE, DD MMM").format(it.key) }
        days.forEach { day ->
            val row = TableRow(context)
            val date = TextView(context)
            date.text = day.key
            date.setPadding(25, 20, 25, 20)
            date.textSize = 16f
            date.setTextColor(resources.getColor(R.color.green, null))
            row.addView(date)
            day.value.forEachIndexed { index, mutableEntry ->
                val news = TextView(context)
                news.text =
                    mutableEntry.value.split(" ").joinToString(""){ it.substring(0, 1) }
                news.textSize = 16f
                news.setTextColor(resources.getColor(R.color.white, null))
                news.setPadding(15, 0, 15, 0)
                news.layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    100
                ).apply {
                    marginStart = 10
                    marginEnd = 20
                }
                row.addView(news)
            }
            table.addView(row)
        }
    }

    private fun initialiseNotifications(acc: Account) {
        val image = requireView().findViewById<ImageView>(R.id.notiSettings)
        var notifications = storage.getNotificationSettings(acc.ID)
        if (notifications.any { it }) {
            image.setImageResource(R.drawable.baseline_notifications_24)
            activity().requestIgnoreBatteryOptimizations()
            registerAlarmManager(notifications)
        } else {
            image.setImageResource(R.drawable.baseline_notifications_none_24)
        }
        image.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        activity(),
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        0
                    )
                    return@setOnClickListener
                }
            }
            val card = MaterialCardView(context)
            card.strokeWidth = 5
            card.strokeColor = resources.getColor(R.color.mid, null)
            card.radius = 75f
            val container = LinearLayout(context)
            container.orientation = LinearLayout.VERTICAL
            val title = TextView(context)
            val timeTableSwitch = MaterialSwitch(context)
            val birthdaySwitch = MaterialSwitch(context)
            val noticeSwitch = MaterialSwitch(context)
            title.text = "Notification Settings"
            title.textSize = 18f
            title.setPadding(0, 0, 0, 15)
            timeTableSwitch.text = "Next Periods"
            birthdaySwitch.text = "Birthdays of classmates"
            noticeSwitch.text = "New Notices"
            try {
                timeTableSwitch.isChecked = notifications[0]
                birthdaySwitch.isChecked = notifications[1]
                noticeSwitch.isChecked = notifications[2]
            } catch (_: Exception) {
            }
            val saveSettings = CompoundButton.OnCheckedChangeListener { _, _ ->
                storage.saveNotificationSettings(
                    acc.ID,
                    arrayOf(
                        timeTableSwitch.isChecked,
                        birthdaySwitch.isChecked,
                        noticeSwitch.isChecked
                    )
                )
            }
            timeTableSwitch.setOnCheckedChangeListener(saveSettings)
            birthdaySwitch.setOnCheckedChangeListener(saveSettings)
            noticeSwitch.setOnCheckedChangeListener(saveSettings)
            container.addView(title)
            container.addView(timeTableSwitch)
            container.addView(birthdaySwitch)
            container.addView(noticeSwitch)

            container.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(50, 50, 50, 50)
            }
            card.addView(container)
            val dialog = MaterialAlertDialogBuilder(context)
            dialog.setView(card)
            dialog.setOnCancelListener {
                initialiseNotifications(acc)
            }
            dialog.show()
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
        params.height = totalHeight + (listView.dividerHeight * (listAdapter.count - 1))
        listView.layoutParams = params
        listView.requestLayout()
    }

    private fun registerAlarmManager(notifications: Array<Boolean>) {
        val alarmManager = activity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (notifications[0]) {

        }
        if (notifications[1] || notifications[2]) {
            val intent = Intent(activity(), BirthdayNotice::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(activity(), 0, intent, PendingIntent.FLAG_MUTABLE)
            try {
                alarmManager.cancel(pendingIntent)
            } catch (_: Exception) {
            }
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis,
                1000 * 60 * 60 * 24,
                pendingIntent
            )
        }
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