package com.example.taskmanagerapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Edit
import com.example.taskmanagerapp.data.Lecture
import java.time.DayOfWeek
import java.time.LocalDate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.HorizontalDivider

// 講義の色名 → (背景色, 枠線色)
private val lectureColorMap = mapOf(
    "青" to (Color(0xFFD9ECFF) to Color(0xFF78AFE8)),
    "赤" to (Color(0xFFFFE0E0) to Color(0xFFE89A9A)),
    "黄" to (Color(0xFFFFF4BF) to Color(0xFFE4CB67)),
    "紫" to (Color(0xFFEEE2FF) to Color(0xFFBCA0E8)),
    "灰" to (Color(0xFFECEFF2) to Color(0xFFAEB7C0))
)

private val lectureColorNames = listOf("青", "赤", "黄", "紫", "灰")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureListScreen(
    viewModel: LectureViewModel,
    settingsViewModel: SettingsViewModel,
    taskViewModel: TaskViewModel,
    onSettingsClick: () -> Unit,
    onAddTaskClick: (lectureName: String) -> Unit
) {

    val lectures by viewModel.allLectures.collectAsState()
    val tasks by taskViewModel.allTasks.collectAsState()
    val semesters by viewModel.allSemesters.collectAsState()
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    var addTargetDayPeriod by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var selectedLecture by remember { mutableStateOf<Lecture?>(null) }
    var editingLecture by remember { mutableStateOf<Lecture?>(null) }
    var noticeTargetLecture by remember { mutableStateOf<Lecture?>(null) }
    var showAddSemesterDialog by remember { mutableStateOf(false) }
    var semesterDropdownExpanded by remember { mutableStateOf(false) }
    var semesterToRename by remember { mutableStateOf<com.example.taskmanagerapp.data.Semester?>(null) }
    var deleteErrorMessage by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    var gridRowWidthPx by remember { mutableStateOf(0f) }
    var draggingLectureId by remember { mutableStateOf<Int?>(null) }
    val timetableScrollState = rememberScrollState()
    // remember { mutableStateOf(...) } ではなく、再描画を起こさない入れ物にする
    val dragOffsetHolder = remember { FloatArray(2) }
    var dragStartDay by remember { mutableStateOf("") }
    var dragStartPeriod by remember { mutableStateOf(0) }
    var moveRequest by remember { mutableStateOf<Triple<Lecture, String, Int>?>(null) }
    var moveConflictMessage by remember { mutableStateOf<String?>(null) }

    val visibleLectures = lectures.filter { it.semester == selectedSemester }

    val days = buildList {
        if (settings.includeSunday) add("日")
        addAll(listOf("月", "火", "水", "木", "金"))
        if (settings.includeSaturday) add("土")
    }

    val cellWidthPx = if (gridRowWidthPx > 0f)
        (gridRowWidthPx - with(density) { 36.dp.toPx() }) / days.size
    else 0f
    val cellHeightPx = with(density) { 84.dp.toPx() }

    val today = remember { todayDayJP() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("時間割") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(timetableScrollState, enabled = draggingLectureId == null)   // ← enabled を追加
        ) {

            // 学期選択行
            Row(verticalAlignment = Alignment.CenterVertically) {

                ExposedDropdownMenuBox(
                    expanded = semesterDropdownExpanded,
                    onExpandedChange = { semesterDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedSemester,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("表示学期", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = semesterDropdownExpanded,
                        onDismissRequest = { semesterDropdownExpanded = false }
                    ) {
                        semesters.forEach { semester ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(semester.name)
                                        IconButton(
                                            onClick = {
                                                semesterToRename = semester
                                                semesterDropdownExpanded = false
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "名前を変更", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectSemester(semester.name)
                                    semesterDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = { showAddSemesterDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "学期を追加")
                }

                IconButton(onClick = {
                    val target = semesters.find { it.name == selectedSemester }

                    when {
                        target == null -> Unit

                        semesters.size <= 1 -> {
                            deleteErrorMessage = "学期は最低1つ必要なため削除できません"
                        }

                        lectures.any { it.semester == selectedSemester } -> {
                            deleteErrorMessage = "この学期を使用している講義があるため削除できません。先に講義を削除するか、別の学期に変更してください。"
                        }

                        else -> {
                            viewModel.deleteSemester(target)
                        }
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "学期を削除")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 曜日の見出し行
            Row(modifier = Modifier.fillMaxWidth()) {

                Box(modifier = Modifier.width(36.dp))

                days.forEach { day ->
                    val weekendColor = when (day) {
                        "日" -> Color(0xFFD63031)
                        "土" -> Color(0xFF2C6399)
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (day == today) Color(0xFFFFF3B0) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                day == today -> Color(0xFF7A5A00)
                                weekendColor != null -> weekendColor
                                else -> Color.Unspecified
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // マス目
            for (period in 1..settings.maxPeriod) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .let {
                            if (period == 1) it.onGloballyPositioned { coords ->
                                gridRowWidthPx = coords.size.width.toFloat()
                            } else it
                        }
                ) {

                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                            .padding(start = 2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val raw = settings.periodTimes.getOrNull(period - 1) ?: ""
                        val parts = raw.split("~")
                        val startTime = parts.getOrElse(0) { "" }
                        val endTime = parts.getOrElse(1) { "" }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.widthIn(min = 30.dp)
                        ) {
                            if (startTime.isNotBlank()) {
                                Text(startTime, fontSize = 8.sp)
                            }

                            Text(
                                "$period",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )

                            if (endTime.isNotBlank()) {
                                Text(endTime, fontSize = 8.sp)
                            }
                        }
                    }

                    days.forEach { day ->

                        val lecture = visibleLectures.find {
                            it.day == day && it.period == period
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LectureCell(
                                day = day,
                                period = period,
                                lecture = lecture,
                                colorMap = lectureColorMap,
                                isToday = day == today,
                                isDragging = draggingLectureId != null && draggingLectureId == lecture?.id,
                                onEmptyClick = { addTargetDayPeriod = day to period },
                                onLectureTap = { selectedLecture = lecture },
                                onDragStart = {
                                    draggingLectureId = lecture?.id
                                    dragOffsetHolder[0] = 0f
                                    dragOffsetHolder[1] = 0f
                                    dragStartDay = day
                                    dragStartPeriod = period
                                },
                                onDrag = { dx, dy ->
                                    dragOffsetHolder[0] += dx
                                    dragOffsetHolder[1] += dy
                                },
                                onDragEnd = {
                                    if (cellWidthPx > 0f && lecture != null) {
                                        val colDelta = (dragOffsetHolder[0] / cellWidthPx).roundToInt()
                                        val rowDelta = (dragOffsetHolder[1] / cellHeightPx).roundToInt()

                                        val startCol = days.indexOf(dragStartDay)
                                        val targetCol = (startCol + colDelta).coerceIn(0, days.size - 1)
                                        val targetRow = (dragStartPeriod - 1 + rowDelta)
                                            .coerceIn(0, settings.maxPeriod - 1)

                                        val targetDay = days[targetCol]
                                        val targetPeriod = targetRow + 1

                                        if (targetDay != lecture.day || targetPeriod != lecture.period) {
                                            val conflict = visibleLectures.find {
                                                it.day == targetDay &&
                                                        it.period == targetPeriod &&
                                                        it.id != lecture.id
                                            }

                                            if (conflict != null) {
                                                moveConflictMessage =
                                                    "${targetDay}曜${targetPeriod}限には別の講義が登録されています"
                                            } else {
                                                moveRequest = Triple(lecture, targetDay, targetPeriod)
                                            }
                                        }
                                    }
                                    draggingLectureId = null
                                },
                                onDragCancel = { draggingLectureId = null }
                            )
                        }
                    }
                }
            }
        }
    }

    addTargetDayPeriod?.let { (day, period) ->
        LectureFormDialog(
            title = "${day}曜${period}限 講義登録",
            initialName = "",
            initialRoom = "",
            initialColor = "青",
            confirmLabel = "登録",
            onDismiss = { addTargetDayPeriod = null },
            onConfirm = { name, room, color ->
                viewModel.addLecture(
                    name = name,
                    day = day,
                    period = period,
                    room = room,
                    color = color
                )
                addTargetDayPeriod = null
            }
        )
    }

    editingLecture?.let { lecture ->
        LectureFormDialog(
            title = "${lecture.day}曜${lecture.period}限 講義編集",
            initialName = lecture.name,
            initialRoom = lecture.room,
            initialColor = lecture.color,
            confirmLabel = "更新",
            onDismiss = { editingLecture = null },
            onConfirm = { name, room, color ->
                viewModel.updateLecture(lecture, name, room, color)
                editingLecture = null
            }
        )
    }

    selectedLecture?.let { lecture ->
        LectureDetailDialog(
            lecture = lecture,
            onDismiss = { selectedLecture = null },
            onEdit = {
                editingLecture = lecture
                selectedLecture = null
            },
            onAddTask = {
                onAddTaskClick(lecture.name)
                selectedLecture = null
            },
            onSaveMemo = { memo ->
                viewModel.updateMemo(lecture, memo)
            },
            onManageNotice = {                     // ← 追加
                noticeTargetLecture = lecture
                selectedLecture = null
            },
            onDelete = {
                viewModel.deleteLecture(lecture)
                selectedLecture = null
            }
        )
    }

    semesterToRename?.let { semester ->

        var nameText by remember { mutableStateOf(semester.name) }

        AlertDialog(
            onDismissRequest = { semesterToRename = null },
            title = { Text("学期名を変更") },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("学期名") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nameText.isNotBlank()) {
                        viewModel.renameSemester(semester, nameText)
                        semesterToRename = null
                    }
                }) {
                    Text("変更")
                }
            },
            dismissButton = {
                TextButton(onClick = { semesterToRename = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showAddSemesterDialog) {
        AddSemesterDialog(
            onDismiss = { showAddSemesterDialog = false },
            onConfirm = { name ->
                viewModel.addSemester(name)
                showAddSemesterDialog = false
            }
        )
    }

    deleteErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteErrorMessage = null },
            title = { Text("削除できません") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { deleteErrorMessage = null }) {
                    Text("閉じる")
                }
            }
        )
    }

    moveRequest?.let { (lecture, newDay, newPeriod) ->
        AlertDialog(
            onDismissRequest = { moveRequest = null },
            title = { Text("講義移動") },
            text = { Text("${lecture.name} を ${newDay}曜${newPeriod}限へ移動しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.moveLecture(lecture, newDay, newPeriod)
                    moveRequest = null
                }) {
                    Text("移動")
                }
            },
            dismissButton = {
                TextButton(onClick = { moveRequest = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    moveConflictMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { moveConflictMessage = null },
            title = { Text("移動できません") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { moveConflictMessage = null }) {
                    Text("閉じる")
                }
            }
        )
    }

    noticeTargetLecture?.let { lecture ->

        val notices by viewModel.allNotices.collectAsState()
        val lectureNotices = notices.filter { it.lectureName == lecture.name }

        LectureNoticeDialog(
            lecture = lecture,
            notices = lectureNotices,
            onDismiss = { noticeTargetLecture = null },
            onAddCancel = { date, memo ->
                viewModel.addCancelNotice(lecture.name, date, memo)
                noticeTargetLecture = null
            },
            onAddMakeup = { date, makeupPeriod, memo ->
                viewModel.addMakeupNotice(lecture.name, date, makeupPeriod, memo)
                noticeTargetLecture = null
            },
            onDeleteNotice = { notice ->
                viewModel.deleteNotice(notice)
            }
        )
    }
}

@Composable
fun AddSemesterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("学期を追加") },
        text = {
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("学期名") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (nameText.isNotBlank()) {
                    onConfirm(nameText)
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
fun LectureFormDialog(
    title: String,
    initialName: String,
    initialRoom: String,
    initialColor: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, room: String, color: String) -> Unit
) {
    var nameText by remember { mutableStateOf(initialName) }
    var roomText by remember { mutableStateOf(initialRoom) }
    var colorText by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("講義名") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = roomText,
                    onValueChange = { roomText = it },
                    label = { Text("教室") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("カラー", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    lectureColorNames.forEach { name ->
                        val swatch = lectureColorMap[name]?.second ?: Color.Gray

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (colorText == name) 3.dp else 1.dp,
                                    color = if (colorText == name) Color.Black else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { colorText = name }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onConfirm(nameText, roomText, colorText)
                    }
                }
            ) {
                Text(confirmLabel)
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
fun LectureDetailDialog(
    lecture: Lecture,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAddTask: () -> Unit,
    onSaveMemo: (String) -> Unit,
    onManageNotice: () -> Unit,   // ← 追加
    onDelete: () -> Unit
) {
    var memoText by remember(lecture.id) { mutableStateOf(lecture.memo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(lecture.name)

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "編集")
                }
            }
        },
        text = {
            Column {
                Text("${lecture.day}曜 ${lecture.period}限")
                if (lecture.room.isNotBlank()) {
                    Text("教室: ${lecture.room}")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("メモ", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = memoText,
                    onValueChange = {
                        memoText = it
                        onSaveMemo(it)
                    },
                    placeholder = { Text("テスト範囲など大切なことをメモしておこう!") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onManageNotice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("休講・補講を登録")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("削除", color = Color.Red)
                }
                TextButton(onClick = onAddTask) {
                    Text("課題を追加")
                }
            }
        }
    )
}

@Composable
private fun LectureCell(
    day: String,
    period: Int,
    lecture: Lecture?,
    colorMap: Map<String, Pair<Color, Color>>,
    isToday: Boolean,
    isDragging: Boolean,
    onEmptyClick: () -> Unit,
    onLectureTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val colors = lecture?.let { colorMap[it.color] }

    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1f,
        animationSpec = tween(150),
        label = "cellScale"
    )
    var dragVisualOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(2.dp)
            .offset {                                    // ← 追加(指の動きに追従させる)
                IntOffset(
                    dragVisualOffset.x.roundToInt(),
                    dragVisualOffset.y.roundToInt()
                )
            }
            .scale(scale)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer(shadowElevation = if (isDragging) 12f else 0f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    colors != null -> colors.first
                    isToday -> Color(0xFFFFFBE6)
                    else -> Color(0xFFFAFCFF)
                }
            )
            .then(
                if (lecture != null) {
                    Modifier
                        .pointerInput(lecture.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragVisualOffset = Offset.Zero   // ← 追加
                                    onDragStart()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragVisualOffset += amount   // ← 追加(見た目の位置を更新)
                                    onDrag(amount.x, amount.y)
                                },
                                onDragEnd = {
                                    dragVisualOffset = Offset.Zero   // ← 追加(離したら位置をリセット)
                                    onDragEnd()
                                },
                                onDragCancel = {
                                    dragVisualOffset = Offset.Zero   // ← 追加
                                    onDragCancel()
                                }
                            )
                        }
                        .pointerInput(lecture.id) {
                            detectTapGestures(onTap = { onLectureTap() })
                        }
                } else {
                    Modifier.clickable(onClick = onEmptyClick)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (lecture != null) {
            Box(modifier = Modifier.fillMaxSize()) {

                Text(
                    lecture.name,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp, start = 4.dp, end = 4.dp)
                )

                if (lecture.room.isNotBlank()) {
                    Text(
                        lecture.room,
                        fontSize = 10.sp,
                        color = Color(0xFF4B5563),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun LectureNoticeDialog(
    lecture: Lecture,
    notices: List<com.example.taskmanagerapp.data.LectureNotice>,   // ← 追加
    onDismiss: () -> Unit,
    onAddCancel: (date: LocalDate, memo: String) -> Unit,
    onAddMakeup: (date: LocalDate, makeupPeriod: Int, memo: String) -> Unit,
    onDeleteNotice: (com.example.taskmanagerapp.data.LectureNotice) -> Unit   // ← 追加
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var mode by remember { mutableStateOf("cancel") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var makeupPeriod by remember { mutableStateOf(1) }
    var memo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${lecture.name} の休講・補講") },
        text = {
            Column {

                if (notices.isNotEmpty()) {
                    Text("登録済み", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    notices.sortedBy { it.date }.forEach { notice ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val label = if (notice.type == "cancel") {
                                "休講: ${notice.date}"
                            } else {
                                "補講: ${notice.date}（${notice.startTime}限）"
                            }

                            Text(label, fontSize = 13.sp)

                            TextButton(onClick = { onDeleteNotice(notice) }) {
                                Text("削除", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text("新規登録", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = mode == "cancel",
                        onClick = { mode = "cancel" },
                        label = { Text("休講") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == "makeup",
                        onClick = { mode = "makeup" },
                        label = { Text("補講") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val cal = java.util.Calendar.getInstance()
                        cal.set(date.year, date.monthValue - 1, date.dayOfMonth)

                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (mode == "cancel") "休講日: $date" else "補講実施日: $date"
                    )
                }

                if (mode == "makeup") {

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("元々の曜日: ${lecture.day}曜 ${lecture.period}限", style = MaterialTheme.typography.labelMedium)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("補講の時限", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = {
                            if (makeupPeriod > 1) makeupPeriod--
                        }) { Text("−") }

                        Text(
                            "${makeupPeriod}限",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        OutlinedButton(onClick = {
                            if (makeupPeriod < 8) makeupPeriod++
                        }) { Text("＋") }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ(任意)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (mode == "cancel") {
                    onAddCancel(date, memo)
                } else {
                    onAddMakeup(date, makeupPeriod, memo)
                }
            }) {
                Text("登録")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

private fun todayDayJP(): String {
    return when (LocalDate.now().dayOfWeek) {
        DayOfWeek.MONDAY -> "月"
        DayOfWeek.TUESDAY -> "火"
        DayOfWeek.WEDNESDAY -> "水"
        DayOfWeek.THURSDAY -> "木"
        DayOfWeek.FRIDAY -> "金"
        DayOfWeek.SATURDAY -> "土"
        DayOfWeek.SUNDAY -> "日"
    }
}