package com.example.taskmanagerapp.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate

class LectureNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        Log.d("TMWorker", "LectureNotificationWorker 開始")

        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val settingsRepository = SettingsRepository(applicationContext)
            val settings = settingsRepository.settingsFlow.first()

            Log.d("TMWorker", "設定取得完了: lectureNotifyEnabled=${settings.lectureNotifyEnabled}")

            if (settings.lectureNotifyEnabled) {

                val today = LocalDate.now()
                val activeBreak = settings.breaks.find { today in it.start..it.end }

                if (activeBreak == null) {

                    val todayJP = when (today.dayOfWeek) {
                        DayOfWeek.MONDAY -> "月"
                        DayOfWeek.TUESDAY -> "火"
                        DayOfWeek.WEDNESDAY -> "水"
                        DayOfWeek.THURSDAY -> "木"
                        DayOfWeek.FRIDAY -> "金"
                        DayOfWeek.SATURDAY -> "土"
                        DayOfWeek.SUNDAY -> "日"
                    }

                    val lectures = db.lectureDao().getAllLectures().first()
                        .filter { it.day == todayJP }
                        .distinctBy { it.name to it.period }

                    Log.d("TMWorker", "今日(${todayJP}曜)の講義数: ${lectures.size}")

                    val notices = db.lectureNoticeDao().getAllNotices().first()
                    val cancelledNames = notices
                        .filter { it.date == today && it.type == "cancel" }
                        .map { it.lectureName }
                        .toSet()

                    val activeLectures = lectures.filterNot { it.name in cancelledNames }

                    if (activeLectures.isNotEmpty()) {
                        val firstPeriod = activeLectures.minOf { it.period }
                        Log.d("TMWorker", "通知を表示します: ${firstPeriod}限から")
                        NotificationHelper.showLectureNotification(
                            applicationContext,
                            "今日の講義は${firstPeriod}限からです（${activeLectures.size}件）"
                        )
                    } else {
                        Log.d("TMWorker", "今日の講義が0件のため通知しません")
                    }
                } else {
                    Log.d("TMWorker", "長期休暇中のため通知しません")
                }
            }

            val time = effectiveLectureNotifyTime(settings)
            NotificationScheduler.scheduleLectureNotification(applicationContext, time)

            Log.d("TMWorker", "LectureNotificationWorker 正常終了")
            return Result.success()

        } catch (e: Exception) {
            Log.e("TMWorker", "LectureNotificationWorker でエラー発生", e)
            return Result.failure()
        }
    }
}