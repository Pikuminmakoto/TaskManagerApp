package com.example.taskmanagerapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventCategoryDao {

    @Query("SELECT * FROM event_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<EventCategory>>

    @Query("SELECT * FROM event_categories")
    suspend fun getAllCategoriesOnce(): List<EventCategory>

    @Query("DELETE FROM event_categories")
    suspend fun deleteAll(): Int

    @Insert
    suspend fun insertCategory(category: EventCategory): Long

    @Delete
    suspend fun deleteCategory(category: EventCategory): Int

    @Query("SELECT COUNT(*) FROM event_categories")
    suspend fun countCategories(): Int
}