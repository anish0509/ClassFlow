package com.anish18.classflow.data.database.dao

import androidx.room.*
import com.anish18.classflow.data.model.Exam
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE semesterId = :semesterId ORDER BY examDate ASC, examTime ASC")
    fun getExamsForSemester(semesterId: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE semesterId = :semesterId AND isCompleted = 0 ORDER BY examDate ASC, examTime ASC")
    fun getPendingExamsForSemester(semesterId: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE examDate >= :todayDate ORDER BY examDate ASC, examTime ASC")
    fun getUpcomingExams(todayDate: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamById(id: Long): Exam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)
}
