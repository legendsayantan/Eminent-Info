package com.legendsayantan.eminentinfo.data

/**
 * @author legendsayantan
 */
data class TimeTable(var daySlots: Array<DaySlots>,var holidays:HashMap<Long,String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimeTable

        if (!daySlots.contentEquals(other.daySlots)) return false

        return true
    }

    override fun hashCode(): Int {
        return daySlots.contentHashCode()
    }

    companion object{
        fun optimiseTable(table: TimeTable):TimeTable{
            val newTable = TimeTable(Array(7) { _ -> DaySlots(arrayListOf()) }, hashMapOf())
            table.daySlots.forEachIndexed { index, daySlots ->
                newTable.daySlots[index] = DaySlots.optimise(daySlots)
            }
            return newTable
        }
    }

}
