package com.melkii_mel.activitylog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.melkii_mel.activitylog.data.ActivityInfo
import com.melkii_mel.activitylog.data.Userdata

class ActivityInfosRecyclerViewAdapter(
    private val activity: AppCompatActivity,
    private var activityInfos: MutableList<ActivityInfo>,
    private val activityInfoSelectedListener: ActivityInfoSelectedListener
) :
    RecyclerView.Adapter<ActivityInfosRecyclerViewAdapter.MyViewHolder>() {

    private val states: MutableMap<ActivityInfo, State> = mutableMapOf()

    fun interface ActivityInfoSelectedListener {
        fun onActivityInfoSelected(activityInfo: ActivityInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_button, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(activityInfos[position])
    }

    override fun getItemCount(): Int {
        return activityInfos.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun changeDataSet(activityInfos: MutableList<ActivityInfo>) {
        this.activityInfos = activityInfos
        notifyDataSetChanged()
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button: Button = itemView.findViewById(R.id.button)
        private val contextMenu = itemView.findViewById<LinearLayout>(R.id.context_menu)
        private val closeContextMenuButton = itemView.findViewById<ImageButton>(R.id.close_context_menu_button)
        private val deleteButton = itemView.findViewById<ImageButton>(R.id.delete_button)
        private val shareButton = itemView.findViewById<ImageButton>(R.id.share_button)
        private val saveButton = itemView.findViewById<ImageButton>(R.id.save_button)

        private lateinit var currentActivityInfo: ActivityInfo
        private val state
            get() = states.getOrPut(currentActivityInfo) { State(contextExpanded = false) }

        init {
            button.setOnClickListener {
                if (contextMenu.visibility == View.VISIBLE) {
                    closeContextMenu()
                }
                else {
                    activityInfoSelectedListener.onActivityInfoSelected(currentActivityInfo)
                }
            }
            button.setOnLongClickListener {
                openContextMenu()
                true
            }
            closeContextMenuButton.setOnClickListener {
                NewActivityPopupHandler.show(activity, currentActivityInfo) { newActivityInfo ->
                    currentActivityInfo.name = newActivityInfo.name
                    currentActivityInfo.bestIsMin = newActivityInfo.bestIsMin
                    currentActivityInfo.realNumbers = newActivityInfo.realNumbers
                    Userdata.current.serialize(activity)
                    notifyItemChanged(adapterPosition)
                }
            }
            deleteButton.setOnClickListener {
                AlertDialog.Builder(activity).apply {
                    setTitle(context.getString(R.string.delete_activity))
                    setMessage(context.getString(R.string.delete_activity_warning))
                    setPositiveButton(context.getString(R.string.delete)) { dialog, _ ->
                        states.remove(currentActivityInfo)
                        activityInfos.removeAt(adapterPosition)
                        Userdata.current.serialize(context)
                        notifyItemRemoved(adapterPosition)
                        dialog.dismiss()
                    }
                    setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    show()
                }
            }
            shareButton.setOnClickListener {
                val applicationId = BuildConfig.APPLICATION_ID
                val file = currentActivityInfo.toCsvFile(activity)
                @Suppress("SpellCheckingInspection") val uri = FileProvider.getUriForFile(activity, "${applicationId}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                activity.startActivity(Intent.createChooser(intent,
                    activity.getString(R.string.share_csv)))
                file.deleteOnExit()
            }
            saveButton.setOnClickListener {
                val fileInfo = currentActivityInfo.toCsvFileInfo()
                saveFileWithUserSelection(fileInfo.fileName, fileInfo.mimeType, fileInfo.content)
            }
        }

        fun bind(activityInfo: ActivityInfo) {
            currentActivityInfo = activityInfo
            contextMenu.visibility = if (state.contextExpanded) View.VISIBLE else View.GONE
            button.text = activityInfo.name
        }

        private fun openContextMenu() {
            contextMenu.visibility = View.VISIBLE
            state.contextExpanded = true
        }

        private fun closeContextMenu() {
            contextMenu.visibility = View.GONE
            state.contextExpanded = false
        }
    }

    data class State(var contextExpanded: Boolean)
}
