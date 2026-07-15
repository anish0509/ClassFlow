package com.anish18.classflow.data.database.dao

import androidx.room.*
import com.anish18.classflow.data.model.Attendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC, markedAt DESC")
    fun getAllAttendanceFlow(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE courseId = :courseId ORDER BY date DESC")
    fun getAttendanceForCourseFlow(courseId: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE courseId = :courseId ORDER BY date DESC")
    suspend fun getAttendanceForCourse(courseId: String): List<Attendance>

    @Query("SELECT * FROM attendance WHERE classId = :classId AND date = :date LIMIT 1")
    suspend fun getAttendanceForClassAndDate(classId: String, date: String): Attendance?

    @Query("SELECT * FROM attendance WHERE id = :id")
    suspend fun getAttendanceById(id: String): Attendance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE date = :date")
    suspend fun deleteAttendanceForDate(date: String)
}
