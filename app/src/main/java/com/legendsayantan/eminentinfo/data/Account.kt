package com.legendsayantan.eminentinfo.data

import com.google.gson.Gson

/**
 * @author legendsayantan
 */
data class Account(val name: String, val ID: String, val sessionKey: String, var accessor:Int=0,val csrfToken:String,var course:String = "", var batch:String = ""){
    fun toJson():String{
        return Gson().toJson(this)
    }
}