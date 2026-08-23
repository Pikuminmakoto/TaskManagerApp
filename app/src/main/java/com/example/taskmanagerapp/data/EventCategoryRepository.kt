package com.example.taskmanagerapp.data

import kotlinx.coroutines.flow.Flow

class EventCategoryRepository(private val dao: EventCategoryDao) {

    val allCategories: Flow<List<EventCategory>> = dao.getAllCategories()

    suspend fun insert(name: String) {
        dao.insertCategory(EventCategory(name = name))
    }

    suspend fun delete(category: EventCategory) {
        dao.deleteCategory(category)
    }

    suspend fun ensureDefaults() {
        if (dao.countCategories() == 0) {
            dao.insertCategory(EventCategory(name = "一般"))
        }
    }
}