package com.example.taskmanagerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.example.taskmanagerapp.ui.CalendarScreen
import com.example.taskmanagerapp.ui.CalendarViewModel
import com.example.taskmanagerapp.ui.DashboardOverlay
import com.example.taskmanagerapp.ui.LectureListScreen
import com.example.taskmanagerapp.ui.LectureViewModel
import com.example.taskmanagerapp.ui.SettingsScreen
import com.example.taskmanagerapp.ui.SettingsViewModel
import com.example.taskmanagerapp.ui.TaskListScreen
import com.example.taskmanagerapp.ui.TaskViewModel
import com.example.taskmanagerapp.ui.TodoListScreen
import com.example.taskmanagerapp.ui.TodoViewModel
import com.example.taskmanagerapp.ui.theme.TaskManagerAppTheme
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.taskmanagerapp.data.NotificationHelper
import com.example.taskmanagerapp.data.NotificationScheduler
import com.example.taskmanagerapp.data.effectiveLectureNotifyTime
import kotlinx.coroutines.flow.first

private data class TabInfo(val id: String, val label: String, val icon: ImageVector)

private val allTabs = listOf(
    TabInfo("task", "課題", Icons.Default.List),
    TabInfo("lecture", "時間割", Icons.Default.Schedule),
    TabInfo("todo", "ToDo", Icons.Default.Checklist),
    TabInfo("calendar", "カレンダー", Icons.Default.CalendarMonth)
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createChannels(this)

        val app = application as TaskManagerApplication

        setContent {
            TaskManagerAppTheme {

                val taskViewModel: TaskViewModel = viewModel(
                    factory = TaskViewModel.Factory(app.taskRepository)
                )

                val lectureViewModel: LectureViewModel = viewModel(
                    factory = LectureViewModel.Factory(app.lectureRepository, app.semesterRepository, app.lectureNoticeRepository)
                )

                val todoViewModel: TodoViewModel = viewModel(
                    factory = TodoViewModel.Factory(app.todoRepository, app.todoCategoryRepository)
                )

                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(app.settingsRepository)
                )

                val calendarViewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.Factory(app.eventRepository, app.eventCategoryRepository)
                )

                val settings by settingsViewModel.settings.collectAsState()

                val context = androidx.compose.ui.platform.LocalContext.current

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                LaunchedEffect(settings.lectureNotifyEnabled, settings.lectureNotifyTime, settings.periodTimes) {
                    if (settings.lectureNotifyEnabled) {
                        NotificationScheduler.scheduleLectureNotification(
                            context,
                            effectiveLectureNotifyTime(settings)
                        )
                    } else {
                        NotificationScheduler.cancelLectureNotification(context)
                    }
                }

                LaunchedEffect(settings.taskNotifyEnabled) {
                    if (settings.taskNotifyEnabled) {
                        NotificationScheduler.scheduleTaskNotification(context)
                    } else {
                        NotificationScheduler.cancelTaskNotification(context)
                    }
                }

                var selectedTab by remember { mutableStateOf("task") }
                var showSettings by remember { mutableStateOf(false) }
                var prefillLecture by remember { mutableStateOf<String?>(null) }
                var showDashboard by remember { mutableStateOf(true) }   // ← 追加
                var showOnboarding by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val actualSettings = app.settingsRepository.settingsFlow.first()
                    if (!actualSettings.onboardingDone) {
                        showOnboarding = true
                    }
                }

                val orderedTabs = settings.tabOrder
                    .mapNotNull { id -> allTabs.find { it.id == id } }
                    .let { it + allTabs.filter { tab -> tab !in it } }

                Box(modifier = Modifier.fillMaxSize()) {

                    if (showSettings) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { showSettings = false }
                        )
                    } else {
                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                NavigationBar {
                                    orderedTabs.forEach { tab ->
                                        NavigationBarItem(
                                            selected = selectedTab == tab.id,
                                            onClick = { selectedTab = tab.id },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }
                        ) { padding ->

                            Box(modifier = Modifier.padding(padding)) {
                                when (selectedTab) {
                                    "task" -> TaskListScreen(
                                        viewModel = taskViewModel,
                                        lectureViewModel = lectureViewModel,   // ← この行を追加
                                        prefillLecture = prefillLecture,
                                        onPrefillHandled = { prefillLecture = null }
                                    )
                                    "lecture" -> LectureListScreen(
                                        viewModel = lectureViewModel,
                                        settingsViewModel = settingsViewModel,
                                        taskViewModel = taskViewModel,
                                        onSettingsClick = { showSettings = true },
                                        onAddTaskClick = { lectureName ->
                                            prefillLecture = lectureName
                                            selectedTab = "task"
                                        }
                                    )
                                    "todo" -> TodoListScreen(viewModel = todoViewModel)
                                    "calendar" -> CalendarScreen(
                                        viewModel = calendarViewModel,
                                        taskViewModel = taskViewModel,
                                        lectureViewModel = lectureViewModel,
                                        settingsViewModel = settingsViewModel
                                    )
                                }
                            }
                        }
                    }

                    if (showDashboard) {
                        DashboardOverlay(
                            taskViewModel = taskViewModel,
                            lectureViewModel = lectureViewModel,
                            settingsViewModel = settingsViewModel,
                            onDismiss = { showDashboard = false }
                        )
                    }
                    if (showOnboarding) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text("このアプリの使い方") },
                            text = {
                                Column {
                                    Text("・下のタブで「課題」「時間割」「ToDo」「カレンダー」を切り替えられます")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("・右下の＋ボタンから、それぞれ新しい項目を追加できます")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("・時間割の右上の歯車から、細かい設定を変更できます")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("・起動時に表示されるこの画面は、今日の予定をまとめたダッシュボードです")
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    settingsViewModel.setOnboardingDone()
                                    showOnboarding = false
                                }) {
                                    Text("はじめる")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}