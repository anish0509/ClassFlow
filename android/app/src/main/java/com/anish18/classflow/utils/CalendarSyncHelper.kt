package com.anish18.classflow.utils

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Semester
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

object CalendarSyncHelper {

    private fun getCustomCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.NAME
        )
        val uri = CalendarContract.Calendars.CONTENT_URI
        try {
            context.contentResolver.query(uri, projection, "${CalendarContract.Calendars.NAME} = ?", arrayOf("ClassFlow Timetable"), null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If not found, create it!
        return createCustomCalendar(context)
    }

    private fun createCustomCalendar(context: Context): Long? {
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "ClassFlow")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "ClassFlow")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "ClassFlow Timetable")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "ClassFlow Timetable")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF00D2FF.toInt()) // Neon Blue tint
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "classflow@anish18.com")
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        
        return try {
            val resultUri = context.contentResolver.insert(uri, values)
            resultUri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearAllSyncedEvents(context: Context): Boolean {
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "ClassFlow")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val selection = "${CalendarContract.Calendars.NAME} = ?"
        val selectionArgs = arrayOf("ClassFlow Timetable")
        return try {
            val count = context.contentResolver.delete(uri, selection, selectionArgs)
            Log.d("CalendarSync", "Deleted custom calendar directory. Count: $count")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun syncTimetableToCalendar(
        context: Context,
        classes: List<ClassSession>,
        courses: Map<String, Course>,
        semester: Semester
    ): Boolean {
        // 1. Delete/clear any existing custom calendar first
        clearAllSyncedEvents(context)

        // 2. Resolve or create custom calendar ID
        val calendarId = getCustomCalendarId(context) ?: return false
        
        try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val semStartDate = LocalDate.parse(semester.startDate, dateFormatter)
            val semEndDate = LocalDate.parse(semester.endDate, dateFormatter)

            val dayMap = mapOf(
                "monday" to java.time.DayOfWeek.MONDAY,
                "tuesday" to java.time.DayOfWeek.TUESDAY,
                "wednesday" to java.time.DayOfWeek.WEDNESDAY,
                "thursday" to java.time.DayOfWeek.THURSDAY,
                "friday" to java.time.DayOfWeek.FRIDAY,
                "saturday" to java.time.DayOfWeek.SATURDAY,
                "sunday" to java.time.DayOfWeek.SUNDAY
            )

            val rruleDayMap = mapOf(
                "monday" to "MO",
                "tuesday" to "TU",
                "wednesday" to "WE",
                "thursday" to "TH",
                "friday" to "FR",
                "saturday" to "SA",
                "sunday" to "SU"
            )

            classes.forEach { session ->
                val course = courses[session.courseId] ?: return@forEach
                val targetDayOfWeek = dayMap[session.dayOfWeek.lowercase()] ?: return@forEach
                val rruleDay = rruleDayMap[session.dayOfWeek.lowercase()] ?: return@forEach

                // Find the first date of this weekday on or after the semester start date
                var firstClassDate = semStartDate
                while (firstClassDate.dayOfWeek != targetDayOfWeek) {
                    firstClassDate = firstClassDate.plusDays(1)
                }

                // Parse times
                val startParts = session.startTime.split(":")
                val endParts = session.endTime.split(":")
                if (startParts.size < 2 || endParts.size < 2) return@forEach
                
                val startHour = startParts[0].trim().toInt()
                val startMin = startParts[1].trim().toInt()
                val endHour = endParts[0].trim().toInt()
                val endMin = endParts[1].trim().toInt()

                val startDateTime = LocalDateTime.of(firstClassDate, LocalTime.of(startHour, startMin))
                val endDateTime = LocalDateTime.of(firstClassDate, LocalTime.of(endHour, endMin))

                val startMillis = startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Form RRule with UNTIL date formatted as yyyyMMdd'T'HHmmss'Z' (in UTC)
                val untilFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'235959'Z'")
                val untilStr = semEndDate.format(untilFormatter)
                val rrule = "FREQ=WEEKLY;UNTIL=$untilStr;BYDAY=$rruleDay"

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.TITLE, "${course.name} (${course.shortName})")
                    put(CalendarContract.Events.DESCRIPTION, "ClassFlowSync Course: ${course.name}\nProfessor: ${course.professor}\nRoom: ${session.room ?: course.room}")
                    put(CalendarContract.Events.EVENT_LOCATION, session.room ?: course.room)
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    put(CalendarContract.Events.RRULE, rrule)
                }

                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
