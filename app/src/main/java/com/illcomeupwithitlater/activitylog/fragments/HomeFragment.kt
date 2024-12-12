package com.melkii_mel.activitylog.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.illcomeupwithitlater.activitylog.fragments.ActivityInfoFragment
import com.melkii_mel.activitylog.ActivityInfosRecyclerViewAdapter
import com.melkii_mel.activitylog.NewActivityPopupHandler
import com.melkii_mel.activitylog.R
import com.melkii_mel.activitylog.data.Userdata

class HomeFragment: MainActivityFragment() {
    private lateinit var addActivityButton: ImageButton
    lateinit var activityViewRecyclerAdapter: ActivityInfosRecyclerViewAdapter
    private lateinit var activityViewListRecycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addActivityButton = view.findViewById(R.id.add_activity_info_fab)
        activityViewListRecycler = view.findViewById(R.id.activity_info_list_recycler_view)

        addActivityButton.setOnClickListener {
            NewActivityPopupHandler.show(activity) { info ->
                Userdata.current.activityInfos.add(info)
                Userdata.current.serialize(requireContext())
                activityViewListRecycler.adapter?.notifyItemInserted(Userdata.current.activityInfos.count() - 1)
            }
        }

        activityViewListRecycler.layoutManager = LinearLayoutManager(activity)
        activityViewRecyclerAdapter = ActivityInfosRecyclerViewAdapter(activity, Userdata.current.activityInfos) { activityInfo ->
            activity.setBackEnabled(true)
            activity.toolbarTitle.text = activityInfo.name
            activity.setFragment(ActivityInfoFragment(activityInfo))
        }
        activityViewListRecycler.adapter = activityViewRecyclerAdapter
    }
}