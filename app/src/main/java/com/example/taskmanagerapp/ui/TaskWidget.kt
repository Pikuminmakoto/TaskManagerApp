package com.example.taskmanagerapp.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Box
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.GlanceTheme
import androidx.glance.layout.height
import com.example.taskmanagerapp.data.WidgetData
import com.example.taskmanagerapp.data.WidgetDataProvider

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val data = WidgetDataProvider.load(context)

        provideContent {
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }
}

@Composable
private fun WidgetContent(data: WidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(Color(0xFFE0E4E8))
            .padding(10.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {

        // ヘッダー：件数
        Text(
            "本日の講義件数: ${data.lectureCountToday}件",
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color(0xFF243447))
            ),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        // 未提出課題ブロック（見出し + 枠で囲む）
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(8.dp)
                .background(Color(0xFFFDECEC))
                .padding(6.dp)
        ) {
            Text(
                "未提出課題 ${data.unsubmittedTaskCount}件",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFFC0392B))
                ),
                maxLines = 1
            )

            data.upcomingTasks.forEach { task ->
                Text(
                    "・${task.assignment}(${task.deadline})",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(Color(0xFFC0392B))
                    ),
                    maxLines = 1
                )
            }
        }

        if (data.todayEventTitles.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                "予定: " + data.todayEventTitles.joinToString(", "),
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(Color(0xFF27AE60))
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // 次の講義：強調表示
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(8.dp)
                .background(Color(0xFF4A90E2))
                .padding(vertical = 5.dp, horizontal = 6.dp)
        ) {
            Text(
                data.nextLectureInfo,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.White)
                ),
                maxLines = 1
            )
        }
    }
}

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}