package com.example.taskmanagerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "lecture_notices")
data class LectureNotice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lectureName: String,
    val date: LocalDate,        // 休講/補講が実施される日
    val type: String,           // "cancel"(休講) または "makeup"(補講)
    val originalDay: String = "", // 補講の場合：元々の曜日（例: "月"）
    val startTime: String = "",
    val endTime: String = "",
    val memo: String = ""
)