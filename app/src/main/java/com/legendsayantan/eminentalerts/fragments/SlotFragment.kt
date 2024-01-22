package com.legendsayantan.eminentalerts.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.legendsayantan.eminentalerts.R

private const val ARG_SUB = "subject"
private const val ARG_HOST = "name"
private const val ARG_TIME = "time"

/**
 * A simple [Fragment] subclass.
 * Use the [SlotFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SlotFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var subject: String? = null
    private var host: String? = null
    private var time: String? = null
    public var longclick: (String)->Unit? = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            subject = it.getString(ARG_SUB)
            host = it.getString(ARG_HOST)
            time = it.getString(ARG_TIME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_timeslot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.host).text = host
        view.findViewById<TextView>(R.id.time).text = time
        subject?.split("(").let {
            if (it != null) {
                view.findViewById<TextView>(R.id.subject).text = it[0]
                view.findViewById<TextView>(R.id.code).text = if(it.size>1) it[1].replace(")","") else ""
            }
        }
        view.findViewById<LinearLayout>(R.id.slotContainer).setOnLongClickListener {
            subject?.let { it1 -> longclick(it1) }
            false
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param subject Parameter 1.
         * @param host Parameter 2.
         * @return A new instance of fragment PeriodFragment.
         */
        @JvmStatic
        fun newInstance(subject: String, host: String, time:String) =
            SlotFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUB, subject)
                    putString(ARG_HOST, host)
                    putString(ARG_TIME, time)
                }
            }
    }
}