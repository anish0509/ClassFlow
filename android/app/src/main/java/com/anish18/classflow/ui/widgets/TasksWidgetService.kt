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
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Task
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TasksRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TasksRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var db: TimetableDatabase? = null
    private var pendingTasks: List<Task> = emptyList()
    private var courseMap: Map<String, Course> = emptyMap()
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
            Log.e("TasksWidget", "Error closing database in factory", e)
        }
    }

    override fun getCount(): Int = pendingTasks.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= pendingTasks.size) {
            return RemoteViews(context.packageName, R.layout.widget_task_row)
        }

        val task = pendingTasks[position]
        val course = task.courseId?.let { courseMap[it] }
        val views = RemoteViews(context.packageName, R.layout.widget_task_row)

        views.setTextViewText(R.id.task_title, task.title)

        // Build meta text
        val courseName = course?.shortName ?: "General"
        val dueDateText = if (!task.dueDate.isNullOrEmpty()) {
            try {
                val parsedDate = LocalDate.parse(task.dueDate)
                val today = LocalDate.now()
                val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, parsedDate)
                val formatted = parsedDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
                when {
                    daysLeft < 0 -> " • Overdue – $formatted"
                    daysLeft == 0L -> " • Due today"
                    daysLeft == 1L -> " • Due tomorrow"
                    else -> " • Due $formatted"
                }
            } catch (e: Exception) {
                " • Due ${task.dueDate}"
            }
        } else ""
        views.setTextViewText(R.id.task_meta, "$courseName$dueDateText")

        // Dynamic row background based on wallpaper brightness
        views.setInt(
            R.id.row_container,
            "setBackgroundResource",
            if (isWallpaperLight) R.drawable.widget_row_background_light else R.drawable.widget_row_background_dark
        )

        // Dynamic text colors based on wallpaper brightness
        if (isWallpaperLight) {
            views.setTextColor(R.id.task_title, Color.parseColor("#E6000000"))
            views.setTextColor(R.id.task_meta, Color.parseColor("#80000000"))
        } else {
            views.setTextColor(R.id.task_title, Color.parseColor("#F5FFFFFF"))
            views.setTextColor(R.id.task_meta, Color.parseColor("#AAFFFFFF"))
        }

        // Color-code checkbox icon with course color
        val colorStr = course?.color ?: "#FFC084FC"
        try {
            views.setInt(R.id.task_checkbox, "setColorFilter", Color.parseColor(colorStr))
        } catch (e: Exception) {
            views.setInt(R.id.task_checkbox, "setColorFilter", Color.parseColor("#FFC084FC"))
        }

        // Fill-in intent so the provider can handle the tap
        val fillInIntent = Intent().apply {
            putExtra(TasksWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.task_checkbox, fillInIntent)
        views.setOnClickFillInIntent(R.id.task_title, fillInIntent)

        return views
    }

    override fun onDataSetChanged() {
        val database = db ?: return
        try {
            runBlocking {
                val activeSemester = database.semesterDao().getActiveSemester()
                courseMap = if (activeSemester != null) {
                    database.courseDao().getCoursesBySemester(activeSemester.id).associateBy { it.id }
                } else {
                    emptyMap()
                }
                pendingTasks = database.taskDao().getPendingTasks()
            }
        } catch (e: Exception) {
            Log.e("TasksWidget", "Error fetching tasks dataset in factory", e)
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
