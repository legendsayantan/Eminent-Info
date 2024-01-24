package com.legendsayantan.eminentinfo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
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
        //if appstorage has no accounts, inflate login fragment
        //else inflate home fragment
        reloadUI()
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