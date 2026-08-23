package com.example.taskmanagerapp

import android.app.Application
import com.example.taskmanagerapp.data.AppDatabase
import com.example.taskmanagerapp.data.EventCategoryRepository
import com.example.taskmanagerapp.data.EventRepository
import com.example.taskmanagerapp.data.LectureRepository
import com.example.taskmanagerapp.data.SemesterRepository
import com.example.taskmanagerapp.data.SettingsRepository
import com.example.taskmanagerapp.data.TaskRepository
import com.example.taskmanagerapp.data.TodoCategoryRepository
import com.example.taskmanagerapp.data.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.taskmanagerapp.data.LectureNoticeRepository

class TaskManagerApplication : Application() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val taskRepository by lazy { TaskRepository(database.taskDao()) }
    val lectureRepository by lazy { LectureRepository(database.lectureDao()) }
    val todoRepository by lazy { TodoRepository(database.todoDao()) }
    val semesterRepository by lazy { SemesterRepository(database.semesterDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val todoCategoryRepository by lazy { TodoCategoryRepository(database.todoCategoryDao()) }
    val eventRepository by lazy { EventRepository(database.eventDao()) }
    val eventCategoryRepository by lazy { EventCategoryRepository(database.eventCategoryDao()) }

    val lectureNoticeRepository by lazy { LectureNoticeRepository(database.lectureNoticeDao()) }   // ← 追加

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            semesterRepository.ensureDefaults()
            todoCategoryRepository.ensureDefaults()
            eventCategoryRepository.ensureDefaults()
        }
    }
}