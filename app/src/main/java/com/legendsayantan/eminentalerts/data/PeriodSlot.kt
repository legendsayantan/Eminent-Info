package com.legendsayantan.eminentalerts.data

/**
 * @author legendsayantan
 */
data class PeriodSlot(val startTime:Long,val subject:String,val host:String){
    companion object{
        const val duration = 50*60*1000L
    }
}
