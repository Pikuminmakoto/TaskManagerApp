package com.example.taskmanagerapp.ui

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.taskmanagerapp.data.Task
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    lectureViewModel: LectureViewModel,
    prefillLecture: String? = null,
    onPrefillHandled: () -> Unit = {}
) {

    val tasks by viewModel.allTasks.collectAsState()
    val lectures by lectureViewModel.allLectures.collectAsState()
    val lectureNames = remember(lectures) { lectures.map { it.name }.distinct() }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAddForm by remember { mutableStateOf(false) }
    var lectureText by remember { mutableStateOf("") }
    var assignmentText by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf(LocalDate.now()) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(prefillLecture) {
        if (!prefillLecture.isNullOrBlank()) {
            lectureText = prefillLecture
            assignmentText = ""
            deadlineDate = LocalDate.now()
            showAddForm = true
            onPrefillHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("課題一覧") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "課題を追加")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            AnimatedVisibility(visible = showAddForm) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { showAddForm = false }) {
                                Icon(Icons.Default.Close, contentDescription = "閉じる")
                            }
                        }

                        val suggestions = remember(lectureText, lectureNames) {
                            if (lectureText.isBlank()) emptyList()
                            else lectureNames.filter { it.contains(lectureText) && it != lectureText }.take(5)
                        }

                        OutlinedTextField(
                            value = lectureText,
                            onValueChange = { lectureText = it },
                            label = { Text("講義名") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (suggestions.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                Column {
                                    suggestions.forEach { name ->
                                        Text(
                                            name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { lectureText = name }
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = assignmentText,
                            onValueChange = { assignmentText = it },
                            label = { Text("課題名") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val d = deadlineDate
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        deadlineDate = LocalDate.of(year, month + 1, dayOfMonth)
                                    },
                                    d.year, d.monthValue - 1, d.dayOfMonth
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("締切日: $deadlineDate")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (lectureText.isNotBlank() && assignmentText.isNotBlank()) {
                                    viewModel.addTask(lectureText, assignmentText, deadlineDate)
                                    lectureText = ""
                                    assignmentText = ""
                                    deadlineDate = LocalDate.now()
                                    showAddForm = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("追加")
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("まだ課題がありません", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "右下の＋ボタンから追加しましょう",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onClick = { editingTask = task },
                            onToggleSubmitted = { viewModel.toggleSubmitted(task) },
                            onDelete = {
                                val deleted = task
                                viewModel.deleteTask(task)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "課題を削除しました",
                                        actionLabel = "元に戻す"
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreTask(deleted)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    editingTask?.let { task ->
        TaskEditDialog(
            task = task,
            lectureSuggestions = lectureNames,
            onDismiss = { editingTask = null },
            onConfirm = { lecture, assignment, deadline ->
                viewModel.updateTask(task, lecture, assignment, deadline)
                editingTask = null
            }
        )
    }
}

@Composable
fun TaskEditDialog(
    task: Task,
    lectureSuggestions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (lecture: String, assignment: String, deadline: LocalDate) -> Unit
) {
    val context = LocalContext.current

    var lectureText by remember { mutableStateOf(task.lecture) }
    var assignmentText by remember { mutableStateOf(task.assignment) }
    var deadlineDate by remember { mutableStateOf(task.deadline) }

    val suggestions = remember(lectureText) {
        if (lectureText.isBlank()) emptyList()
        else lectureSuggestions.filter { it.contains(lectureText) && it != lectureText }.take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("課題を編集") },
        text = {
            Column {
                OutlinedTextField(
                    value = lectureText,
                    onValueChange = { lectureText = it },
                    label = { Text("講義名") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestions.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Column {
                            suggestions.forEach { name ->
                                Text(
                                    name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { lectureText = name }
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = assignmentText,
                    onValueChange = { assignmentText = it },
                    label = { Text("課題名") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val d = deadlineDate
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                deadlineDate = LocalDate.of(year, month + 1, dayOfMonth)
                            },
                            d.year, d.monthValue - 1, d.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("締切日: $deadlineDate")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (lectureText.isNotBlank() && assignmentText.isNotBlank()) {
                    onConfirm(lectureText, assignmentText, deadlineDate)
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggleSubmitted: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = task.submitted,
                onCheckedChange = { onToggleSubmitted() }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(task.lecture, style = MaterialTheme.typography.labelMedium)
                Text(task.assignment, style = MaterialTheme.typography.bodyMedium)
                Text("締切: ${task.deadline}", style = MaterialTheme.typography.labelSmall)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除")
            }
        }
    }
}