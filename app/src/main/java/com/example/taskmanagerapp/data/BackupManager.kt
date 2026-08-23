package com.example.taskmanagerapp.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object BackupManager {

    suspend fun export(context: Context, uri: Uri) {
        val db = AppDatabase.getDatabase(context)
        val root = JSONObject()

        root.put("tasks", JSONArray().apply {
            db.taskDao().getAllTasksOnce().forEach {
                put(JSONObject().apply {
                    put("lecture", it.lecture)
                    put("assignment", it.assignment)
                    put("deadline", it.deadline.toString())
                    put("submitted", it.submitted)
                })
            }
        })

        root.put("lectures", JSONArray().apply {
            db.lectureDao().getAllLecturesOnce().forEach {
                put(JSONObject().apply {
                    put("name", it.name)
                    put("day", it.day)
                    put("period", it.period)
                    put("room", it.room)
                    put("semester", it.semester)
                    put("color", it.color)
                    put("memo", it.memo)
                })
            }
        })

        root.put("todos", JSONArray().apply {
            db.todoDao().getAllTodosOnce().forEach {
                put(JSONObject().apply {
                    put("text", it.text)
                    put("completed", it.completed)
                    put("category", it.category)
                    put("order", it.order)
                })
            }
        })

        root.put("todoCategories", JSONArray().apply {
            db.todoCategoryDao().getAllCategoriesOnce().forEach {
                put(JSONObject().apply { put("name", it.name) })
            }
        })

        root.put("semesters", JSONArray().apply {
            db.semesterDao().getAllSemestersOnce().forEach {
                put(JSONObject().apply { put("name", it.name) })
            }
        })

        root.put("events", JSONArray().apply {
            db.eventDao().getAllEventsOnce().forEach {
                put(JSONObject().apply {
                    put("title", it.title)
                    put("date", it.date.toString())
                    put("endDate", it.endDate.toString())
                    put("category", it.category)
                    put("color", it.color)
                    put("startTime", it.startTime)
                    put("endTime", it.endTime)
                    put("displayStyle", it.displayStyle)
                })
            }
        })

        root.put("eventCategories", JSONArray().apply {
            db.eventCategoryDao().getAllCategoriesOnce().forEach {
                put(JSONObject().apply { put("name", it.name) })
            }
        })

        root.put("lectureNotices", JSONArray().apply {
            db.lectureNoticeDao().getAllNoticesOnce().forEach {
                put(JSONObject().apply {
                    put("lectureName", it.lectureName)
                    put("date", it.date.toString())
                    put("type", it.type)
                    put("originalDay", it.originalDay)
                    put("startTime", it.startTime)
                    put("endTime", it.endTime)
                    put("memo", it.memo)
                })
            }
        })

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString(2).toByteArray())
        }
    }

    suspend fun import(context: Context, uri: Uri) {
        val db = AppDatabase.getDatabase(context)

        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() } ?: return

        val root = JSONObject(text)

        db.taskDao().deleteAll()
        db.lectureDao().deleteAll()
        db.todoDao().deleteAll()
        db.todoCategoryDao().deleteAll()
        db.semesterDao().deleteAll()
        db.eventDao().deleteAll()
        db.eventCategoryDao().deleteAll()
        db.lectureNoticeDao().deleteAll()

        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.taskDao().insertTask(
                    Task(
                        lecture = o.getString("lecture"),
                        assignment = o.getString("assignment"),
                        deadline = LocalDate.parse(o.getString("deadline")),
                        submitted = o.getBoolean("submitted")
                    )
                )
            }
        }

        root.optJSONArray("lectures")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.lectureDao().insertLecture(
                    Lecture(
                        name = o.getString("name"),
                        day = o.getString("day"),
                        period = o.getInt("period"),
                        room = o.getString("room"),
                        semester = o.getString("semester"),
                        color = o.getString("color"),
                        memo = o.optString("memo", "")
                    )
                )
            }
        }

        root.optJSONArray("todos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.todoDao().insertTodo(
                    Todo(
                        text = o.getString("text"),
                        completed = o.getBoolean("completed"),
                        category = o.getString("category"),
                        order = o.optInt("order", 0)
                    )
                )
            }
        }

        root.optJSONArray("todoCategories")?.let { arr ->
            for (i in 0 until arr.length()) {
                db.todoCategoryDao().insertCategory(TodoCategory(name = arr.getJSONObject(i).getString("name")))
            }
        }

        root.optJSONArray("semesters")?.let { arr ->
            for (i in 0 until arr.length()) {
                db.semesterDao().insertSemester(Semester(name = arr.getJSONObject(i).getString("name")))
            }
        }

        root.optJSONArray("events")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.eventDao().insertEvent(
                    Event(
                        title = o.getString("title"),
                        date = LocalDate.parse(o.getString("date")),
                        endDate = LocalDate.parse(o.optString("endDate", o.getString("date"))),
                        category = o.getString("category"),
                        color = o.optString("color", "青"),
                        startTime = o.optString("startTime", ""),
                        endTime = o.optString("endTime", ""),
                        displayStyle = o.optString("displayStyle", "text")
                    )
                )
            }
        }

        root.optJSONArray("eventCategories")?.let { arr ->
            for (i in 0 until arr.length()) {
                db.eventCategoryDao().insertCategory(EventCategory(name = arr.getJSONObject(i).getString("name")))
            }
        }

        root.optJSONArray("lectureNotices")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.lectureNoticeDao().insertNotice(
                    LectureNotice(
                        lectureName = o.getString("lectureName"),
                        date = LocalDate.parse(o.getString("date")),
                        type = o.getString("type"),
                        originalDay = o.optString("originalDay", ""),
                        startTime = o.optString("startTime", ""),
                        endTime = o.optString("endTime", ""),
                        memo = o.optString("memo", "")
                    )
                )
            }
        }
    }
}