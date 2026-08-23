package com.example.taskmanagerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val completed: Boolean,
    val category: String,
    val order: Int = 0   // ← 追加
)