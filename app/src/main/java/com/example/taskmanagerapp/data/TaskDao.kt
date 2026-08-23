package com.example.taskmanagerapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll(): Int

    @Insert
    suspend fun insertTask(task: Task): Long   // ← 戻り値を追加(挿入されたIDが返る)

    @Update
    suspend fun updateTask(task: Task): Int    // ← 戻り値を追加(更新された行数が返る)

    @Delete
    suspend fun deleteTask(task: Task): Int    // ← 戻り値を追加(削除された行数が返る)
}