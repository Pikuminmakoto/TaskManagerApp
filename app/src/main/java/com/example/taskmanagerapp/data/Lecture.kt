package com.example.taskmanagerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val day: String,
    val period: Int,
    val room: String,
    val semester: String,
    val color: String,
    val memo: String = ""   // ← 追加
)