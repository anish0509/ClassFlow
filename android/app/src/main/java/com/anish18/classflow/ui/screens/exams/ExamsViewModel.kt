package com.anish18.classflow.ui.screens.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Exam
import com.anish18.classflow.data.model.Semester
import com.anish18.classflow.data.repository.ExamRepository
import com.anish18.classflow.data.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExamsViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    val currentSemester: StateFlow<Semester?> = timetableRepository.activeSemesterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courses: StateFlow<List<Course>> = timetableRepository.activeCoursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filterType = MutableStateFlow("All") // "All", "Midterm", "Quiz", "Final"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    val exams: StateFlow<List<Exam>> = currentSemester.flatMapLatest { sem ->
        if (sem == null) flowOf(emptyList())
        else examRepository.getExamsForSemester(sem.id)
    }.combine(_filterType) { examList, filter ->
        if (filter == "All") examList
        else examList.filter { it.examType.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nearestUpcomingExam: StateFlow<Exam?> = exams.map { list ->
        val todayStr = LocalDate.now().toString()
        list.filter { !it.isCompleted && it.examDate >= todayStr }
            .minByOrNull { "${it.examDate} ${it.examTime}" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setFilterType(filter: String) {
        _filterType.value = filter
    }

    fun addExam(
        title: String,
        examType: String,
        courseId: String?,
        examDate: String,
        examTime: String,
        location: String?,
        notes: String?,
        reminderMinutes: Int
    ) {
        val sem = currentSemester.value ?: return
        viewModelScope.launch {
            val exam = Exam(
                title = title.trim(),
                examType = examType,
                courseId = courseId,
                examDate = examDate,
                examTime = examTime,
                location = location?.trim()?.ifEmpty { null },
                notes = notes?.trim()?.ifEmpty { null },
                semesterId = sem.id,
                reminderMinutesBefore = reminderMinutes
            )
            examRepository.addExam(exam)
        }
    }

    fun updateExam(exam: Exam) {
        viewModelScope.launch {
            examRepository.updateExam(exam)
        }
    }

    fun toggleExamCompleted(exam: Exam) {
        viewModelScope.launch {
            examRepository.updateExam(exam.copy(isCompleted = !exam.isCompleted))
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            examRepository.deleteExam(exam)
        }
    }
}
