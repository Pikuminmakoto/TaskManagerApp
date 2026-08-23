package com.example.taskmanagerapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LectureNoticeRepository(private val dao: LectureNoticeDao) {

    val allNotices: Flow<List<LectureNotice>> = dao.getAllNotices()

    suspend fun insert(notice: LectureNotice) {
        dao.insertNotice(notice)
    }

    suspend fun delete(notice: LectureNotice) {
        dao.deleteNotice(notice)
    }

    suspend fun deleteByLectureName(lectureName: String) {
        dao.getAllNotices().first()
            .filter { it.lectureName == lectureName }
            .forEach { dao.deleteNotice(it) }
    }
}