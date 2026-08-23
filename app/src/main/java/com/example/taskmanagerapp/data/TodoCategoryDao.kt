package com.example.taskmanagerapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoCategoryDao {

    @Query("SELECT * FROM todo_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<TodoCategory>>

    @Query("SELECT * FROM todo_categories")
    suspend fun getAllCategoriesOnce(): List<TodoCategory>

    @Query("DELETE FROM todo_categories")
    suspend fun deleteAll(): Int

    @Insert
    suspend fun insertCategory(category: TodoCategory): Long

    @Delete
    suspend fun deleteCategory(category: TodoCategory): Int

    @Query("SELECT COUNT(*) FROM todo_categories")
    suspend fun countCategories(): Int
}