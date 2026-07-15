package com.anish18.classflow.data.database.dao

import androidx.room.*
import com.anish18.classflow.data.model.ClassSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassSessionDao {
    @Query("SELECT * FROM classes ORDER BY startTime ASC")
    fun getAllClassesFlow(): Flow<List<ClassSession>>

    @Query("SELECT * FROM classes WHERE semesterId = :semesterId ORDER BY startTime ASC")
    fun getClassesBySemesterFlow(semesterId: String): Flow<List<ClassSession>>

    @Query("SELECT * FROM classes WHERE semesterId = :semesterId ORDER BY startTime ASC")
    suspend fun getClassesBySemester(semesterId: String): List<ClassSession>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: String): ClassSession?

    @Query("SELECT * FROM classes WHERE semesterId = :semesterId AND dayOfWeek = :day ORDER BY startTime ASC")
    fun getClassesForDayFlow(semesterId: String, day: String): Flow<List<ClassSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classSession: ClassSession)

    @Update
    suspend fun updateClass(classSession: ClassSession)

    @Delete
    suspend fun deleteClass(classSession: ClassSession)
}
