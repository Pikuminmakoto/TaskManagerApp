package com.example.taskmanagerapp.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class TaskNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        val db = AppDatabase.getDatabase(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        val settings = settingsRepository.settingsFlow.first()

        if (settings.taskNotifyEnabled) {

            val tomorrow = LocalDate.now().plusDays(1)
            val tasks = db.taskDao().getAllTasks().first()
            val count = tasks.count { it.deadline == tomorrow && !it.submitted }

            if (count > 0) {
                NotificationHelper.showTaskNotification(
                    applicationContext,
                    "明日締切の課題が${count}件あります"
                )
            }
        }

        // 翌日分を再予約
        NotificationScheduler.scheduleTaskNotification(applicationContext)

        return Result.success()
    }
}