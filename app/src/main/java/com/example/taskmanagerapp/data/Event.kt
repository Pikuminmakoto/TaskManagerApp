package com.example.taskmanagerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val date: LocalDate,          // 開始日
    val endDate: LocalDate = date, // 終了日（未指定なら開始日と同じ＝単日）
    val category: String,
    val color: String = "青",
    val startTime: String = "",
    val endTime: String = "",
    val displayStyle: String = "text"
)