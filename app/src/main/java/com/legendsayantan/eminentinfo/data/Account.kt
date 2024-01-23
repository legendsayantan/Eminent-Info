package com.legendsayantan.eminentinfo.data

/**
 * @author legendsayantan
 */
data class Account(val name: String, val ID: String, val sessionKey: String, var accessor:Int=0,val csrfToken:String,var course:String = "", var batch:String = "")