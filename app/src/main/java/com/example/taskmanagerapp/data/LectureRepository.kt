package com.example.taskmanagerapp.data

import kotlinx.coroutines.flow.Flow

class LectureRepository(private val lectureDao: LectureDao) {

    val allLectures: Flow<List<Lecture>> = lectureDao.getAllLectures()

    suspend fun insert(lecture: Lecture) {
        lectureDao.insertLecture(lecture)
    }

    suspend fun update(lecture: Lecture) {
        lectureDao.updateLecture(lecture)
    }

    suspend fun delete(lecture: Lecture) {
        lectureDao.deleteLecture(lecture)
    }
}