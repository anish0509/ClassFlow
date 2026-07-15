package com.anish18.classflow.data.database.dao

import androidx.room.*
import com.anish18.classflow.data.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAllCoursesFlow(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name ASC")
    fun getCoursesBySemesterFlow(semesterId: String): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY name ASC")
    suspend fun getCoursesBySemester(semesterId: String): List<Course>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: String): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("SELECT * FROM course_attachments WHERE courseId = :courseId")
    fun getAttachmentsForCourseFlow(courseId: String): Flow<List<com.anish18.classflow.data.model.CourseAttachment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: com.anish18.classflow.data.model.CourseAttachment)

    @Delete
    suspend fun deleteAttachment(attachment: com.anish18.classflow.data.model.CourseAttachment)
}
