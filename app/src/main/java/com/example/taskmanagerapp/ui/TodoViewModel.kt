package com.example.taskmanagerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.data.Todo
import com.example.taskmanagerapp.data.TodoCategory
import com.example.taskmanagerapp.data.TodoCategoryRepository
import com.example.taskmanagerapp.data.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val ALL_CATEGORY = "すべて"

class TodoViewModel(
    private val todoRepository: TodoRepository,
    private val categoryRepository: TodoCategoryRepository
) : ViewModel() {

    val allTodos: StateFlow<List<Todo>> = todoRepository.allTodos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<TodoCategory>> = categoryRepository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORY)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun selectCategory(name: String) {
        _selectedCategory.value = name
    }

    fun addTodo(text: String) {
        val category = if (_selectedCategory.value == ALL_CATEGORY) "一般" else _selectedCategory.value
        val nextOrder = (allTodos.value.maxOfOrNull { it.order } ?: 0) + 1

        viewModelScope.launch {
            todoRepository.insert(
                Todo(
                    text = text,
                    completed = false,
                    category = category,
                    order = nextOrder
                )
            )
        }
    }

    fun toggleCompleted(todo: Todo) {
        viewModelScope.launch {
            todoRepository.update(todo.copy(completed = !todo.completed))
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.delete(todo)
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insert(name)
        }
    }

    // カテゴリ削除。そのカテゴリのToDoは「一般」へ移動してから削除する
    fun deleteCategory(category: TodoCategory) {
        viewModelScope.launch {
            allTodos.value
                .filter { it.category == category.name }
                .forEach { todo ->
                    todoRepository.update(todo.copy(category = "一般"))
                }

            categoryRepository.delete(category)

            if (_selectedCategory.value == category.name) {
                _selectedCategory.value = ALL_CATEGORY
            }
        }
    }

    fun deleteCompleted() {
        viewModelScope.launch {
            allTodos.value
                .filter { it.completed }
                .forEach { todoRepository.delete(it) }
        }
    }

    fun moveTodo(todo: Todo, direction: Int) {
        viewModelScope.launch {
            val list = allTodos.value.toMutableList()
            val index = list.indexOf(todo)
            val newIndex = index + direction

            if (index !in list.indices || newIndex !in list.indices) return@launch

            val target = list[newIndex]

            val tempOrder = todo.order
            todoRepository.update(todo.copy(order = target.order))
            todoRepository.update(target.copy(order = tempOrder))
        }
    }

    fun updateTodoText(todo: Todo, text: String) {
        viewModelScope.launch {
            todoRepository.update(todo.copy(text = text))
        }
    }

    fun restoreTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.insert(todo.copy(id = 0))
        }
    }

    class Factory(
        private val todoRepository: TodoRepository,
        private val categoryRepository: TodoCategoryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(todoRepository, categoryRepository) as T
        }
    }
}