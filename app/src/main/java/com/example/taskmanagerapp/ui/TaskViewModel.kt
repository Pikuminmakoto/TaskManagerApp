package com.example.taskmanagerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.data.Task
import com.example.taskmanagerapp.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val allTasks: StateFlow<List<Task>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTask(lecture: String, assignment: String, deadline: java.time.LocalDate) {
        viewModelScope.launch {
            repository.insert(
                Task(
                    lecture = lecture,
                    assignment = assignment,
                    deadline = deadline,
                    submitted = false
                )
            )
        }
    }

    fun toggleSubmitted(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(submitted = !task.submitted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }

    fun updateTask(task: Task, lecture: String, assignment: String, deadline: LocalDate) {
        viewModelScope.launch {
            repository.update(task.copy(lecture = lecture, assignment = assignment, deadline = deadline))
        }
    }

    fun restoreTask(task: Task) {
        viewModelScope.launch {
            repository.insert(task.copy(id = 0))
        }
    }

    // ViewModelの作り方をAndroidに教えるための仕組み
    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
    }
}