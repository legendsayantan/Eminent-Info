package com.legendsayantan.eminentinfo.utils

import android.content.Context
import com.google.gson.Gson
import com.legendsayantan.eminentinfo.data.Account
import com.legendsayantan.eminentinfo.data.AccountAttendance
import com.legendsayantan.eminentinfo.data.TimeTable

/**
 * @author legendsayantan
 */
class AppStorage(context: Context) {
    val accounts = context.getSharedPreferences("accounts",Context.MODE_PRIVATE)

    fun saveAccount(account: Account){
        accounts.edit().putString(account.ID, Gson().toJson(account)).apply()
    }

    fun getAllAccounts():List<Account>{
        val accounts = mutableListOf<Account>()
        this.accounts.all.filter { it.key!="active" }.forEach {
            accounts.add(Gson().fromJson(it.value.toString(),Account::class.java))
        }
        return accounts
    }

    fun deleteAccount(account: Account){
        accounts.edit().remove(account.ID).apply()
    }

    fun getActiveAccount():Account{
        accounts.getString("active",null)?.let {
            getAllAccounts().firstOrNull { account -> account.ID == it }?.let {
                return it
            }
        }
        return getAllAccounts().first()
    }

    fun setActiveAccount(account: Account){
        accounts.edit().putString("active",account.ID).apply()
    }

    val timetables = context.getSharedPreferences("timetables",Context.MODE_PRIVATE)
    fun saveTimeTable(ID:String, table: TimeTable){
        timetables.edit().putString(ID, Gson().toJson(table)).apply()
    }
    fun getTimeTable(ID:String):TimeTable{
        return Gson().fromJson(timetables.getString(ID,"{}"),TimeTable::class.java)
    }

    val notifications = context.getSharedPreferences("notifications",Context.MODE_PRIVATE)

    fun getNotificationSettings(ID:String):Array<Boolean>{
        return Gson().fromJson(notifications.getString(ID,"[]"),Array<Boolean>::class.java)
    }
    fun saveNotificationSettings(ID:String, settings:Array<Boolean>){
        notifications.edit().putString(ID, Gson().toJson(settings)).apply()
    }

    val attendance  = context.getSharedPreferences("attendance",Context.MODE_PRIVATE)

    fun getAttendance(ID:String):AccountAttendance{
        return Gson().fromJson(attendance.getString(ID,"{}"),AccountAttendance::class.java)
    }
    fun saveAttendance(ID: String, att: AccountAttendance){
        attendance.edit().putString(ID, Gson().toJson(att)).apply()
    }

}