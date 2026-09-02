package com.example.taskmanagerapp.data

import android.content.Context
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class WidgetData(
    val lectureCountToday: Int,
    val unsubmittedTaskCount: Int,
    val upcomingTasks: List<Task>,   // ← タイトルの配列からTaskそのものに変更
    val nextLectureInfo: String,
    val todayEventTitles: List<String>
)

object WidgetDataProvider {

    suspend fun load(context: Context): WidgetData {

        val db = AppDatabase.getDatabase(context)
        val settingsRepository = SettingsRepository(context)
        val settings = settingsRepository.settingsFlow.first()

        val today = LocalDate.now()
        val now = LocalTime.now()

        val todayJP = when (today.dayOfWeek) {
            DayOfWeek.MONDAY -> "月"
            DayOfWeek.TUESDAY -> "火"
            DayOfWeek.WEDNESDAY -> "水"
            DayOfWeek.THURSDAY -> "木"
            DayOfWeek.FRIDAY -> "金"
            DayOfWeek.SATURDAY -> "土"
            DayOfWeek.SUNDAY -> "日"
        }

        val tasks = db.taskDao().getAllTasksOnce()
        val unsubmittedTaskCount = tasks.count { !it.submitted }
        val upcomingTaskTitles = tasks
            .filter { !it.submitted }
            .sortedBy { it.deadline }
            .take(3)
            .map { it.assignment }
        val upcomingTasks = tasks
            .filter { !it.submitted }
            .sortedBy { it.deadline }
            .take(3)

        val events = db.eventDao().getAllEventsOnce()
        val todayEventTitles = events
            .filter { today in it.date..it.endDate }
            .map { it.title }

        val activeBreak = settings.breaks.find { today in it.start..it.end }

        var lectureCountToday = 0
        var nextLectureInfo = "今日の講義はありません"

        if (activeBreak == null) {

            val allLectures = db.lectureDao().getAllLecturesOnce()
            val notices = db.lectureNoticeDao().getAllNoticesOnce()
            val cancelledNames = notices
                .filter { it.date == today && it.type == "cancel" }
                .map { it.lectureName }
                .toSet()

            val todayLectures = allLectures
                .filter { it.day == todayJP }
                .filterNot { it.name in cancelledNames }
                .distinctBy { it.name to it.period }
                .sortedBy { it.period }

            lectureCountToday = todayLectures.size

            if (todayLectures.isEmpty()) {
                nextLectureInfo = "今日の講義はありません"
            } else {

                val upcoming = todayLectures.mapNotNull { lecture ->
                    val raw = settings.periodTimes.getOrNull(lecture.period - 1) ?: ""
                    val startStr = raw.split("~").getOrNull(0)
                    val start = startStr?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    if (start != null) lecture to start else null
                }.filter { (_, start) -> start.isAfter(now) }
                    .minByOrNull { (_, start) -> start }

                nextLectureInfo = if (upcoming != null) {
                    val (lecture, start) = upcoming
                    "次: ${lecture.period}限 ${start}〜"
                } else {
                    "本日の講義は終了しました"
                }
            }
        } else {
            nextLectureInfo = "${activeBreak.name}期間です"
        }

        return WidgetData(
            lectureCountToday = lectureCountToday,
            unsubmittedTaskCount = unsubmittedTaskCount,
            upcomingTasks = upcomingTasks,
            nextLectureInfo = nextLectureInfo,
            todayEventTitles = todayEventTitles
        )
    }
}