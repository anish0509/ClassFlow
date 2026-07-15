package com.anish18.classflow.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anish18.classflow.data.database.dao.*
import com.anish18.classflow.data.model.*

@Database(
    entities = [
        Semester::class,
        Course::class,
        ClassSession::class,
        Attendance::class,
        Task::class,
        CourseAttachment::class,
        Holiday::class
    ],
    version = 5,
    exportSchema = false
)
abstract class TimetableDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun courseDao(): CourseDao
    abstract fun classSessionDao(): ClassSessionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun taskDao(): TaskDao
    abstract fun holidayDao(): HolidayDao
}
