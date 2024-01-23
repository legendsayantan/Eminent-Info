package com.legendsayantan.eminentinfo.data

/**
 * @author legendsayantan
 */
data class PeriodSlot(val startTime:Long, val subject:String, val host:String, var duration: Long = defaultDuration){
    companion object{
        const val defaultDuration = 50*60*1000L
    }
}
