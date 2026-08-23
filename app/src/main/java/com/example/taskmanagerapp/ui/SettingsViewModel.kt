package com.example.taskmanagerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.data.AppSettings
import com.example.taskmanagerapp.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.taskmanagerapp.data.BreakPeriod

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setMaxPeriod(value: Int) {
        viewModelScope.launch { repository.setMaxPeriod(value) }
    }

    fun setIncludeSaturday(value: Boolean) {
        viewModelScope.launch { repository.setIncludeSaturday(value) }
    }

    fun setIncludeSunday(value: Boolean) {
        viewModelScope.launch { repository.setIncludeSunday(value) }
    }

    fun setPeriodTime(period: Int, time: String) {
        viewModelScope.launch { repository.setPeriodTime(period, time) }
    }
    fun setTabOrder(order: List<String>) {
        viewModelScope.launch { repository.setTabOrder(order) }
    }

    fun moveTab(tabId: String, direction: Int) {
        val current = settings.value.tabOrder.toMutableList()
        val index = current.indexOf(tabId)
        val newIndex = index + direction

        if (index !in current.indices || newIndex !in current.indices) return

        val temp = current[index]
        current[index] = current[newIndex]
        current[newIndex] = temp

        setTabOrder(current)
    }

    fun addBreak(name: String, start: java.time.LocalDate, end: java.time.LocalDate) {
        viewModelScope.launch { repository.addBreak(name, start, end) }
    }

    fun deleteBreak(b: BreakPeriod) {
        viewModelScope.launch { repository.deleteBreak(b) }
    }

    fun setLectureNotifyEnabled(value: Boolean) {
        viewModelScope.launch { repository.setLectureNotifyEnabled(value) }
    }

    fun setLectureNotifyTime(time: String) {
        viewModelScope.launch { repository.setLectureNotifyTime(time) }
    }

    fun setTaskNotifyEnabled(value: Boolean) {
        viewModelScope.launch { repository.setTaskNotifyEnabled(value) }
    }

    fun setOnboardingDone() {
        viewModelScope.launch { repository.setOnboardingDone() }
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
    }
}