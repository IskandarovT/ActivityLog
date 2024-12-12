package com.melkii_mel.activitylog.data

import android.content.Context
import android.util.Log
import com.melkii_mel.activitylog.R
import com.melkii_mel.activitylog.calendarFromIso8601String
import com.melkii_mel.activitylog.readJsonFromFile
import com.melkii_mel.activitylog.writeStringToLocalFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.StringReader

@Serializable
class Userdata(val activityInfos: MutableList<ActivityInfo> = mutableListOf()) {

    companion object {
        private var _current: Userdata? = null
        const val FULL_FILE_NAME = "userdata.json"

        fun initUserdata(context: Context) {
            _current = deserialize(context)
        }

        val current: Userdata
            get() {
                if (_current == null) {
                    throw IllegalStateException("Userdata is not initialized yet. Call initUserdata(context) before accessing it.")
                }
                return _current!!
            }

        private fun deserialize(context: Context): Userdata {
            try {
                val userdata: Userdata = Json.decodeFromString(readJsonFromFile(context, FULL_FILE_NAME))
                writeStringToLocalFile(context, userdata.toJsonString(), "$FULL_FILE_NAME.bak")
                return userdata
            } catch (_: Exception) {
                try {
                    val userdata: Userdata = Json.decodeFromString(readJsonFromFile(context, "$FULL_FILE_NAME.bak"))
                    writeStringToLocalFile(context, userdata.toJsonString(), FULL_FILE_NAME)
                    return userdata
                } catch (_: Exception) {
                    return Userdata()
                }
            }
        }

        fun deserializeFromJsonString(
            json: String, onSuccess: (() -> Unit)? = null, onError: (() -> Unit)? = null
        ) {
            try {
                _current = Json.decodeFromString(json)
                onSuccess?.invoke()
            } catch (_: SerializationException) {
                onError?.invoke()
            } catch (_: IllegalArgumentException) {
                onError?.invoke()
            }
        }
    }

    fun toJsonString(): String {
        return Json.encodeToString(this)
    }

    fun toJsonFile(context: Context): File {
        return File(context.cacheDir, FULL_FILE_NAME).apply {
            writeText(toJsonString())
        }
    }

    fun serialize(context: Context) {
        val jsonString = toJsonString()
        writeStringToLocalFile(context, jsonString, FULL_FILE_NAME)
        writeStringToLocalFile(context, jsonString, "$FULL_FILE_NAME.bak")
    }

    fun importActivityInfoFromJsonString(
        json: String, onSuccess: (() -> Unit)? = null, onError: (() -> Unit)? = null
    ) {
        try {
            activityInfos.add(Json.decodeFromString(json))
            onSuccess?.invoke()
        } catch (_: SerializationException) {
            onError?.invoke()
        } catch (_: IllegalArgumentException) {
            onError?.invoke()
        }
    }

    fun addActivityInfoFromCsv(
        context: Context,
        csv: String,
        emptyActivityInfo: ActivityInfo,
        onSuccess: (() -> Unit)?,
        onError: ((String?) -> Unit)?
    ) {
        if (emptyActivityInfo.days.isNotEmpty()) {
            onError?.invoke("DEBUG_ERROR: Provided emptyActivityInfo is not empty!")
            return
        }

        val sr = StringReader(csv)
        val lines = sr.readLines()
        val header = lines.firstOrNull()
        if (header == null || header != ActivityInfo.CSV_HEADER) {
            onError?.invoke(context.getString(R.string.provided_csv_is_missing) + " ${ActivityInfo.CSV_HEADER}\" " + context.getString(
                R.string.header
            ) + "!")
            return
        }
        for ((i, line) in lines.drop(1).withIndex()) {
            val lineIndex = i + 1
            val columns = line.split(",")
            if (columns.size != 2) {
                onError?.invoke(context.getString(R.string.provided_csv_has_invalid_number_of_values_on_line) + "${lineIndex}!")
                return
            }
            val date = try {
                calendarFromIso8601String(columns[0].trim())
            } catch (e: Exception) {
                onError?.invoke(context.getString(R.string.invalid_date_format_on_line) + "$lineIndex")
                return
            }
            val quantity = columns[1].trim().toDoubleOrNull()
            if (quantity == null) {
                onError?.invoke(context.getString(R.string.invalid_quantity_format_on_line) + "$lineIndex")
                return
            }
            emptyActivityInfo.add(Entry(date, quantity))
        }
        if (!activityInfos.contains(emptyActivityInfo)) {
            activityInfos.add(emptyActivityInfo)
        }
        onSuccess?.invoke()
        return
    }
}
