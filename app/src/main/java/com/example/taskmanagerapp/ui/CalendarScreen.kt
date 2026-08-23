package com.example.taskmanagerapp.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.example.taskmanagerapp.data.Event
import com.example.taskmanagerapp.data.EventCategory
import com.example.taskmanagerapp.data.HolidayManager
import com.example.taskmanagerapp.data.Lecture
import com.example.taskmanagerapp.data.Task
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar

private val eventColorMap = mapOf(
    "青" to (Color(0xFFD9ECFF) to Color(0xFF2C6399)),
    "赤" to (Color(0xFFFFE0E0) to Color(0xFFB03A3A)),
    "黄" to (Color(0xFFFFF4BF) to Color(0xFF8A6D00)),
    "紫" to (Color(0xFFEEE2FF) to Color(0xFF6B4A9E)),
    "灰" to (Color(0xFFECEFF2) to Color(0xFF55606A)),
    "緑" to (Color(0xFFDFF5E1) to Color(0xFF2E7D32))
)

private val eventColorNames = listOf("青", "赤", "黄", "紫", "灰", "緑")

private fun japaneseDay(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> "月"
    DayOfWeek.TUESDAY -> "火"
    DayOfWeek.WEDNESDAY -> "水"
    DayOfWeek.THURSDAY -> "木"
    DayOfWeek.FRIDAY -> "金"
    DayOfWeek.SATURDAY -> "土"
    DayOfWeek.SUNDAY -> "日"
}

private fun buildMonthGrid(yearMonth: YearMonth): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    val leadingEmpty = firstOfMonth.dayOfWeek.value % 7
    val start = firstOfMonth.minusDays(leadingEmpty.toLong())
    return (0 until 42).map { start.plusDays(it.toLong()) }
}

