package com.legendsayantan.eminentalerts.utils

import android.content.Context
import com.google.gson.Gson
import com.legendsayantan.eminentalerts.data.Account
import com.legendsayantan.eminentalerts.data.TimeTable

/**
 * @author legendsayantan
 */
class AppStorage(context: Context) {
    val accounts = context.getSharedPreferences("accounts",Context.MODE_PRIVATE)

    fun addNewAccount(account: Account){
        accounts.edit().putString(account.ID, Gson().toJson(account)).apply()
    }

    fun getAllAccounts():List<Account>{
        val accounts = mutableListOf<Account>()
        this.accounts.all.forEach {
            accounts.add(Gson().fromJson(it.value.toString(),Account::class.java))
        }
        return accounts
    }

    fun deleteAccount(account: Account){
        accounts.edit().remove(account.ID).apply()
    }

    fun getActiveAccount():Account{
        accounts.getString("active",null)?.let {
            return getAllAccounts().first { account -> account.ID == it }
        }
        return getAllAccounts().first()
    }

    fun setActiveAccount(account: Account){
        accounts.edit().putString("active",account.ID).apply()
    }

    val timetables = context.getSharedPreferences("timetables",Context.MODE_PRIVATE)
    fun addTimeTable(ID:String,table: TimeTable){
        timetables.edit().putString(ID, Gson().toJson(table)).apply()
    }
    fun getTimeTable(ID:String):TimeTable{
        return Gson().fromJson(timetables.getString(ID,"{}"),TimeTable::class.java)
    }

}