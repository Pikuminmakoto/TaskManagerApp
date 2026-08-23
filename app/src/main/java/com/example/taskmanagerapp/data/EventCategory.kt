package com.example.taskmanagerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_categories")
data class EventCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)