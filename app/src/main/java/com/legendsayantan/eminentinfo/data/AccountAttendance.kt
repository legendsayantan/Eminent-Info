package com.legendsayantan.eminentinfo.data

/**
 * @author legendsayantan
 */
data class AccountAttendance(
    val subjects: ArrayList<SubjectAttendance>,
    val absence: HashMap<Long, String>,
    val lastUpdated: Long
){
}