package com.legendsayantan.eminentinfo

import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
        supportFragmentManager.beginTransaction().replace(R.id.container, Fragment()).commit()
        supportFragmentManager.beginTransaction().replace(
            R.id.container, if (appStorage.getAllAccounts().isEmpty()) {
                LoginFragment()
            } else {
                HomeFragment()
            }
        ).commit()
    }
    fun addNewAccount(){
        supportFragmentManager.beginTransaction().replace(
            R.id.container, LoginFragment()
        ).commit()
    }
}