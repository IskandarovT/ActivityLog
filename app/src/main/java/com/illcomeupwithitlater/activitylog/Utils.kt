package com.melkii_mel.activitylog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.Normalizer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class Utils {
    companion object {

        fun getDatePart(calendar: Calendar): Calendar {
            val newCalendar = calendar.clone() as Calendar
            newCalendar.set(Calendar.HOUR_OF_DAY, 0)
            newCalendar.set(Calendar.MINUTE, 0)
            newCalendar.set(Calendar.SECOND, 0)
            newCalendar.set(Calendar.MILLISECOND, 0)
            return newCalendar
        }

        fun compareDateParts(date1: Calendar, date2: Calendar): Int {
            return getDatePart(date1).compareTo(getDatePart(date2))
        }

        fun dpToPx(context: Context, dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        fun showDateTimePickerDialog(
            context: Context,
            initCalendar: Calendar? = null,
            dateSelectedListener: (Calendar) -> Unit
        ) {
            val calendar = initCalendar ?: Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val timePickerDialog = TimePickerDialog(
                        context, { _, hourOfDay, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            dateSelectedListener(calendar)
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
                    )
                    timePickerDialog.show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        enum class FormatCalendarMode {
            DATE, TIME, DATE_TIME
        }

        fun formatCalendar(
            calendar: Calendar,
            formatCalendarMode: FormatCalendarMode = FormatCalendarMode.DATE_TIME
        ): String {
            val format = when (formatCalendarMode) {
                FormatCalendarMode.DATE_TIME -> {
                    SimpleDateFormat.getDateTimeInstance(
                        SimpleDateFormat.LONG, SimpleDateFormat.SHORT, Locale.getDefault()
                    )
                }

                FormatCalendarMode.DATE -> {
                    SimpleDateFormat.getDateInstance(
                        SimpleDateFormat.LONG, Locale.getDefault()
                    )
                }

                FormatCalendarMode.TIME -> {
                    SimpleDateFormat.getTimeInstance(
                        SimpleDateFormat.SHORT, Locale.getDefault()
                    )
                }
            }

            return format.format(calendar.time)
        }
    }
}

fun EditText.setOnTextChangedListener(listener: () -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?, start: Int, count: Int, after: Int
        ) {

        }

        override fun onTextChanged(
            s: CharSequence?, start: Int, before: Int, count: Int
        ) {
            listener()
        }

        override fun afterTextChanged(s: Editable?) {

        }

    })
}

/**
 * Set as -1 to apply default formatting with locale settings
 * Set as 0 for integer conversion
 * Set as any other positive number for custom decimal places formatting
 */
fun Double.toFormattedString(digits: Int = -1): String {
    return when (digits) {
        -1 -> NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
        0 -> this.toInt().toString()
        else -> {
            val decimalFormat = DecimalFormat.getInstance(Locale.getDefault()) as DecimalFormat
            decimalFormat.maximumFractionDigits = digits
            decimalFormat.format(this)
        }
    }
}

fun closeKeyboard(activity: Activity) {
    val inputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = activity.currentFocus
    view?.let {
        inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

private val sliding: MutableMap<View, ObjectAnimator> = mutableMapOf()

fun slideIn(
    slidingLayout: View,
    durationMs: Long,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null
) {
    slidingLayout.measure(0, 0)
    slidingLayout.translationY = -slidingLayout.measuredHeight.toFloat()
    val animator = ObjectAnimator.ofFloat(
        slidingLayout, "translationY", -slidingLayout.measuredHeight.toFloat(), 0f
    )
    animator.duration = durationMs
    animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            slidingLayout.visibility = View.VISIBLE
            onAnimationEnd?.invoke()
        }

        override fun onAnimationCancel(animation: Animator) {
            super.onAnimationCancel(animation)
            onAnimationEnd?.invoke()
        }
    })
    sliding[slidingLayout]?.cancel()
    sliding[slidingLayout] = animator
    onAnimationStart?.invoke()
    animator.start()
}


fun slideOut(
    slidingLayout: View,
    durationMs: Long,
    onAnimationStart: (() -> Unit)? = null,
    onAnimationEnd: (() -> Unit)? = null
) {
    slidingLayout.post {
        val animator = ObjectAnimator.ofFloat(
            slidingLayout, "translationY", 0f, -slidingLayout.height.toFloat()
        )
        animator.duration = durationMs
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                onAnimationEnd?.invoke()
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                onAnimationEnd?.invoke()
            }
        })
        sliding[slidingLayout]?.cancel()
        sliding[slidingLayout] = animator
        animator.start()
        onAnimationStart?.invoke()
    }
}

fun writeStringToLocalFile(context: Context, json: String, fileName: String) {
    try {
        val fileOutputStream: FileOutputStream =
            context.openFileOutput(fileName, Context.MODE_PRIVATE)
        val outputStreamWriter = OutputStreamWriter(fileOutputStream)
        outputStreamWriter.write(json)
        outputStreamWriter.close()

        File(context.filesDir, fileName).copyTo(File(context.filesDir, "$fileName.bak"), overwrite = true)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun readJsonFromFile(context: Context, fileName: String): String {
    return try {
        val fileInputStream: FileInputStream = context.openFileInput(fileName)
        val inputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader = inputStreamReader.buffered()
        val stringBuilder = StringBuilder()

        bufferedReader.forEachLine { stringBuilder.append(it) }

        inputStreamReader.close()
        stringBuilder.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

fun getSafeString(str: String): String {
    val normalizedString = Normalizer.normalize(str, Normalizer.Form.NFD)
    val safeString = normalizedString.replace(Regex("[^a-zA-Z0-9]"), "_")
    return safeString
}

@Suppress("SpellCheckingInspection")
private val iso8601DateFormatter by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
}

fun Calendar.toIso8601String(): String {
    return iso8601DateFormatter.format(this.time)
}

fun calendarFromIso8601String(str: String): Calendar {
    return Calendar.getInstance().apply {
        time = iso8601DateFormatter.parse(str) ?: throw IllegalArgumentException("Invalid date format!")
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) {
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
    }
    return null
}


private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
private lateinit var filePickerLauncherActionPointer: (String, String, String) -> Unit
private lateinit var fileSaverLauncher: ActivityResultLauncher<Intent>
private lateinit var fileSaverContent: String

fun ComponentActivity.initFilePickerLauncher() {
    filePickerLauncher = this.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri = data?.data
            if (uri != null) {
                val contentResolver = this.contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().use { it.readText() }
                    val fileName = getFileName(this, uri)?.split(".") ?: mutableListOf("", "")
                    filePickerLauncherActionPointer(fileName[0], fileName[1], content)
                }
            }
        }
    }
}

/**
 * String 0 is Name
 * String 1 is Extension
 * String 2 is Content
 */
fun pickFile(fileType: String, action: (String, String, String) -> Unit) {
    filePickerLauncherActionPointer = action
    filePickerLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        this.type = fileType
    })
}

fun ComponentActivity.initFileSaverLauncher() {
    fileSaverLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(fileSaverContent.toByteArray())
                }
            }
        }
    }
}

fun saveFileWithUserSelection(fileName: String, mimeType: String, content: String) {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
        putExtra(Intent.EXTRA_TITLE, fileName)
    }
    fileSaverContent = content
    fileSaverLauncher.launch(intent)
}
