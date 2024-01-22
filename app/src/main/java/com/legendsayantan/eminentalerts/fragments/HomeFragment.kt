package com.legendsayantan.eminentalerts.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.legendsayantan.eminentalerts.MainActivity
import com.legendsayantan.eminentalerts.R
import com.legendsayantan.eminentalerts.adapters.ViewPagerAdapter
import com.legendsayantan.eminentalerts.data.Account
import com.legendsayantan.eminentalerts.data.PeriodSlot
import com.legendsayantan.eminentalerts.utils.Misc.Companion.beautifyCase
import com.legendsayantan.eminentalerts.utils.Misc.Companion.relativeTime
import com.legendsayantan.eminentalerts.utils.Scrapers
import java.time.DayOfWeek
import java.util.Calendar
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

        initialiseTimeTable(acc)


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
        heading.text = if(collapsed)"Today :" else "Timetable :"
        try {
            if (collapsed) {
                val viewPager = ViewPager2(context)
                val adapter = ViewPagerAdapter(activity())
                var todaySlots =
                    table.daySlots[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]
                val now = System.currentTimeMillis() % 86400000
                if((todaySlots.periods.last().startTime+(PeriodSlot.duration*2))<now){
                    todaySlots = table.daySlots[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)%7]
                    heading.text = "Tomorrow :"
                }
                val timeSlot =
                    todaySlots.periods.indexOfFirst { (now - it.startTime) < PeriodSlot.duration }
                todaySlots.periods.forEachIndexed { index, periodSlot ->
                    adapter.addFragment(
                        SlotFragment.newInstance(
                            periodSlot.subject,
                            periodSlot.host,
                            if (timeSlot >= 0 && (abs(timeSlot - index) <= 1)) {
                                relativeTime(periodSlot.startTime, now)
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
                viewPager.currentItem = timeSlot
            } else {
                val viewPagers = List(7) { i -> ViewPager2(context) }
                var lastSearch = ""
                val onLongClick :(String)->Unit= { subToFind->
                    if(lastSearch==subToFind){
                        viewPagers.forEach { it.animate().alpha(1f) }
                        lastSearch = ""
                    }else{
                        lastSearch = subToFind
                        table.daySlots.forEachIndexed { index, daySlots ->
                            val findex = daySlots.periods.indexOfFirst { it.subject==subToFind }
                            if(findex>=0) {
                                viewPagers[index].animate().alpha(1f)
                                viewPagers[index].setCurrentItem(findex, true)
                            }else{
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
                    textView.setPadding(0,5,0,0)
                    textView.gravity = Gravity.CENTER
                    container.addView(viewPager)
                    if(index+1!=viewPagers.size)container.addView(textView)
                    viewPager.isUserInputEnabled = true
                    viewPager.adapter = adapter
                }
            }
            container.setPadding(10, 10, 10, 10)
        } catch (_: Exception) { }
        refershBtn.setOnClickListener {
            refershBtn.animate().rotation(360f).setDuration(1000).start()
            scrapers.retrieveTimetable(acc) {
                activity().runOnUiThread {
                    if (it != null) {
                        storage.addTimeTable(acc.ID, it)
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