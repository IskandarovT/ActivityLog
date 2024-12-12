package com.melkii_mel.activitylog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.melkii_mel.activitylog.ActivityInfoRecyclerViewAdapter.ViewHolder
import com.melkii_mel.activitylog.data.ActivityInfo
import com.melkii_mel.activitylog.data.ActivityInfoDay
import com.melkii_mel.activitylog.data.Userdata

class ActivityInfoRecyclerViewAdapter(
    private var activity: Activity, private var activityInfo: ActivityInfo? = null
) : RecyclerView.Adapter<ViewHolder>() {

    private var states: MutableMap<ActivityInfoDay, ViewState> = mutableMapOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.entries_day_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(activityInfo!!.days[position])
    }

    override fun getItemCount(): Int {
        return activityInfo?.days?.size ?: 0
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateActivityInfo(new: ActivityInfo) {
        activityInfo = new
        bindActivityInfo()
        notifyDataSetChanged()
    }

    private val daysAdapters: MutableMap<ActivityInfoDay, DayRecyclerViewAdapter> = mutableMapOf()

    //TODO: Optimize if necessary
    @SuppressLint("NotifyDataSetChanged")
    fun bindActivityInfo() {
        fun bindDay(day: ActivityInfoDay, position: Int) {
            val adapter = DayRecyclerViewAdapter(activity, day)
            daysAdapters[day] = adapter
            adapter.setItemRemovedListener {
                if (day.entries.isEmpty()) {
                    deleteDay(position)
                } else {
                    notifyItemChanged(position)
                }
            }
            notifyDataSetChanged()
        }

        if (activityInfo == null) {
            throw IllegalStateException("Can't bind empty ActivityInfo!")
        }

        var position = 0
        for (day in activityInfo!!.days) {
            bindDay(day, position++)
        }
        activityInfo!!.setDayAddedListener { day, pos ->
            bindDay(day, pos)
        }
        activityInfo!!.setEntryAddedListener { day, _, _ ->
            daysAdapters[day]!!.notifyDataSetChanged()
            notifyItemChanged(activityInfo!!.days.indexOf(day))
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val showChildrenButton =
            itemView.findViewById<ImageButton>(R.id.show_children_toggle)
        private var showChildrenButtonChecked = false
        private val dateTextView = itemView.findViewById<TextView>(R.id.date_text_view)
        private val bestTextView = itemView.findViewById<TextView>(R.id.best_text_view)
        private val totalTextView = itemView.findViewById<TextView>(R.id.total_text_view)
        private val entriesRecycler =
            itemView.findViewById<RecyclerView>(R.id.entries_recycler_view)
        private val entriesRecyclerViewHolder =
            itemView.findViewById<FrameLayout>(R.id.entries_recycler_view_holder)
        private val entriesDayHeader = itemView.findViewById<ViewGroup>(R.id.entries_day_header)

        private val contextMenu = itemView.findViewById<FrameLayout>(R.id.context_menu)
        private val closeContextMenuButton =
            itemView.findViewById<ImageButton>(R.id.close_context_menu_button)
        private val deleteButton = itemView.findViewById<ImageButton>(R.id.delete_button)

        private lateinit var currentDay: ActivityInfoDay

        private val state: ViewState
            get() = states.getOrPut(currentDay) { ViewState() }

        init {
            entriesRecycler.layoutManager = LinearLayoutManager(activity)

            showChildrenButton.setOnClickListener {
                state.isExpanded = !state.isExpanded
                showChildrenButtonChecked = state.isExpanded
                if (state.isExpanded) {
                    entriesDayHeader.setBackgroundResource(R.drawable.ripple_round_top_corners_10dp_charcoal)
                    entriesRecyclerViewHolder.visibility = View.VISIBLE
                    showChildrenButton.setImageResource(R.drawable.chevron_up)
                    slideIn(entriesRecycler, 100)
                } else {
                    showChildrenButton.setImageResource(R.drawable.chevron_down)
                    slideOut(entriesRecycler, 100, null) {
                        entriesRecyclerViewHolder.visibility = View.GONE
                        entriesDayHeader.setBackgroundResource(R.drawable.ripple_round_corners_10dp_charcoal)
                    }
                }
            }
            itemView.setOnLongClickListener {
                contextMenu.visibility = View.VISIBLE
                state.isContextExpanded = true
                true
            }
            itemView.setOnClickListener {
                contextMenu.visibility = View.GONE
                state.isContextExpanded = false
            }
            closeContextMenuButton.setOnClickListener {
                contextMenu.visibility = View.GONE
                state.isContextExpanded = false
            }
            deleteButton.setOnClickListener {
                AlertDialog.Builder(activity).apply {
                    setTitle(context.getString(R.string.delete_day_of_records))
                    setMessage(context.getString(R.string.delete_day_warning))
                    setPositiveButton(context.getString(R.string.delete)) { dialog, _ ->
                        deleteDay(adapterPosition)
                        dialog.dismiss()
                    }
                    setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
            }
        }

        fun bind(day: ActivityInfoDay) {
            currentDay = day

            dateTextView.text = Utils.formatCalendar(day.date, Utils.Companion.FormatCalendarMode.DATE)
            bestTextView.text = activityInfo?.getBestInDay(day)?.value?.toFormattedString() ?: "N/A"
            totalTextView.text = activityInfo?.getTotalInDay(day)?.toFormattedString(2) ?: "N/A"

            if (entriesRecycler.adapter == null) {
                entriesRecycler.adapter = daysAdapters[day]
            }

            contextMenu.visibility = if (state.isContextExpanded) View.VISIBLE else View.GONE

            if (showChildrenButtonChecked != state.isExpanded) {
                state.isExpanded = showChildrenButtonChecked
                showChildrenButton.performClick()
            }
        }
    }

    data class ViewState(var isExpanded: Boolean = false, var isContextExpanded: Boolean = false)

    private fun deleteDay(position: Int) {
        daysAdapters.remove(activityInfo!!.days[position])
        states.remove(activityInfo!!.days[position])
        activityInfo!!.removeDayAt(position)
        Userdata.current.serialize(activity)
        notifyItemRemoved(position)
    }
}
