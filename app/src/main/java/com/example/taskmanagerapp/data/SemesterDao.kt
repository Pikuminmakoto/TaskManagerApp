package com.example.taskmanagerapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {

    @Query("SELECT * FROM semesters ORDER BY id ASC")
    fun getAllSemesters(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters")
    suspend fun getAllSemestersOnce(): List<Semester>

    @Query("DELETE FROM semesters")
    suspend fun deleteAll(): Int

    @Insert
    suspend fun insertSemester(semester: Semester): Long

    @Delete
    suspend fun deleteSemester(semester: Semester): Int

    @Query("SELECT COUNT(*) FROM semesters")
    suspend fun countSemesters(): Int

    @Update
    suspend fun updateSemester(semester: Semester): Int
}