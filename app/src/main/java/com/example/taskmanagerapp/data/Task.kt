package com.example.taskmanagerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lecture: String,
    val assignment: String,
    val deadline: LocalDate,
    val submitted: Boolean
)