package com.example.taskmanagerapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val defaultTabOrder = listOf("task", "lecture", "todo", "calendar")

data class BreakPeriod(val name: String, val start: LocalDate, val end: LocalDate)

data class AppSettings(
    val maxPeriod: Int = 5,
    val includeSaturday: Boolean = false,
    val includeSunday: Boolean = false,
    val periodTimes: List<String> = List(8) { "" },
    val tabOrder: List<String> = defaultTabOrder,
    val breaks: List<BreakPeriod> = emptyList(),
    val lectureNotifyEnabled: Boolean = true,
    val lectureNotifyTime: String = "",
    val taskNotifyEnabled: Boolean = true,
    val onboardingDone: Boolean = false
)

// 講義通知の実際の時刻を計算する（1限の開始時刻-1時間、無ければ7:00）
fun effectiveLectureNotifyTime(settings: AppSettings): String {
    if (settings.lectureNotifyTime.isNotBlank()) return settings.lectureNotifyTime

    val period1 = settings.periodTimes.getOrNull(0) ?: ""
    val start = period1.split("~").getOrNull(0)

    if (!start.isNullOrBlank()) {
        val parts = start.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()
        val m = parts.getOrNull(1)?.toIntOrNull()

        if (h != null && m != null) {
            var newHour = h - 1
            if (newHour < 0) newHour += 24
            return String.format("%02d:%02d", newHour, m)
        }
    }

    return "07:00"
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val MAX_PERIOD = intPreferencesKey("max_period")
        val INCLUDE_SATURDAY = booleanPreferencesKey("include_saturday")
        val INCLUDE_SUNDAY = booleanPreferencesKey("include_sunday")
        val PERIOD_TIMES = stringPreferencesKey("period_times")
        val TAB_ORDER = stringPreferencesKey("tab_order")

        val BREAKS = stringPreferencesKey("breaks")
        val LECTURE_NOTIFY_ENABLED = booleanPreferencesKey("lecture_notify_enabled")
        val LECTURE_NOTIFY_TIME = stringPreferencesKey("lecture_notify_time")
        val TASK_NOTIFY_ENABLED = booleanPreferencesKey("task_notify_enabled")

        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->

        val timesRaw = prefs[Keys.PERIOD_TIMES] ?: ""
        val savedTimes = if (timesRaw.isBlank()) emptyList() else timesRaw.split(",")
        val times = List(8) { index -> savedTimes.getOrElse(index) { "" } }

        val orderRaw = prefs[Keys.TAB_ORDER] ?: ""
        val savedOrder = if (orderRaw.isBlank()) defaultTabOrder else orderRaw.split(",")
        val order = savedOrder + defaultTabOrder.filter { it !in savedOrder }   // ← 保存済みの並びに無いIDを末尾に補完

        val breaksRaw = prefs[Keys.BREAKS] ?: ""
        val breaks = if (breaksRaw.isBlank()) emptyList() else breaksRaw.split(";").mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size == 3) {
                try {
                    BreakPeriod(parts[0], LocalDate.parse(parts[1]), LocalDate.parse(parts[2]))
                } catch (e: Exception) { null }
            } else null
        }

        AppSettings(
            maxPeriod = prefs[Keys.MAX_PERIOD] ?: 5,
            includeSaturday = prefs[Keys.INCLUDE_SATURDAY] ?: false,
            includeSunday = prefs[Keys.INCLUDE_SUNDAY] ?: false,
            periodTimes = times,
            tabOrder = order,
            breaks = breaks,

            lectureNotifyEnabled = prefs[Keys.LECTURE_NOTIFY_ENABLED] ?: true,
            lectureNotifyTime = prefs[Keys.LECTURE_NOTIFY_TIME] ?: "",
            taskNotifyEnabled = prefs[Keys.TASK_NOTIFY_ENABLED] ?: true,
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false
        )
    }

    suspend fun setMaxPeriod(value: Int) {
        context.settingsDataStore.edit { it[Keys.MAX_PERIOD] = value }
    }

    suspend fun setIncludeSaturday(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.INCLUDE_SATURDAY] = value }
    }

    suspend fun setIncludeSunday(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.INCLUDE_SUNDAY] = value }
    }

    suspend fun setPeriodTime(period: Int, time: String) {
        context.settingsDataStore.edit { prefs ->
            val timesRaw = prefs[Keys.PERIOD_TIMES] ?: ""
            val savedTimes = if (timesRaw.isBlank()) emptyList() else timesRaw.split(",")
            val times = MutableList(8) { index -> savedTimes.getOrElse(index) { "" } }

            times[period - 1] = time

            prefs[Keys.PERIOD_TIMES] = times.joinToString(",")
        }
    }

    suspend fun setTabOrder(order: List<String>) {
        context.settingsDataStore.edit { it[Keys.TAB_ORDER] = order.joinToString(",") }
    }

    suspend fun addBreak(name: String, start: LocalDate, end: LocalDate) {
        context.settingsDataStore.edit { prefs ->
            val raw = prefs[Keys.BREAKS] ?: ""
            val entries = (if (raw.isBlank()) listOf() else raw.split(";")).toMutableList()
            entries.add("$name,$start,$end")
            prefs[Keys.BREAKS] = entries.joinToString(";")
        }
    }

    suspend fun deleteBreak(breakPeriod: BreakPeriod) {
        context.settingsDataStore.edit { prefs ->
            val raw = prefs[Keys.BREAKS] ?: ""
            val entries = (if (raw.isBlank()) listOf() else raw.split(";")).toMutableList()
            entries.removeAll { it == "${breakPeriod.name},${breakPeriod.start},${breakPeriod.end}" }
            prefs[Keys.BREAKS] = entries.joinToString(";")
        }
    }

    suspend fun setLectureNotifyEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.LECTURE_NOTIFY_ENABLED] = value }
    }

    suspend fun setLectureNotifyTime(time: String) {
        context.settingsDataStore.edit { it[Keys.LECTURE_NOTIFY_TIME] = time }
    }

    suspend fun setTaskNotifyEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.TASK_NOTIFY_ENABLED] = value }
    }

    suspend fun setOnboardingDone() {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }
}