package com.example.taskmanagerapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.taskmanagerapp.data.Todo
import com.example.taskmanagerapp.data.TodoCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(viewModel: TodoViewModel) {

    val todos by viewModel.allTodos.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var todoText by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<TodoCategory?>(null) }
    var hideCompleted by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(false) }
    var showDeleteCompletedConfirm by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val categoryList = listOf(ALL_CATEGORY) + categories.map { it.name }
    val pagerState = rememberPagerState(
        initialPage = categoryList.indexOf(selectedCategory).coerceAtLeast(0)
    ) { categoryList.size }

    LaunchedEffect(pagerState.currentPage, categories) {
        val cat = categoryList.getOrNull(pagerState.currentPage) ?: ALL_CATEGORY
        if (cat != selectedCategory) {
            viewModel.selectCategory(cat)
        }
    }

    val incompleteCount = todos.count {
        !it.completed && (selectedCategory == ALL_CATEGORY || it.category == selectedCategory)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ToDo (未完了: ${incompleteCount}件)") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "ToDoを追加")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            // カテゴリタブ
            ScrollableTabRow(
                selectedTabIndex = categoryList.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedCategory == ALL_CATEGORY,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    text = { Text(ALL_CATEGORY) }
                )

                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategory == category.name,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index + 1) }
                        },
                        text = { Text(category.name) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showAddCategoryDialog = true }) {
                    Text("＋ タブ追加")
                }

                if (selectedCategory != ALL_CATEGORY && selectedCategory != "一般") {
                    TextButton(onClick = {
                        categories.find { it.name == selectedCategory }?.let {
                            categoryToDelete = it
                        }
                    }) {
                        Text("タブ削除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // 完了済み表示・並べ替え・一括削除の行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hideCompleted,
                        onCheckedChange = { hideCompleted = it }
                    )
                    Text("完了済みを非表示", style = MaterialTheme.typography.labelMedium)
                }

                Row {
                    TextButton(onClick = { sortMode = !sortMode }) {
                        Text(if (sortMode) "並べ替え終了" else "並べ替え")
                    }
                    TextButton(onClick = { showDeleteCompletedConfirm = true }) {
                        Text("完了済み削除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 常時表示だった入力欄 → カードに変更
            AnimatedVisibility(visible = showAddForm) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { showAddForm = false }) {
                                Icon(Icons.Default.Close, contentDescription = "閉じる")
                            }
                        }

                        OutlinedTextField(
                            value = todoText,
                            onValueChange = { todoText = it },
                            label = { Text("やること") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (todoText.isNotBlank()) {
                                    viewModel.addTodo(todoText)
                                    todoText = ""
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

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.weight(1f)
            ) { page ->

                val pageCategory = categoryList.getOrElse(page) { ALL_CATEGORY }

                val pageTodos = todos
                    .filter { pageCategory == ALL_CATEGORY || it.category == pageCategory }
                    .filter { !(hideCompleted && it.completed) }

                if (pageTodos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ToDoはありません", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "右下の＋ボタンから追加しましょう",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn {
                        items(pageTodos, key = { it.id }) { todo ->
                            TodoRow(
                                todo = todo,
                                sortMode = sortMode,
                                canMoveUp = pageTodos.indexOf(todo) > 0,
                                canMoveDown = pageTodos.indexOf(todo) < pageTodos.size - 1,
                                onClick = { editingTodo = todo },
                                onToggle = { viewModel.toggleCompleted(todo) },
                                onDelete = {
                                    val deleted = todo
                                    viewModel.deleteTodo(todo)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "ToDoを削除しました",
                                            actionLabel = "元に戻す"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreTodo(deleted)
                                        }
                                    }
                                },
                                onMoveUp = { viewModel.moveTodo(todo, -1) },
                                onMoveDown = { viewModel.moveTodo(todo, 1) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            existingNames = categories.map { it.name } + ALL_CATEGORY,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name)
                showAddCategoryDialog = false
            }
        )
    }

    categoryToDelete?.let { category ->

        val count = todos.count { it.category == category.name }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("${category.name} タブを削除しますか?") },
            text = {
                if (count > 0) {
                    Text("このタブにある${count}件のToDoは「一般」へ移動します。")
                } else {
                    Text("この操作は元に戻せません。")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category)
                    categoryToDelete = null
                }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    editingTodo?.let { todo ->

        var text by remember(todo.id) { mutableStateOf(todo.text) }

        AlertDialog(
            onDismissRequest = { editingTodo = null },
            title = { Text("ToDoを編集") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("やること") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) {
                        viewModel.updateTodoText(todo, text)
                        editingTodo = null
                    }
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTodo = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showDeleteCompletedConfirm) {

        val completedCount = todos.count { it.completed }

        AlertDialog(
            onDismissRequest = { showDeleteCompletedConfirm = false },
            title = { Text("完了済みタスクの削除") },
            text = {
                if (completedCount == 0) {
                    Text("完了済みタスクはありません")
                } else {
                    Text("${completedCount}件の完了済みタスクを削除しますか?\nこの操作は元に戻せません。")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (completedCount > 0) {
                            viewModel.deleteCompleted()
                        }
                        showDeleteCompletedConfirm = false
                    }
                ) {
                    Text(if (completedCount > 0) "削除" else "閉じる", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                if (completedCount > 0) {
                    TextButton(onClick = { showDeleteCompletedConfirm = false }) {
                        Text("キャンセル")
                    }
                }
            }
        )
    }
}

@Composable
fun AddCategoryDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ToDoタブ追加") },
        text = {
            Column {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        errorText = null
                    },
                    label = { Text("タブ名") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null
                )
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = nameText.trim()

                when {
                    trimmed.isEmpty() -> errorText = "タブ名を入力してください"
                    existingNames.contains(trimmed) -> errorText = "同じ名前のタブが存在します"
                    else -> onConfirm(trimmed)
                }
            }) {
                Text("追加")
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
fun TodoRow(
    todo: Todo,
    sortMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Checkbox(
            checked = todo.completed,
            onCheckedChange = { onToggle() },
            enabled = !sortMode
        )

        Text(
            text = todo.text,
            modifier = Modifier
                .weight(1f)
                .let { if (!sortMode) it.clickable(onClick = onClick) else it },
            textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None
        )

        if (sortMode) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "上へ")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "下へ")
            }
        } else {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "削除")
            }
        }
    }
}