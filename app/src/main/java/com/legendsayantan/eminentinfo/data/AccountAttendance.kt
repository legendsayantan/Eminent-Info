package com.legendsayantan.eminentinfo.data

/**
 * @author legendsayantan
 */
data class AccountAttendance(
    val subjects: ArrayList<SubjectAttendance>,
    var absence: HashMap<Long, String> = hashMapOf(),
    val lastUpdated: Long
){
}