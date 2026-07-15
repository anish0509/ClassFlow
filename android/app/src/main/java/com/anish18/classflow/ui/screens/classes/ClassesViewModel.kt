package com.anish18.classflow.ui.screens.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Semester
import com.anish18.classflow.data.repository.AppSettings
import com.anish18.classflow.data.repository.TimetableRepository
import android.content.Context
import android.widget.Toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ClassesViewModel @Inject constructor(
    private val repository: TimetableRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    val activeSemester: StateFlow<Semester?> = repository.activeSemesterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courses: StateFlow<List<Course>> = repository.activeCoursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classes: StateFlow<List<ClassSession>> = repository.activeClassesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<com.anish18.classflow.data.model.Attendance>> = repository.allAttendanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Recently used room/venue strings, persisted across sessions (max 5). */
    val recentRooms: StateFlow<List<String>> = appSettings.recentRooms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCourse(name: String, shortName: String, professor: String, credits: Int, room: String, color: String) {
        viewModelScope.launch {
            val semesterId = repository.getActiveSemester()?.id ?: return@launch
            val newCourse = Course(
                id = UUID.randomUUID().toString(),
                name = name,
                shortName = shortName,
                professor = professor,
                credits = credits,
                room = room,
                color = color,
                semesterId = semesterId
            )
            repository.insertCourse(newCourse)
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            repository.updateCourse(course)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }

    fun addClassSession(courseId: String, dayOfWeek: String, startTime: String, endTime: String, room: String?) {
        viewModelScope.launch {
            val semesterId = repository.getActiveSemester()?.id ?: return@launch
            val clash = repository.checkClash(dayOfWeek, startTime, endTime, semesterId)
            if (clash != null) {
                _toastMessage.emit("Class Slot Clash: Overlaps with an existing class session!")
                return@launch
            }
            val newSession = ClassSession(
                id = UUID.randomUUID().toString(),
                courseId = courseId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room,
                semesterId = semesterId
            )
            repository.insertClass(newSession)
            // Persist the room to recent rooms
            if (!room.isNullOrBlank()) {
                appSettings.addRecentRoom(room)
            }
        }
    }

    fun deleteClassSession(session: ClassSession) {
        viewModelScope.launch {
            repository.deleteClass(session)
        }
    }
}
