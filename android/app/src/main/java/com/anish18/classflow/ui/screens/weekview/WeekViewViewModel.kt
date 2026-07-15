package com.anish18.classflow.ui.screens.weekview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Semester
import com.anish18.classflow.data.model.Task
import com.anish18.classflow.data.repository.AppSettings
import com.anish18.classflow.data.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WeekViewViewModel @Inject constructor(
    private val repository: TimetableRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    val activeSemester: StateFlow<Semester?> = repository.activeSemesterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courses: StateFlow<List<Course>> = repository.activeCoursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classes: StateFlow<List<ClassSession>> = repository.activeClassesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<Task>> = repository.allTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val holidays: StateFlow<List<com.anish18.classflow.data.model.Holiday>> = repository.allHolidaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showTasksOnTimetable: StateFlow<Boolean> = appSettings.showTasksOnTimetable
}
