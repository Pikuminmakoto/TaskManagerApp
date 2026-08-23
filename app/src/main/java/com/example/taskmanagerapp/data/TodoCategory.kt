package com.example.taskmanagerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_categories")
data class TodoCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)