package com.melkii_mel.activitylog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.melkii_mel.activitylog.Utils.Companion.dpToPx
import com.melkii_mel.activitylog.data.ActivityInfo

class NewActivityPopupHandler {
    companion object {
        @SuppressLint("InflateParams")
        fun show(
            appCompatActivity: AppCompatActivity,
            initialActivity: ActivityInfo? = null,
            muteActivityAddedSnackBar: Boolean = false,
            onCreated: (ActivityInfo) -> Unit
        ) {
            val dialogView = appCompatActivity.layoutInflater.inflate(
                R.layout.dialog_add_new_activity_info, null
            )

            val dialogTitleTextView: TextView = dialogView.findViewById(R.id.text_view)
            val titleEditText: EditText = dialogView.findViewById(R.id.title_text_edit)
            val bestDirectionToggle: ToggleButton =
                dialogView.findViewById(R.id.best_direction_toggle)
            val numberTypeToggle: ToggleButton = dialogView.findViewById(R.id.number_type_toggle)
            val cancelButton: Button = dialogView.findViewById(R.id.cancel_button)
            val confirmButton: Button = dialogView.findViewById(R.id.confirm_button)

            if (initialActivity != null) {
                dialogTitleTextView.text = appCompatActivity.getString(R.string.edit_activity)
                titleEditText.setText(initialActivity.name)
                bestDirectionToggle.isChecked = !initialActivity.bestIsMin
                numberTypeToggle.isChecked = !initialActivity.realNumbers
            } else {
                dialogTitleTextView.text = appCompatActivity.getString(R.string.add_new_activity)
            }

            val dialog = Dialog(appCompatActivity, R.style.CustomDialogTheme).apply {
                setContentView(dialogView)
                window?.apply {
                    setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val paddingPx = dpToPx(appCompatActivity, 10)
                    decorView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                }
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            confirmButton.setOnClickListener {
                val titleText = titleEditText.text.toString()

                if (titleText.isBlank()) {
                    val errorDialogView = appCompatActivity.layoutInflater.inflate(
                        R.layout.title_empty_error, null
                    )
                    val dialog1 =
                        Dialog(appCompatActivity, R.style.CustomDialogTheme).apply {
                            setContentView(errorDialogView)
                            window?.apply {
                                setLayout(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                                val paddingPx = dpToPx(appCompatActivity, 10)
                                decorView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                            }
                            show()
                        }
                    errorDialogView.findViewById<Button>(R.id.ok_button).setOnClickListener {
                        dialog1.dismiss()
                    }
                } else {
                    if (!muteActivityAddedSnackBar) {
                        Snackbar.make(
                            appCompatActivity.findViewById(R.id.root_layout),
                            appCompatActivity.getString(R.string.new_activity_added),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    val bestIsMin = !bestDirectionToggle.isChecked
                    val realNumbers = !numberTypeToggle.isChecked
                    val newActivityInfo = ActivityInfo(titleText, realNumbers, bestIsMin)
                    onCreated(newActivityInfo)
                    dialog.dismiss()
                }
            }

            dialog.show()

            titleEditText.post {
                titleEditText.requestFocus()
                val imm =
                    titleEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(titleEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }
}
