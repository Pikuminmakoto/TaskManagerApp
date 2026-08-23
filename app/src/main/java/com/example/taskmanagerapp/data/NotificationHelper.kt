package com.example.taskmanagerapp.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val LECTURE_CHANNEL_ID = "lecture_channel"
    const val TASK_CHANNEL_ID = "task_channel"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                LECTURE_CHANNEL_ID,
                "講義通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "今日の講義をお知らせします" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                TASK_CHANNEL_ID,
                "課題通知",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "締切間近の課題をお知らせします" }
        )
    }

    fun showLectureNotification(context: Context, message: String) {
        show(context, LECTURE_CHANNEL_ID, 1001, "時間割", message)
    }

    fun showTaskNotification(context: Context, message: String) {
        show(context, TASK_CHANNEL_ID, 1002, "課題", message)
    }

    private fun show(context: Context, channelId: String, notificationId: Int, title: String, message: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}