package com.example.taskmanagerapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskmanagerapp.data.Lecture
import com.example.taskmanagerapp.data.LectureRepository
import com.example.taskmanagerapp.data.Semester
import com.example.taskmanagerapp.data.SemesterRepository
import com.example.taskmanagerapp.data.LectureNotice
import com.example.taskmanagerapp.data.LectureNoticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate


class LectureViewModel(
    private val lectureRepository: LectureRepository,
    private val semesterRepository: SemesterRepository,
    private val noticeRepository: LectureNoticeRepository   // ← 追加
) : ViewModel() {

    val allLectures: StateFlow<List<Lecture>> = lectureRepository.allLectures.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allNotices: StateFlow<List<LectureNotice>> = noticeRepository.allNotices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addCancelNotice(lectureName: String, date: LocalDate, memo: String) {
        viewModelScope.launch {
            noticeRepository.insert(
                LectureNotice(lectureName = lectureName, date = date, type = "cancel", memo = memo)
            )
        }
    }

    fun addMakeupNotice(
        lectureName: String,
        date: LocalDate,
        makeupPeriod: Int,
        memo: String
    ) {
        viewModelScope.launch {
            noticeRepository.insert(
                LectureNotice(
                    lectureName = lectureName,
                    date = date,
                    type = "makeup",
                    originalDay = "",
                    startTime = makeupPeriod.toString(),
                    endTime = "",
                    memo = memo
                )
            )
        }
    }

    fun deleteNotice(notice: LectureNotice) {
        viewModelScope.launch {
            noticeRepository.delete(notice)
        }
    }

    val allSemesters: StateFlow<List<Semester>> = semesterRepository.allSemesters.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedSemester = MutableStateFlow("前期")
    val selectedSemester: StateFlow<String> = _selectedSemester.asStateFlow()

    fun selectSemester(name: String) {
        _selectedSemester.value = name
    }

    fun addSemester(name: String) {
        viewModelScope.launch {
            semesterRepository.insert(name)
        }
    }

    fun deleteSemester(semester: Semester) {
        viewModelScope.launch {
            semesterRepository.delete(semester)

            if (_selectedSemester.value == semester.name) {
                val remaining = allSemesters.value.firstOrNull { it.id != semester.id }
                _selectedSemester.value = remaining?.name ?: "前期"
            }
        }
    }

    fun addLecture(
        name: String,
        day: String,
        period: Int,
        room: String,
        color: String
    ) {
        viewModelScope.launch {
            lectureRepository.insert(
                Lecture(
                    name = name,
                    day = day,
                    period = period,
                    room = room,
                    semester = _selectedSemester.value,
                    color = color,
                    memo = ""
                )
            )
        }
    }

    fun deleteLecture(lecture: Lecture) {
        viewModelScope.launch {
            lectureRepository.delete(lecture)
            noticeRepository.deleteByLectureName(lecture.name)
        }
    }

    fun updateLecture(
        lecture: Lecture,
        name: String,
        room: String,
        color: String
    ) {
        viewModelScope.launch {
            lectureRepository.update(
                lecture.copy(
                    name = name,
                    room = room,
                    color = color
                )
            )
        }
    }

    fun updateMemo(lecture: Lecture, memo: String) {
        viewModelScope.launch {
            lectureRepository.update(lecture.copy(memo = memo))
        }
    }

    fun renameSemester(semester: Semester, newName: String) {
        viewModelScope.launch {
            semesterRepository.rename(semester, newName)
            if (_selectedSemester.value == semester.name) {
                _selectedSemester.value = newName
            }
        }
    }

    fun moveLecture(lecture: Lecture, newDay: String, newPeriod: Int) {
        viewModelScope.launch {
            lectureRepository.update(lecture.copy(day = newDay, period = newPeriod))
        }
    }

    class Factory(
        private val lectureRepository: LectureRepository,
        private val semesterRepository: SemesterRepository,
        private val noticeRepository: LectureNoticeRepository   // ← 追加
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LectureViewModel(lectureRepository, semesterRepository, noticeRepository) as T
        }
    }
}