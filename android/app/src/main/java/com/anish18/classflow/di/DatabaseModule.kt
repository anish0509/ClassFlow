package com.anish18.classflow.di

import android.content.Context
import androidx.room.Room
import com.anish18.classflow.data.database.TimetableDatabase
import com.anish18.classflow.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TimetableDatabase {
        return Room.databaseBuilder(
            context,
            TimetableDatabase::class.java,
            "timetable.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideSemesterDao(db: TimetableDatabase): SemesterDao = db.semesterDao()

    @Provides
    fun provideCourseDao(db: TimetableDatabase): CourseDao = db.courseDao()

    @Provides
    fun provideClassSessionDao(db: TimetableDatabase): ClassSessionDao = db.classSessionDao()

    @Provides
    fun provideAttendanceDao(db: TimetableDatabase): AttendanceDao = db.attendanceDao()

    @Provides
    fun provideTaskDao(db: TimetableDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideHolidayDao(db: TimetableDatabase): HolidayDao = db.holidayDao()
}
