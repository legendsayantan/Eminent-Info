package com.legendsayantan.eminentinfo

import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.legendsayantan.eminentinfo.fragments.HomeFragment
import com.legendsayantan.eminentinfo.fragments.LoginFragment
import com.legendsayantan.eminentinfo.utils.AppStorage

class MainActivity : AppCompatActivity() {
    val appStorage by lazy { AppStorage(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        reloadUI()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            //copy to clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("Error", e.cause.toString()+"\n"+e.stackTraceToString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this,"Error copied to clipboard",Toast.LENGTH_LONG).show()
            //restart app
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
    fun reloadUI(){
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        val fragmentToShow = if (appStorage.getAllAccounts().isEmpty()) {
            LoginFragment()
        } else {
            HomeFragment()
        }
        val toBeRefreshed = currentFragment!=null && (currentFragment is HomeFragment == fragmentToShow is HomeFragment)
        if(toBeRefreshed)
            supportFragmentManager.beginTransaction().setCustomAnimations(
                R.anim.slide_in_right, // Enter animation for the new fragment
                R.anim.slide_out_left, // Exit animation for the old fragment
                R.anim.slide_in_left, // Enter animation for the old fragment (on back press)
                R.anim.slide_out_right // Exit animation for the new fragment (on back press)
            ).remove(currentFragment!!).commit()
        Handler(mainLooper).postDelayed({
            supportFragmentManager.beginTransaction().setCustomAnimations(
                R.anim.slide_in_right, // Enter animation for the new fragment
                R.anim.slide_out_left, // Exit animation for the old fragment
                R.anim.slide_in_left, // Enter animation for the old fragment (on back press)
                R.anim.slide_out_right // Exit animation for the new fragment (on back press)
            ).replace(
                R.id.container, fragmentToShow
            ).commit()
        },if(toBeRefreshed) 350 else 0)
    }
    fun addNewAccount(){
        supportFragmentManager.beginTransaction().setCustomAnimations(
            R.anim.slide_in_left,
            R.anim.slide_out_right,
            R.anim.slide_in_right,
            R.anim.slide_out_left
        ).replace(
            R.id.container, LoginFragment()
        ).commit()
    }
}