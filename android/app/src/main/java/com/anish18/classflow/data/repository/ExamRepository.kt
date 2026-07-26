package com.anish18.classflow.data.repository

import com.anish18.classflow.data.database.dao.ExamDao
import com.anish18.classflow.data.model.Exam
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val examDao: ExamDao
) {
    fun getExamsForSemester(semesterId: String): Flow<List<Exam>> {
        return examDao.getExamsForSemester(semesterId)
    }

    fun getPendingExamsForSemester(semesterId: String): Flow<List<Exam>> {
        return examDao.getPendingExamsForSemester(semesterId)
    }

    fun getUpcomingExams(todayDate: String): Flow<List<Exam>> {
        return examDao.getUpcomingExams(todayDate)
    }

    suspend fun getExamById(id: Long): Exam? {
        return examDao.getExamById(id)
    }

    suspend fun addExam(exam: Exam): Long {
        return examDao.insertExam(exam)
    }

    suspend fun updateExam(exam: Exam) {
        examDao.updateExam(exam)
    }

    suspend fun deleteExam(exam: Exam) {
        examDao.deleteExam(exam)
    }
}
