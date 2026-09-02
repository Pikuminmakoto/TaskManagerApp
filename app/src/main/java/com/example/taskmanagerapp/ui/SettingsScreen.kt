package com.example.taskmanagerapp.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar
import com.example.taskmanagerapp.data.BreakPeriod
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.HelpOutline
import kotlinx.coroutines.launch
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import com.example.taskmanagerapp.ui.TaskWidgetReceiver

private fun tabLabel(id: String): String = when (id) {
    "task" -> "課題"
    "lecture" -> "時間割"
    "todo" -> "ToDo"
    "calendar" -> "カレンダー"   // ← 追加
    else -> id
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            item {
                Text("表示する曜日", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("土曜日を表示")
                    Switch(
                        checked = settings.includeSaturday,
                        onCheckedChange = { viewModel.setIncludeSaturday(it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("日曜日を表示")
                    Switch(
                        checked = settings.includeSunday,
                        onCheckedChange = { viewModel.setIncludeSunday(it) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("時限の数", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (settings.maxPeriod > 1) {
                                viewModel.setMaxPeriod(settings.maxPeriod - 1)
                            }
                        }
                    ) { Text("−") }

                    Text("${settings.maxPeriod}限まで", style = MaterialTheme.typography.bodyLarge)

                    OutlinedButton(
                        onClick = {
                            if (settings.maxPeriod < 8) {
                                viewModel.setMaxPeriod(settings.maxPeriod + 1)
                            }
                        }
                    ) { Text("＋") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("時限の時間帯", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(settings.maxPeriod) { index ->
                val period = index + 1
                val context = LocalContext.current

                val raw = settings.periodTimes.getOrNull(index) ?: ""
                val parts = raw.split("~")
                val startTime = parts.getOrElse(0) { "" }
                val endTime = parts.getOrElse(1) { "" }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$period 限", modifier = Modifier.width(48.dp))

                    OutlinedButton(
                        onClick = {
                            showTimePickerDialog(context, startTime) { newStart ->
                                viewModel.setPeriodTime(period, "$newStart~$endTime")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startTime.ifBlank { "開始時刻" })
                    }

                    Text("〜", modifier = Modifier.padding(horizontal = 4.dp))

                    OutlinedButton(
                        onClick = {
                            showTimePickerDialog(context, endTime) { newEnd ->
                                viewModel.setPeriodTime(period, "$startTime~$newEnd")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(endTime.ifBlank { "終了時刻" })
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("タブの順番", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(settings.tabOrder) { tabId ->

                val index = settings.tabOrder.indexOf(tabId)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tabLabel(tabId), modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { viewModel.moveTab(tabId, -1) },
                        enabled = index > 0
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "上へ")
                    }

                    IconButton(
                        onClick = { viewModel.moveTab(tabId, 1) },
                        enabled = index < settings.tabOrder.size - 1
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "下へ")
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))

                var showBreakHelp by remember { mutableStateOf(false) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("長期休暇・試験期間", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showBreakHelp = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "説明", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showBreakHelp) {
                    AlertDialog(
                        onDismissRequest = { showBreakHelp = false },
                        title = { Text("長期休暇・試験期間について") },
                        text = {
                            Text(
                                "ここで登録した期間は、カレンダー・ダッシュボードで「講義」の代わりに期間名が表示され、講義の予定は表示されなくなります。夏休みや試験期間など、通常の時間割通りに講義が行われない期間の登録に使えます。"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showBreakHelp = false }) {
                                Text("閉じる")
                            }
                        }
                    )
                }
            }

            items(settings.breaks) { breakPeriod ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(breakPeriod.name, fontWeight = FontWeight.Bold)
                        Text("${breakPeriod.start} 〜 ${breakPeriod.end}", fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { viewModel.deleteBreak(breakPeriod) }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {

                var breakName by remember { mutableStateOf("") }
                var breakStart by remember { mutableStateOf<java.time.LocalDate?>(null) }
                var breakEnd by remember { mutableStateOf<java.time.LocalDate?>(null) }
                val context = LocalContext.current

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = breakName,
                    onValueChange = { breakName = it },
                    label = { Text("期間名(例: 夏季休暇、定期試験期間)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day -> breakStart = java.time.LocalDate.of(year, month + 1, day) },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(breakStart?.toString() ?: "開始日")
                    }

                    Text("〜", modifier = Modifier.padding(horizontal = 4.dp))

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day -> breakEnd = java.time.LocalDate.of(year, month + 1, day) },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(breakEnd?.toString() ?: "終了日")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val start = breakStart
                        val end = breakEnd
                        if (breakName.isNotBlank() && start != null && end != null && !end.isBefore(start)) {
                            viewModel.addBreak(breakName, start, end)
                            breakName = ""
                            breakStart = null
                            breakEnd = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("期間を追加")
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("通知", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val context = LocalContext.current

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("講義の通知")
                    Switch(
                        checked = settings.lectureNotifyEnabled,
                        onCheckedChange = { viewModel.setLectureNotifyEnabled(it) }
                    )
                }

                if (settings.lectureNotifyEnabled) {

                    val displayTime = com.example.taskmanagerapp.data.effectiveLectureNotifyTime(settings)

                    OutlinedButton(
                        onClick = {
                            showTimePickerDialog(context, displayTime) { newTime ->
                                viewModel.setLectureNotifyTime(newTime)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("通知時刻: $displayTime")
                    }

                    Text(
                        "1限の時間帯が設定されていれば、その1時間前が自動で初期値になります",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("課題の通知")
                    Switch(
                        checked = settings.taskNotifyEnabled,
                        onCheckedChange = { viewModel.setTaskNotifyEnabled(it) }
                    )
                }

                if (settings.taskNotifyEnabled) {
                    Text(
                        "締切前日の7:00に自動で通知されます",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            item {

                Spacer(modifier = Modifier.height(24.dp))
                Text("データのバックアップ", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val backupContext = LocalContext.current
                val backupScope = rememberCoroutineScope()
                var backupMessage by remember { mutableStateOf<String?>(null) }
                var showImportConfirm by remember { mutableStateOf(false) }
                var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        backupScope.launch {
                            com.example.taskmanagerapp.data.BackupManager.export(backupContext, uri)
                            backupMessage = "書き出しが完了しました"
                        }
                    }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        pendingImportUri = uri
                        showImportConfirm = true
                    }
                }

                Button(
                    onClick = { exportLauncher.launch("task_manager_backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("データを書き出す")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("データを読み込む")
                }

                backupMessage?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = Color.Gray, fontSize = 12.sp)
                }

                if (showImportConfirm) {
                    AlertDialog(
                        onDismissRequest = { showImportConfirm = false },
                        title = { Text("データを読み込みますか?") },
                        text = {
                            Text("現在アプリ内にあるすべてのデータが、選んだファイルの内容で上書きされます。この操作は元に戻せません。")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val uri = pendingImportUri
                                if (uri != null) {
                                    backupScope.launch {
                                        com.example.taskmanagerapp.data.BackupManager.import(backupContext, uri)
                                        backupMessage = "読み込みが完了しました。アプリを再起動してください"
                                    }
                                }
                                showImportConfirm = false
                            }) {
                                Text("読み込む", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportConfirm = false }) {
                                Text("キャンセル")
                            }
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("ホーム画面ウィジェット", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "今日の講義件数や未提出課題をホーム画面で確認できるウィジェットを追加できます。",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                val widgetContext = LocalContext.current

                Button(
                    onClick = {
                        val appWidgetManager = widgetContext.getSystemService(AppWidgetManager::class.java)
                        val provider = ComponentName(widgetContext, TaskWidgetReceiver::class.java)

                        if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                            appWidgetManager.requestPinAppWidget(provider, null, null)
                        } else {
                            Toast.makeText(
                                widgetContext,
                                "この端末では自動追加に対応していません。ホーム画面の長押し→ウィジェットから手動で追加してください。",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ウィジェットをホーム画面に追加")
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private fun showTimePickerDialog(
    context: android.content.Context,
    initialTime: String,
    onTimeSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    var hour = calendar.get(Calendar.HOUR_OF_DAY)
    var minute = calendar.get(Calendar.MINUTE)

    if (initialTime.contains(":")) {
        val split = initialTime.split(":")
        hour = split.getOrNull(0)?.toIntOrNull() ?: hour
        minute = split.getOrNull(1)?.toIntOrNull() ?: minute
    }

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected(String.format("%02d:%02d", selectedHour, selectedMinute))
        },
        hour,
        minute,
        true
    ).show()
}