private const val PAGE_COUNT = 2000
private const val CENTER_PAGE = PAGE_COUNT / 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    taskViewModel: TaskViewModel,
    lectureViewModel: LectureViewModel,
    settingsViewModel: SettingsViewModel
) {
    val events by viewModel.allEvents.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val tasks by taskViewModel.allTasks.collectAsState()
    val lectures by lectureViewModel.allLectures.collectAsState()
    val lectureNotices by lectureViewModel.allNotices.collectAsState()
    val selectedSemester by lectureViewModel.selectedSemester.collectAsState()

    val calSettings by settingsViewModel.settings.collectAsState()

    fun activeBreak(date: LocalDate) = calSettings.breaks.find { date in it.start..it.end }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var addEventDate by remember { mutableStateOf<LocalDate?>(null) }
    var titleQuery by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<EventCategory?>(null) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }


    val titleSuggestions = viewModel.suggestEvents(titleQuery)

    val baseMonth = remember { YearMonth.now() }
    val pagerState = rememberPagerState(initialPage = CENTER_PAGE) { PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()

    val displayedMonth = baseMonth.plusMonths((pagerState.currentPage - CENTER_PAGE).toLong())

    val showAll = selectedCategory == ALL_CATEGORY
    val showGeneral = showAll || selectedCategory == "一般"
    val today = LocalDate.now()

    fun eventsOn(date: LocalDate): List<Event> = events.filter {
        date in it.date..it.endDate &&
                (selectedCategory == ALL_CATEGORY || it.category == selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("カレンダー") })
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                ScrollableTabRow(
                    selectedTabIndex = (listOf(ALL_CATEGORY) + categories.map { it.name })
                        .indexOf(selectedCategory).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Tab(
                        selected = selectedCategory == ALL_CATEGORY,
                        onClick = { viewModel.selectCategory(ALL_CATEGORY) },
                        text = {
                            Text(
                                ALL_CATEGORY,
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategory == ALL_CATEGORY) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategory == ALL_CATEGORY)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    categories.forEach { category ->
                        Tab(
                            selected = selectedCategory == category.name,
                            onClick = { viewModel.selectCategory(category.name) },
                            text = {
                                Text(
                                    category.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedCategory == category.name) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategory == category.name)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                IconButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "タブ追加", modifier = Modifier.size(18.dp))
                }

                if (selectedCategory != ALL_CATEGORY && selectedCategory != "一般") {
                    IconButton(
                        onClick = {
                            categories.find { it.name == selectedCategory }?.let {
                                categoryToDelete = it
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "タブ削除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "前月", modifier = Modifier.size(18.dp))
                }

                Text(
                    "${displayedMonth.year}年${displayedMonth.monthValue}月",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "次月", modifier = Modifier.size(18.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                listOf("日", "月", "火", "水", "木", "金", "土").forEachIndexed { index, day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = when (index) {
                                0 -> Color(0xFFD63031)
                                6 -> Color(0xFF2C6399)
                                else -> Color.Unspecified
                            }
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->

                val monthForPage = baseMonth.plusMonths((page - CENTER_PAGE).toLong())
                val days = buildMonthGrid(monthForPage)

                Column(modifier = Modifier.fillMaxSize()) {
                    for (week in 0 until 6) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            for (col in 0 until 7) {

                                val date = days[week * 7 + col]
                                val inMonth = date.month == monthForPage.month
                                val isToday = date == today
                                val holidayName = HolidayManager.getHolidayName(date)

                                val dateColor = when {
                                    holidayName != null -> Color(0xFFD63031)
                                    col == 0 -> Color(0xFFD63031)
                                    col == 6 -> Color(0xFF2C6399)
                                    else -> Color(0xFF243447)
                                }

                                val dayEvents = eventsOn(date)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(1.dp)
                                        .background(
                                            if (isToday) Color(0xFFFFFBE6) else Color(0xFFFAFCFF)
                                        )
                                        .clickable { selectedDate = date }
                                        .padding(2.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isToday) Color(0xFF4A90E2) else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "${date.dayOfMonth}",
                                                    fontSize = 11.sp,
                                                    lineHeight = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isToday) Color.White else dateColor,
                                                    modifier = Modifier.alpha(if (inMonth) 1f else 0.35f)
                                                )
                                            }

                                            if (dayEvents.size > 2) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF9E9E9E)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "+${dayEvents.size - 2}",
                                                        fontSize = 7.sp,
                                                        lineHeight = 7.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        if (holidayName != null && showGeneral) {
                                            Text(
                                                holidayName,
                                                fontSize = 8.sp,
                                                color = Color(0xFFE67E22),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (showAll) {
                                            val taskCount = tasks.count { it.deadline == date }
                                            if (taskCount > 0) {
                                                Text("課題${taskCount}件", fontSize = 8.sp, color = Color(0xFFC0392B))
                                            }

                                            val activeBreak = activeBreak(date)

                                            if (activeBreak != null) {
                                                when (date) {
                                                    activeBreak.start -> Text(
                                                        "(${activeBreak.name}→)", fontSize = 7.sp, color = Color.Gray,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                                    )
                                                    activeBreak.end -> Text(
                                                        "(←${activeBreak.name})", fontSize = 7.sp, color = Color.Gray,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                                    )
                                                    else -> {}
                                                }
                                            } else {
                                                val lectureCount = lectures.count {
                                                    it.day == japaneseDay(date) && it.semester == selectedSemester
                                                }
                                                if (lectureCount > 0) {
                                                    Text("講義${lectureCount}件", fontSize = 8.sp, color = Color(0xFF2C6399))
                                                }
                                            }

                                            // 休講・補講の表示
                                            val cancelNotices = lectureNotices.filter { it.date == date && it.type == "cancel" }
                                            val makeupNotices = lectureNotices.filter { it.date == date && it.type == "makeup" }

                                            cancelNotices.forEach {
                                                Text(
                                                    "休講",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFFD63031),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }

                                            makeupNotices.forEach {
                                                val period = it.startTime   // 補講の時限が文字列で入っている
                                                Text(
                                                    "補講${period}限",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFFD63031),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        dayEvents.take(2).forEach { event ->
                                            val (bg, textColor) = eventColorMap[event.color]
                                                ?: (Color(0xFFD9ECFF) to Color(0xFF2C6399))

                                            val isMultiDay = event.endDate != event.date
                                            val isStart = date == event.date
                                            val isEnd = date == event.endDate

                                            if (event.displayStyle == "banner" || isMultiDay) {

                                                val shape = RoundedCornerShape(
                                                    topStart = if (isStart) 8.dp else 0.dp,
                                                    bottomStart = if (isStart) 8.dp else 0.dp,
                                                    topEnd = if (isEnd) 8.dp else 0.dp,
                                                    bottomEnd = if (isEnd) 8.dp else 0.dp
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .padding(vertical = 0.5.dp)
                                                        .clip(shape)
                                                        .background(bg)
                                                        .fillMaxWidth()
                                                    // .height(11.dp) ← この行を削除
                                                ) {
                                                    Text(
                                                        event.title,
                                                        fontSize = 7.sp,
                                                        lineHeight = 8.sp,   // ← 追加(行間を詰めつつ最低限の高さは確保)
                                                        color = textColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)   // ← vertical = 1.dp を追加
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    event.title,   // ← 「・」を削除
                                                    fontSize = 8.sp,
                                                    color = textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedDate?.let { date ->
        DayDetailDialog(
            date = date,
            holidayName = HolidayManager.getHolidayName(date),
            breakName = activeBreak(date)?.name,
            tasks = if (showAll) tasks.filter { it.deadline == date } else emptyList(),
            lectures = if (showAll && activeBreak(date) == null) {
                lectures.filter { it.day == japaneseDay(date) && it.semester == selectedSemester }
                    .sortedBy { it.period }
            } else emptyList(),
            events = eventsOn(date),
            onDismiss = { selectedDate = null },
            onAddClick = {
                addEventDate = date
                selectedDate = null
            },
            onDeleteEvent = { viewModel.deleteEvent(it) },
                    onEditEvent = { event ->
                editingEvent = event
                selectedDate = null
            }
        )
    }

    addEventDate?.let { date ->
        AddEventDialog(
            date = date,
            suggestions = titleSuggestions,
            onQueryChange = { titleQuery = it },
            onDismiss = {
                addEventDate = null
                titleQuery = ""
            },
            onConfirm = { title, endDate, color, start, end, style ->
                viewModel.addEvent(title, date, endDate, color, start, end, style)
                addEventDate = null
                titleQuery = ""
            }
        )
    }
    editingEvent?.let { event ->
        AddEventDialog(
            date = event.date,
            initialEvent = event,
            suggestions = emptyList(),
            onQueryChange = {},
            onDismiss = { editingEvent = null },
            onConfirm = { title, endDate, color, start, end, style ->
                viewModel.updateEvent(event, title, endDate, color, start, end, style)
                editingEvent = null
            }
        )
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

        val count = events.count { it.category == category.name }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("${category.name} タブを削除しますか?") },
            text = {
                if (count > 0) {
                    Text("このタブにある${count}件の予定は「一般」へ移動します。")
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
}

@Composable
fun DayDetailDialog(
    date: LocalDate,
    holidayName: String?,
    breakName: String?,   // ← 追加
    tasks: List<Task>,
    lectures: List<Lecture>,
    events: List<Event>,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onEditEvent: (Event) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$date${if (holidayName != null) "（$holidayName）" else ""}") },
        text = {
            Column {

                if (breakName != null) {
                    Text("${breakName}期間です。", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (tasks.isNotEmpty() || lectures.isNotEmpty()) {
                    tasks.forEach {
                        Text("📕 ${it.lecture} / ${it.assignment}", fontSize = 13.sp)
                    }
                    lectures.forEach {
                        Text("${it.period}限 ${it.name}", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text("予定", style = MaterialTheme.typography.labelMedium)

                if (events.isEmpty()) {
                    Text("予定はありません", fontSize = 12.sp, color = Color.Gray)
                } else {
                    events.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditEvent(event) },   // ← 追加
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("・${event.title}", fontSize = 13.sp)
                                // (既存のまま)
                            }
                            TextButton(onClick = { onDeleteEvent(event) }) {
                                Text("削除", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("予定を追加")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
fun AddEventDialog(
    date: LocalDate,
    initialEvent: Event? = null,   // ← 追加(編集時はここに元の予定を渡す)
    suggestions: List<Event>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        endDate: LocalDate,
        color: String,
        start: String,
        end: String,
        style: String
    ) -> Unit
) {
    val context = LocalContext.current

    var titleText by remember { mutableStateOf(initialEvent?.title ?: "") }
    var endDate by remember { mutableStateOf(initialEvent?.endDate ?: date) }
    var color by remember { mutableStateOf(initialEvent?.color ?: "青") }
    var startTime by remember { mutableStateOf(initialEvent?.startTime ?: "") }
    var endTime by remember { mutableStateOf(initialEvent?.endTime ?: "") }
    var style by remember { mutableStateOf(initialEvent?.displayStyle ?: "text") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent == null) "予定を追加" else "予定を編集") },
        text = {
            Column {

                Text("開始日: $date", style = MaterialTheme.typography.labelMedium)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = {
                        titleText = it
                        onQueryChange(it)
                    },
                    label = { Text("予定の内容") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column {
                            suggestions.forEach { s ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // 候補のすべての項目を反映する
                                            titleText = s.title
                                            color = s.color
                                            startTime = s.startTime
                                            endTime = s.endTime
                                            style = s.displayStyle

                                            // 継続日数だけ、選んだ開始日から終了日を計算する
                                            val duration = java.time.temporal.ChronoUnit.DAYS
                                                .between(s.date, s.endDate)
                                            endDate = date.plusDays(duration)

                                            onQueryChange("")
                                        }
                                        .padding(8.dp)
                                ) {
                                    Text(s.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                    val detail = buildList {
                                        if (s.startTime.isNotBlank() || s.endTime.isNotBlank()) {
                                            add("${s.startTime}〜${s.endTime}")
                                        }
                                        if (s.endDate != s.date) {
                                            add("${java.time.temporal.ChronoUnit.DAYS.between(s.date, s.endDate)}日間")
                                        }
                                        add(if (s.displayStyle == "banner") "帯表示" else "文字表示")
                                    }.joinToString(" / ")

                                    Text(detail, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.set(endDate.year, endDate.monthValue - 1, endDate.dayOfMonth)

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                endDate = LocalDate.of(year, month + 1, day)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("終了日: $endDate")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            showTimePickerDialog(context, startTime) { startTime = it }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startTime.ifBlank { "開始時刻" })
                    }
                    Text("〜", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedButton(
                        onClick = {
                            showTimePickerDialog(context, endTime) { endTime = it }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(endTime.ifBlank { "終了時刻" })
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("カラー", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    eventColorNames.forEach { name ->
                        val swatch = eventColorMap[name]?.second ?: Color.Gray

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (color == name) 3.dp else 1.dp,
                                    color = if (color == name) Color.Black else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { color = name }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (endDate == date) {
                    Text("表示方法", style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = style == "text",
                            onClick = { style = "text" },
                            label = { Text("文字のみ") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = style == "banner",
                            onClick = { style = "banner" },
                            label = { Text("帯で表示") }
                        )
                    }
                } else {
                    Text(
                        "複数日にわたる予定は自動的に帯で表示されます",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (titleText.isNotBlank() && !endDate.isBefore(date)) {
                    onConfirm(titleText, endDate, color, startTime, endTime, style)
                }
            }) {
                Text(if (initialEvent == null) "追加" else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
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