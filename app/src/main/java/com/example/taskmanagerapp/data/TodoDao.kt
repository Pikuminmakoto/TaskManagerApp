package com.example.taskmanagerapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY completed ASC, `order` ASC")
    fun getAllTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos")
    suspend fun getAllTodosOnce(): List<Todo>

    @Query("DELETE FROM todos")
    suspend fun deleteAll(): Int

    @Insert
    suspend fun insertTodo(todo: Todo): Long

    @Update
    suspend fun updateTodo(todo: Todo): Int

    @Delete
    suspend fun deleteTodo(todo: Todo): Int
}