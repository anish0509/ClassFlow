package com.anish18.classflow.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Task
import com.anish18.classflow.data.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TimetableRepository
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.allTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courses: StateFlow<List<Course>> = repository.activeCoursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timerDuration = MutableStateFlow(25 * 60)
    val timerDuration = _timerDuration.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(25 * 60)
    val secondsRemaining = _secondsRemaining.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_secondsRemaining.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _secondsRemaining.value -= 1
            }
            if (_secondsRemaining.value == 0) {
                _isTimerRunning.value = false
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer(durationMinutes: Int = 25) {
        pauseTimer()
        _timerDuration.value = durationMinutes * 60
        _secondsRemaining.value = durationMinutes * 60
    }

    fun addTask(title: String, description: String?, courseId: String?, dueDate: String?, dueTime: String?, priority: String) {
        viewModelScope.launch {
            val newTask = Task(
                id = UUID.randomUUID().toString(),
                courseId = courseId,
                title = title,
                description = description,
                dueDate = dueDate,
                dueTime = dueTime,
                priority = priority,
                status = "pending",
                createdAt = Instant.now().toString()
            )
            repository.insertTask(newTask)
        }
    }

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            val updatedTask = if (task.status == "pending") {
                task.copy(status = "completed", completedAt = Instant.now().toString())
            } else {
                task.copy(status = "pending", completedAt = null)
            }
            repository.updateTask(updatedTask)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun updateTask(task: Task, title: String, courseId: String?, dueDate: String?, dueTime: String?) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                title = title,
                courseId = courseId,
                dueDate = dueDate,
                dueTime = dueTime
            )
            repository.updateTask(updatedTask)
        }
    }
}
