package com.example.taskmanagerapp.data

import kotlinx.coroutines.flow.Flow

class TodoCategoryRepository(private val dao: TodoCategoryDao) {

    val allCategories: Flow<List<TodoCategory>> = dao.getAllCategories()

    suspend fun insert(name: String) {
        dao.insertCategory(TodoCategory(name = name))
    }

    suspend fun delete(category: TodoCategory) {
        dao.deleteCategory(category)
    }

    // 初回起動時、カテゴリが1つも無ければ「一般」を自動で作る
    suspend fun ensureDefaults() {
        if (dao.countCategories() == 0) {
            dao.insertCategory(TodoCategory(name = "一般"))
        }
    }
}