package com.example.taskmanagerapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureNoticeDao {

    @Query("SELECT * FROM lecture_notices ORDER BY date ASC")
    fun getAllNotices(): Flow<List<LectureNotice>>

    @Query("SELECT * FROM lecture_notices")
    suspend fun getAllNoticesOnce(): List<LectureNotice>

    @Query("DELETE FROM lecture_notices")
    suspend fun deleteAll(): Int

    @Insert
    suspend fun insertNotice(notice: LectureNotice): Long

    @Delete
    suspend fun deleteNotice(notice: LectureNotice): Int
}