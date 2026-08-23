package com.example.taskmanagerapp.data

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleLectureNotification(context: Context, time: String) {
        val request = OneTimeWorkRequestBuilder<LectureNotificationWorker>()
            .setInitialDelay(computeInitialDelay(time), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "lecture_notification",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleTaskNotification(context: Context) {
        val request = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(computeInitialDelay("07:00"), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "task_notification",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelLectureNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("lecture_notification")
    }

    fun cancelTaskNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("task_notification")
    }

    private fun computeInitialDelay(time: String): Long {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        return Duration.between(now, target).toMillis()
    }
}