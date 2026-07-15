package com.anish18.classflow.data.database.dao

import androidx.room.*
import com.anish18.classflow.data.model.Semester
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY name ASC")
    fun getAllSemestersFlow(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    fun getActiveSemesterFlow(): Flow<Semester?>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSemester(): Semester?

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getSemesterById(id: String): Semester?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: Semester)

    @Update
    suspend fun updateSemester(semester: Semester)

    @Delete
    suspend fun deleteSemester(semester: Semester)

    @Query("UPDATE semesters SET isActive = 0")
    suspend fun deactivateAllSemesters()

    @Transaction
    suspend fun setActiveSemester(semesterId: String) {
        deactivateAllSemesters()
        val semester = getSemesterById(semesterId)
        if (semester != null) {
            updateSemester(semester.copy(isActive = true))
        }
    }
}
