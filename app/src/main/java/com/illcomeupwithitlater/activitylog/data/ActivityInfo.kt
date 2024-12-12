package com.melkii_mel.activitylog.data

import android.content.Context
import com.melkii_mel.activitylog.Utils
import com.melkii_mel.activitylog.Utils.Companion.compareDateParts
import com.melkii_mel.activitylog.getSafeString
import com.melkii_mel.activitylog.toIso8601String
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.util.Calendar

@Serializable
class ActivityInfo(
    var name: String, var realNumbers: Boolean, var bestIsMin: Boolean
) {
    private var _days: MutableList<ActivityInfoDay> = mutableListOf()

    @Transient
    val days: List<ActivityInfoDay> = _days

    fun removeDayAt(position: Int) {
        _days.removeAt(position)
    }

    fun add(entry: Entry) {
        var day = _days.find { d ->
            compareDateParts(d.date, entry.date) == 0
        }
        if (day != null) {
            day.entries.add(entry)
            day.entries.sortWith { e1, e2 ->
                e1.date.compareTo(e2.date)
            }
        } else {
            day = ActivityInfoDay(mutableListOf(entry), Utils.getDatePart(entry.date))
            _days.add(day)
            _days.sortWith { e1, e2 ->
                e1.date.compareTo(e2.date)
            }
            _dayAddedListener?.onDayAdded(day, days.indexOf(day))
        }
        _entryAddedListener?.onEntryAdded(day, entry, day.entries.indexOf(entry))
    }

    fun getBestFrom(from: Calendar, to: Calendar): Entry? {
        return getBest(days.filter { d -> d.date > from && d.date < to }.flatMap { d -> d.entries },
            bestIsMin
        )
    }

    fun getBestInDay(day: ActivityInfoDay): Entry? {
        return getBest(day.entries, bestIsMin)
    }

    fun getBest(): Entry? {
        return getBest(days.flatMap { d -> d.entries }, bestIsMin)
    }

    fun getTotalFrom(from: Calendar, to: Calendar): Double {
        return getTotal(days.filter { d -> d.date > from && d.date < to }
            .flatMap { d -> d.entries })
    }

    fun getTotalInDay(day: ActivityInfoDay): Double {
        return getTotal(day.entries)
    }

    fun getTotal(): Double {
        return getTotal(days.flatMap { d -> d.entries })
    }

    @Transient
    private var _dayAddedListener: DayAddedListener? = null

    fun setDayAddedListener(listener: DayAddedListener) {
        _dayAddedListener = listener
    }

    @Transient
    private var _entryAddedListener: EntryAddedListener? = null

    fun setEntryAddedListener(listener: EntryAddedListener) {
        _entryAddedListener = listener
    }

    companion object {
        fun getBest(entries: List<Entry>, bestIsMin: Boolean): Entry? {
            if (bestIsMin) return entries.minByOrNull { e -> e.value }
            return entries.maxByOrNull { e -> e.value }
        }

        fun getTotal(entries: List<Entry>): Double {
            return entries.sumOf { e -> e.value }
        }

        const val CSV_HEADER = "date,quantity"
    }

    fun interface DayAddedListener {
        fun onDayAdded(day: ActivityInfoDay, index: Int)
    }

    fun interface EntryAddedListener {
        fun onEntryAdded(day: ActivityInfoDay, entry: Entry, positionInDay: Int)
    }

    fun toCsvFile(context: Context): File {
        val sb = StringBuilder()
        sb.appendLine("date,quantity")
        for (day in days) {
            for (entry in day.entries) {
                sb.appendLine("${entry.date.toIso8601String()},${entry.value}")
            }
        }
        return File(context.cacheDir, "${getSafeString(name)}.csv").apply {
            writeText(sb.toString())
        }
    }

    data class FileInfo(val fileName: String, val mimeType: String, val content: String)
    fun toCsvFileInfo(): FileInfo {
        val sb = StringBuilder()
        sb.appendLine(CSV_HEADER)
        for (day in days) {
            for (entry in day.entries) {
                sb.appendLine("${entry.date.toIso8601String()},${entry.value}")
            }
        }
        return FileInfo("${getSafeString(name)}.csv", "text/csv", sb.toString())
    }
}
