package com.anish18.classflow.data.database.dao

import androidx.room.*
import com.anish18.classflow.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, createdAt DESC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE courseId = :courseId ORDER BY dueDate ASC")
    fun getTasksForCourseFlow(courseId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE status = 'pending' ORDER BY dueDate ASC, createdAt DESC")
    suspend fun getPendingTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE courseId = :courseId ORDER BY dueDate ASC")
    suspend fun getTasksForCourse(courseId: String): List<Task>
}
