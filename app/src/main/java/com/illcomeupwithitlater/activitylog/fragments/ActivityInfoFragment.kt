package com.illcomeupwithitlater.activitylog.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.melkii_mel.activitylog.ActivityInfoRecyclerViewAdapter
import com.melkii_mel.activitylog.R
import com.melkii_mel.activitylog.Utils
import com.melkii_mel.activitylog.closeKeyboard
import com.melkii_mel.activitylog.data.ActivityInfo
import com.melkii_mel.activitylog.data.Entry
import com.melkii_mel.activitylog.data.Userdata
import com.melkii_mel.activitylog.fragments.MainActivityFragment
import com.melkii_mel.activitylog.setOnTextChangedListener
import java.util.Calendar

class ActivityInfoFragment(private val activityInfo: ActivityInfo) : MainActivityFragment() {
    private lateinit var activityViewRecycler: RecyclerView
    private lateinit var activityViewRecyclerAdapter: ActivityInfoRecyclerViewAdapter
    private lateinit var quantityEditText: EditText
    private lateinit var removeDateOverrideButton: ImageButton
    private lateinit var overriddenDateTextView: TextView
    private lateinit var overriddenDateTextViewLayout: ViewGroup
    private lateinit var overrideDateButton: ImageButton
    private lateinit var addEntryButton: ImageButton

    private var dateOverridden: Boolean = false
    private var lastCalendar: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_info_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityViewRecycler = view.findViewById(R.id.activity_info_recycler_view)
        quantityEditText = view.findViewById(R.id.quantity_edit_text)
        removeDateOverrideButton = view.findViewById(R.id.remove_date_override_button)
        overriddenDateTextView = view.findViewById<EditText>(R.id.overridden_date_text_view)
        overrideDateButton = view.findViewById(R.id.override_date_button)
        addEntryButton = view.findViewById(R.id.add_entry_button)
        overriddenDateTextViewLayout = view.findViewById(R.id.overridden_date_text_view_layout)

        activityViewRecycler.layoutManager = LinearLayoutManager(requireActivity())
        activityViewRecyclerAdapter = ActivityInfoRecyclerViewAdapter(activity, activityInfo)
        activityViewRecycler.adapter = activityViewRecyclerAdapter
        activityViewRecyclerAdapter.bindActivityInfo()

        addEntryButton.visibility = View.GONE

        quantityEditText.setOnTextChangedListener {
            updateAddEntryButton()
        }

        quantityEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addEntry()
            }
            actionId == EditorInfo.IME_ACTION_DONE
        }

        removeDateOverrideButton.setOnClickListener {
            removeDateOverride()
        }
        overrideDateButton.setOnClickListener {
            Utils.showDateTimePickerDialog(requireContext(), lastCalendar) { calendar ->
                addDateOverride(calendar)
            }
        }

        addEntryButton.setOnClickListener {
            addEntry()
        }
    }

    private fun addDateOverride(calendar: Calendar) {
        lastCalendar = calendar
        dateOverridden = true
        overriddenDateTextView.text = Utils.formatCalendar(calendar)
        overriddenDateTextViewLayout.visibility = View.VISIBLE
        removeDateOverrideButton.visibility = View.VISIBLE
    }

    private fun removeDateOverride() {
        dateOverridden = false
        overriddenDateTextViewLayout.visibility = View.GONE
        removeDateOverrideButton.visibility = View.GONE
    }

    private fun updateAddEntryButton() {
        val quantityString = quantityEditText.text.toString()
        val valid = if (activityInfo.realNumbers) {
            quantityString.toDoubleOrNull() != null
        } else {
            quantityString.toIntOrNull() != null
        }
        addEntryButton.visibility = if (valid) View.VISIBLE else View.GONE
    }

    private fun addEntry() {
        val newEntry = Entry(
            if (dateOverridden) (lastCalendar?.clone() as? Calendar)
                ?: Calendar.getInstance() else Calendar.getInstance(),
            quantityEditText.text.toString().toDoubleOrNull() ?: 0.0
        )
        activityInfo.add(newEntry)
        Userdata.current.serialize(requireContext())
        quantityEditText.text.clear()
        closeKeyboard(requireActivity())
    }
}