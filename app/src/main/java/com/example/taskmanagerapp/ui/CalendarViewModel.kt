package com.example.taskmanagerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.data.Event
import com.example.taskmanagerapp.data.EventCategory
import com.example.taskmanagerapp.data.EventCategoryRepository
import com.example.taskmanagerapp.data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarViewModel(
    private val eventRepository: EventRepository,
    private val categoryRepository: EventCategoryRepository
) : ViewModel() {

    val allEvents: StateFlow<List<Event>> = eventRepository.allEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<EventCategory>> = categoryRepository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedCategory = MutableStateFlow(ALL_CATEGORY)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun selectCategory(name: String) {
        _selectedCategory.value = name
    }

    fun addEvent(
        title: String,
        startDate: LocalDate,
        endDate: LocalDate,
        color: String,
        startTime: String,
        endTime: String,
        displayStyle: String
    ) {
        val category = if (_selectedCategory.value == ALL_CATEGORY) "一般" else _selectedCategory.value

        viewModelScope.launch {
            eventRepository.insert(
                Event(
                    title = title,
                    date = startDate,
                    endDate = endDate,
                    category = category,
                    color = color,
                    startTime = startTime,
                    endTime = endTime,
                    displayStyle = displayStyle
                )
            )
        }
    }

    // これまでに入力した予定から、入力中の文字を含むものを候補として返す（タイトル重複は最新のものだけ）
    fun suggestEvents(query: String): List<Event> {
        if (query.isBlank()) return emptyList()

        return allEvents.value
            .filter { it.title.contains(query) && it.title != query }
            .groupBy { it.title }
            .map { (_, list) -> list.maxByOrNull { it.id }!! }
            .take(5)
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.delete(event)
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insert(name)
        }
    }

    fun deleteCategory(category: EventCategory) {
        viewModelScope.launch {
            allEvents.value
                .filter { it.category == category.name }
                .forEach { eventRepository.update(it.copy(category = "一般")) }

            categoryRepository.delete(category)

            if (_selectedCategory.value == category.name) {
                _selectedCategory.value = ALL_CATEGORY
            }
        }
    }
    fun updateEvent(
        event: Event,
        title: String,
        endDate: LocalDate,
        color: String,
        startTime: String,
        endTime: String,
        displayStyle: String
    ) {
        viewModelScope.launch {
            eventRepository.update(
                event.copy(
                    title = title,
                    endDate = endDate,
                    color = color,
                    startTime = startTime,
                    endTime = endTime,
                    displayStyle = displayStyle
                )
            )
        }
    }

    class Factory(
        private val eventRepository: EventRepository,
        private val categoryRepository: EventCategoryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(eventRepository, categoryRepository) as T
        }
    }
}