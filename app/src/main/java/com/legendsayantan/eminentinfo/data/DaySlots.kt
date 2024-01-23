package com.legendsayantan.eminentinfo.data

/**
 * @author legendsayantan
 */
data class DaySlots(val periods:ArrayList<PeriodSlot>) {
    companion object{
        fun optimise(slots: DaySlots):DaySlots{
            val newArrayList = arrayListOf<PeriodSlot>()
            slots.periods.forEach { slot ->
                if(newArrayList.size==0) newArrayList.add(slot)
                else if (newArrayList.last().subject==slot.subject){
                    newArrayList.removeLast().let {
                        newArrayList.add(it.apply { duration+=slot.duration; })
                    }
                }else {
                    newArrayList.add(slot)
                }
            }
            return DaySlots(newArrayList)
        }
    }
}
