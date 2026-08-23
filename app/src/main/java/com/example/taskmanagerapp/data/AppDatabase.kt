package com.example.taskmanagerapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Task::class, Lecture::class, Todo::class, Semester::class,
        TodoCategory::class, Event::class, EventCategory::class,
        LectureNotice::class   // ← 追加
    ],
    version = 12   // ← 現在の番号から+1してください
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun lectureDao(): LectureDao
    abstract fun todoDao(): TodoDao
    abstract fun semesterDao(): SemesterDao
    abstract fun todoCategoryDao(): TodoCategoryDao
    abstract fun eventDao(): EventDao
    abstract fun eventCategoryDao(): EventCategoryDao
    abstract fun lectureNoticeDao(): LectureNoticeDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_manager_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}