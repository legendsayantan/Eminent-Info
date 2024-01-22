package com.legendsayantan.eminentalerts.data

/**
 * @author legendsayantan
 */
data class TimeTable(var daySlots: Array<DaySlots>) {
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

}
