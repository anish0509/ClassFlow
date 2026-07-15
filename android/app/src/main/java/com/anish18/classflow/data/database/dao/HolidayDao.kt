package com.anish18.classflow.data.database.dao

import androidx.room.*
import com.anish18.classflow.data.model.Holiday
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidaysFlow(): Flow<List<Holiday>>

    @Query("SELECT * FROM holidays WHERE date = :date LIMIT 1")
    suspend fun getHolidayByDate(date: String): Holiday?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoliday(holiday: Holiday)

    @Delete
    suspend fun deleteHoliday(holiday: Holiday)
}
