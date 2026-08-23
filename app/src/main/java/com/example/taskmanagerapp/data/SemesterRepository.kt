package com.example.taskmanagerapp.data

import kotlinx.coroutines.flow.Flow

class SemesterRepository(private val semesterDao: SemesterDao) {

    val allSemesters: Flow<List<Semester>> = semesterDao.getAllSemesters()

    suspend fun insert(name: String) {
        semesterDao.insertSemester(Semester(name = name))
    }

    suspend fun delete(semester: Semester) {
        semesterDao.deleteSemester(semester)
    }

    suspend fun rename(semester: Semester, newName: String) {
        semesterDao.updateSemester(semester.copy(name = newName))
    }

    // 初回起動時、学期が1つも無ければ「前期」「後期」を自動で作る
    suspend fun ensureDefaults() {
        if (semesterDao.countSemesters() == 0) {
            semesterDao.insertSemester(Semester(name = "前期"))
            semesterDao.insertSemester(Semester(name = "後期"))
        }
    }
}