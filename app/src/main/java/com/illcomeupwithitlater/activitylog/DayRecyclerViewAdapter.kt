package com.melkii_mel.activitylog

import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.melkii_mel.activitylog.DayRecyclerViewAdapter.ViewHolder
import com.melkii_mel.activitylog.data.ActivityInfoDay
import com.melkii_mel.activitylog.data.Entry
import com.melkii_mel.activitylog.data.Userdata

class DayRecyclerViewAdapter(private val activity: Activity, private val day: ActivityInfoDay) :
    RecyclerView.Adapter<ViewHolder>() {

    private val states: MutableMap<Entry, ViewState> = mutableMapOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.entry_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(day.entries[position])
    }

    override fun getItemCount(): Int {
        return day.entries.count()
    }

    private var itemRemovedListener: (() -> Unit)? = null
    fun setItemRemovedListener(listener: () -> Unit) {
        itemRemovedListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val dateTextView = itemView.findViewById<TextView>(R.id.date_text_view)
        private val numberTextView = itemView.findViewById<TextView>(R.id.number_text_view)
        private val contextMenu = itemView.findViewById<LinearLayout>(R.id.context_menu)
        private val closeContextMenuButton =
            itemView.findViewById<ImageButton>(R.id.close_context_menu_button)
        private val deleteButton = itemView.findViewById<ImageButton>(R.id.delete_button)

        private val state
            get() = states.getOrPut(currentEntry) { ViewState(contextExpanded = false) }

        private lateinit var currentEntry: Entry

        init {
            itemView.setOnLongClickListener {
                contextMenu.visibility = View.VISIBLE
                state.contextExpanded = true
                true
            }
            itemView.setOnClickListener {
                contextMenu.visibility = View.GONE
                state.contextExpanded = false
            }
            closeContextMenuButton.setOnClickListener {
                contextMenu.visibility = View.GONE
                state.contextExpanded = false
            }
            deleteButton.setOnClickListener {
                AlertDialog.Builder(activity).apply {
                    setTitle(context.getString(R.string.delete_record))
                    setMessage(context.getString(R.string.delete_record_warning))
                    setPositiveButton(context.getString(R.string.delete)) { dialog, _ ->
                        val index = day.entries.indexOf(currentEntry)
                        day.entries.removeAt(index)
                        Userdata.current.serialize(context)
                        states.remove(currentEntry)
                        notifyItemRemoved(index)
                        itemRemovedListener?.invoke()
                        dialog.dismiss()
                    }
                    setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
            }
        }

        fun bind(entry: Entry) {
            currentEntry = entry
            dateTextView.text =
                Utils.formatCalendar(entry.date, Utils.Companion.FormatCalendarMode.TIME)
            numberTextView.text = entry.value.toFormattedString()
            contextMenu.visibility = if (state.contextExpanded) View.VISIBLE else View.GONE
        }
    }

    data class ViewState(var contextExpanded: Boolean)
}