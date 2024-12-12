package com.melkii_mel.activitylog.fragments

import androidx.fragment.app.Fragment
import com.melkii_mel.activitylog.MainActivity

abstract class MainActivityFragment: Fragment() {
    val activity: MainActivity
        get() = requireActivity() as MainActivity
}