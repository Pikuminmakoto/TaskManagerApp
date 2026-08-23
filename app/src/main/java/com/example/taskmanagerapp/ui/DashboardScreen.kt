package com.example.taskmanagerapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

private fun dashboardTodayDayJP(): String = when (LocalDate.now().dayOfWeek) {
    DayOfWeek.MONDAY -> "月"
    DayOfWeek.TUESDAY -> "火"
    DayOfWeek.WEDNESDAY -> "水"
    DayOfWeek.THURSDAY -> "木"
    DayOfWeek.FRIDAY -> "金"
    DayOfWeek.SATURDAY -> "土"
    DayOfWeek.SUNDAY -> "日"
}

@Composable
fun DashboardOverlay(
    taskViewModel: TaskViewModel,
    lectureViewModel: LectureViewModel,
    settingsViewModel: SettingsViewModel,   // ← 追加
    onDismiss: () -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val lectures by lectureViewModel.allLectures.collectAsState()
    val selectedSemester by lectureViewModel.selectedSemester.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val today = LocalDate.now()
    val todayDayJP = dashboardTodayDayJP()
    val todayBreak = settings.breaks.find { today in it.start..it.end }

    val unsubmitted = tasks.count { !it.submitted }
    val todayDeadline = tasks.count { it.deadline == today }
    val overdue = tasks.count { it.deadline.isBefore(today) && !it.submitted }

    val todayLectures = if (todayBreak == null) {
        lectures.filter { it.day == todayDayJP && it.semester == selectedSemester }
            .sortedBy { it.period }
    } else emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    "ダッシュボード",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                val (warningText, warningColor) = when {
                    overdue > 0 -> "⚠ 期限切れ課題が ${overdue} 件あります" to Color(0xFFD63031)
                    todayDeadline > 0 -> "⚠ 今日締切課題が ${todayDeadline} 件あります" to Color(0xFFE67E22)
                    else -> "課題の期限は問題ありません" to Color(0xFF27AE60)
                }

                Text(warningText, color = warningColor, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(12.dp))

                Text("未提出課題 : ${unsubmitted}件")
                Text("今日締切 : ${todayDeadline}件")
                Text("期限切れ : ${overdue}件")
                if (todayBreak != null) {
                    Text("今日の講義 : ${todayBreak.name}期間です", color = Color.Gray)
                } else {
                    Text("今日の講義 : ${todayLectures.size}件")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "今日の予定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (todayBreak != null) {
                    Text("${todayBreak.name}期間です。", color = Color.Gray)
                } else if (todayLectures.isEmpty()) {
                    Text("今日の講義はありません", color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(todayLectures) { lecture ->

                            val lectureTasks = tasks.filter { it.lecture == lecture.name }

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    "${lecture.period}限 ${lecture.name} ${lecture.room}",
                                    fontWeight = FontWeight.Bold
                                )
                                lectureTasks.forEach { task ->
                                    Text(
                                        "  └ ${task.assignment} / 締切: ${task.deadline}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "画面をタップすると閉じます",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}