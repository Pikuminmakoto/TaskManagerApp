package com.example.taskmanagerapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {

    @Query("SELECT * FROM lectures ORDER BY day, period")
    fun getAllLectures(): Flow<List<Lecture>>

    @Query("SELECT * FROM lectures")
    suspend fun getAllLecturesOnce(): List<Lecture>

    @Query("DELETE FROM lectures")
    suspend fun deleteAll(): Int

    @Insert
    suspend fun insertLecture(lecture: Lecture): Long

    @Update
    suspend fun updateLecture(lecture: Lecture): Int

    @Delete
    suspend fun deleteLecture(lecture: Lecture): Int
}