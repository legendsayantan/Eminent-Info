package com.legendsayantan.eminentalerts.data

/**
 * @author legendsayantan
 */
data class Account(val name: String, val ID: String, val sessionKey: String, var accessor:Int=0,val csrfToken:String)