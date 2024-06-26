package com.legendsayantan.eminentinfo.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.legendsayantan.eminentinfo.MainActivity
import com.legendsayantan.eminentinfo.R
import com.legendsayantan.eminentinfo.utils.Misc.Companion.launchUrlInBrowser
import com.legendsayantan.eminentinfo.utils.Scrapers
import java.util.Timer
import kotlin.concurrent.timerTask

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {
    private val passwordField by lazy { view?.findViewById<TextInputEditText>(R.id.password) }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contextCache = requireContext()
        activityCache = requireActivity()
        val loginButton = view.findViewById<MaterialButton>(R.id.loginBtn)
        val githubLink = view.findViewById<MaterialTextView>(R.id.githubLink)
        val passwordToggle = view.findViewById<ImageView>(R.id.password_toggle)
        val backBtn = view.findViewById<MaterialCardView>(R.id.goBack)
        passwordField?.inputType = 129
        passwordToggle.setOnClickListener {
            if (passwordField?.inputType == 129) {
                passwordField?.inputType = 145
                passwordToggle.setImageResource(R.drawable.baseline_visibility_off_24)
            } else {
                passwordField?.inputType = 129
                passwordToggle.setImageResource(R.drawable.baseline_visibility_24)
            }
        }
        loginButton.setOnClickListener {
            loginButton.error = null
            attemptLogin { name, success ->
                if (success) {
                    requireView().findViewById<MaterialCardView>(R.id.materialCardView).children.forEach {
                        it.visibility = View.GONE
                    }
                    backBtn.visibility = View.GONE
                    requireView().findViewById<MaterialTextView>(R.id.heading).alpha = 0f
                    Timer().schedule(timerTask {
                        activity().runOnUiThread {
                            requireView().findViewById<MaterialTextView>(R.id.heading).let {
                                it.textSize = 30f
                                it.text = "Welcome\n$name"
                                it.animate().alpha(1f).setDuration(1000).start()
                                Timer().schedule(timerTask {
                                    activity().runOnUiThread { activity().reloadUI() }
                                }, 2000)
                            }
                        }
                    }, 1000)
                } else {
                    loginButton.error = "Invalid Credentials"
                }
            }
        }
        githubLink.setOnClickListener {
            context.launchUrlInBrowser("https://github.com/legendsayantan")
        }
        if(activity().appStorage.getAllAccounts().isNotEmpty()) backBtn.visibility = View.VISIBLE
        backBtn.setOnClickListener {
            activity().reloadUI()
        }
    }

    override fun getContext(): Context {
        return super.getContext() ?: contextCache
    }

    private lateinit var contextCache: Context

    private fun activity(): MainActivity {
        return (super.getActivity() ?: activityCache) as MainActivity
    }

    private lateinit var activityCache: FragmentActivity
    private fun attemptLogin(callback: (String, Boolean) -> Unit) {
        val ID = view?.findViewById<TextInputEditText>(R.id.fedenaID)?.text.toString().uppercase()
        val password = passwordField?.text.toString()
        if (ID.isEmpty()
            || password.isEmpty()
            || (!ID.contains("ECMT") && !ID.contains("ECPT"))
        ) {
            callback("", false)
            return
        }
        Scrapers(activity()).let { scraper ->
            scraper.retrieveSessionKey(ID, password) { account ->
                if (account != null) {
                    activity().appStorage.saveAccount(account)
                    callback(account.name, true)
                    scraper.getMoreInfo(account) {
                        it?.split("\n,").let { part ->
                            account.course = part?.get(0)?.trim() ?: ""
                            account.batch = part?.get(1)?.trim() ?: ""
                        }
                        activity().appStorage.saveAccount(account)
                    }
                } else {
                    callback("", false)
                }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoginFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}