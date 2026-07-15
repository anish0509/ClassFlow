package com.anish18.classflow.ui.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.room.Room
import com.anish18.classflow.R
import com.anish18.classflow.data.database.TimetableDatabase
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class ClassesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ClassesRemoteViewsFactory(this.applicationContext, intent)
    }
}

class ClassesRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var db: TimetableDatabase? = null
    private var todayClasses: List<ClassSession> = emptyList()
    private var courses: Map<String, Course> = emptyMap()
    private val isWallpaperLight = intent.getBooleanExtra("is_wallpaper_light", false)

    override fun onCreate() {
        db = Room.databaseBuilder(
            context.applicationContext,
            TimetableDatabase::class.java,
            "timetable.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    override fun onDestroy() {
        try {
            db?.close()
        } catch (e: Exception) {
            Log.e("ClassesWidget", "Error closing database in factory", e)
        }
    }

    override fun getCount(): Int = todayClasses.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= todayClasses.size) {
            return RemoteViews(context.packageName, R.layout.widget_class_row)
        }

        val session = todayClasses[position]
        val course = courses[session.courseId]
        val views = RemoteViews(context.packageName, R.layout.widget_class_row)

        views.setTextViewText(R.id.class_title, course?.name ?: "Unknown Course")
        views.setTextViewText(R.id.class_time, "${session.startTime} – ${session.endTime}")

        val roomText = session.room ?: course?.room ?: ""
        views.setTextViewText(R.id.class_meta, if (roomText.isNotEmpty()) " • $roomText" else "")

        // Dynamic row background based on wallpaper brightness
        views.setInt(
            R.id.row_container,
            "setBackgroundResource",
            if (isWallpaperLight) R.drawable.widget_row_background_light else R.drawable.widget_row_background_dark
        )

        // Dynamic text colors based on wallpaper brightness
        if (isWallpaperLight) {
            views.setTextColor(R.id.class_title, Color.parseColor("#E6000000"))
            views.setTextColor(R.id.class_time, Color.parseColor("#80000000"))
            views.setTextColor(R.id.class_meta, Color.parseColor("#60000000"))
        } else {
            views.setTextColor(R.id.class_title, Color.parseColor("#F5FFFFFF"))
            views.setTextColor(R.id.class_time, Color.parseColor("#AAFFFFFF"))
            views.setTextColor(R.id.class_meta, Color.parseColor("#70FFFFFF"))
        }

        // Determine status from time
        val now = LocalTime.now()
        val start = parseLocalTime(session.startTime)
        val end = parseLocalTime(session.endTime)

        val statusText: String
        val labelColor: String
        val dotColor: String
        val baseColor = course?.color ?: "#FF2DD4BF"

        if (now.isAfter(end)) {
            statusText = "DONE"
            labelColor = if (isWallpaperLight) "#80000000" else "#80FFFFFF"
            dotColor = adjustAlpha(baseColor, 0.4f)
        } else if (!now.isBefore(start) && !now.isAfter(end)) {
            statusText = "LIVE"
            labelColor = baseColor
            dotColor = baseColor
        } else {
            statusText = "UPCOMING"
            labelColor = if (isWallpaperLight) "#9E000000" else "#B3FFFFFF"
            dotColor = baseColor
        }

        views.setTextViewText(R.id.class_status_label, statusText)
        views.setTextColor(R.id.class_status_label, Color.parseColor(labelColor))
        views.setInt(R.id.class_status_dot, "setBackgroundColor", Color.parseColor(dotColor))

        return views
    }

    private fun parseLocalTime(timeStr: String): LocalTime {
        return try {
            val parts = timeStr.split(":")
            val hour = parts[0].trim().toInt()
            val minute = parts[1].trim().toInt()
            LocalTime.of(hour, minute)
        } catch (e: Exception) {
            LocalTime.MIDNIGHT
        }
    }

    private fun adjustAlpha(hexColor: String, factor: Float): String {
        return try {
            val color = Color.parseColor(hexColor)
            val alpha = Math.round(Color.alpha(color) * factor)
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
        } catch (e: Exception) {
            "#80FFFFFF"
        }
    }

    override fun onDataSetChanged() {
        val database = db ?: return
        try {
            runBlocking {
                val activeSemester = database.semesterDao().getActiveSemester()
                courses = database.courseDao().getCoursesBySemester(activeSemester?.id ?: "").associateBy { it.id }

                val todayStr = LocalDate.now().dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)

                fun normalizeDay(d: String): String = when {
                    d.startsWith("MON", ignoreCase = true) -> "Monday"
                    d.startsWith("TUE", ignoreCase = true) -> "Tuesday"
                    d.startsWith("WED", ignoreCase = true) -> "Wednesday"
                    d.startsWith("THU", ignoreCase = true) -> "Thursday"
                    d.startsWith("FRI", ignoreCase = true) -> "Friday"
                    d.startsWith("SAT", ignoreCase = true) -> "Saturday"
                    d.startsWith("SUN", ignoreCase = true) -> "Sunday"
                    else -> d
                }

                val targetDayNormalized = normalizeDay(todayStr)

                todayClasses = if (activeSemester != null) {
                    database.classSessionDao().getClassesBySemester(activeSemester.id)
                        .filter { normalizeDay(it.dayOfWeek).equals(targetDayNormalized, ignoreCase = true) }
                        .sortedBy { it.startTime }
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("ClassesWidget", "Error fetching dataset in factory", e)
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
