package com.melkii_mel.activitylog.data

import CalendarSerializer
import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
class ActivityInfoDay(val entries: MutableList<Entry>, @Serializable(with = CalendarSerializer::class) val date: Calendar)
