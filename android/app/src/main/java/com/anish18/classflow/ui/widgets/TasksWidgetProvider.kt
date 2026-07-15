package com.anish18.classflow.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.room.Room
import com.anish18.classflow.MainActivity
import com.anish18.classflow.R
import com.anish18.classflow.data.database.TimetableDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate
import android.app.WallpaperManager
import android.app.WallpaperColors
import android.os.Build
import android.graphics.Color

class TasksWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_COMPLETE_TASK = "com.anish18.classflow.widgets.COMPLETE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private fun isWallpaperLight(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    val hints = colors.colorHints
                    return (hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
                }
            } catch (e: Exception) {
                Log.e("TasksWidget", "Error reading wallpaper colors", e)
            }
        }
        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags != android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_COMPLETE_TASK) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
            Log.d("TasksWidget", "Tapped to complete task ID: $taskId")

            val db = Room.databaseBuilder(
                context.applicationContext,
                TimetableDatabase::class.java,
                "timetable.db"
            )
            .fallbackToDestructiveMigration()
            .build()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val taskDao = db.taskDao()
                    val task = taskDao.getTaskById(taskId)
                    if (task != null) {
                        taskDao.updateTask(task.copy(status = "completed", completedAt = LocalDate.now().toString()))
                        Log.d("TasksWidget", "Task completed: ${task.title}")

                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Completed: ${task.title} 🎉", Toast.LENGTH_SHORT).show()
                        }

                        // Refresh all task widgets
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val thisWidget = ComponentName(context, TasksWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                        for (id in appWidgetIds) {
                            appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.tasks_list_view)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TasksWidget", "Error completing task from widget", e)
                } finally {
                    db.close()
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("TasksWidget", "onUpdate called for IDs: ${appWidgetIds.joinToString()}")
        val db = Room.databaseBuilder(
            context.applicationContext,
            TimetableDatabase::class.java,
            "timetable.db"
        )
        .fallbackToDestructiveMigration()
        .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetData(context, appWidgetManager, appWidgetIds, db)
            } catch (e: Exception) {
                Log.e("TasksWidget", "Error updating tasks widget", e)
            } finally {
                db.close()
            }
        }
    }

    private suspend fun updateWidgetData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        db: TimetableDatabase
    ) {
        val pendingTasks = db.taskDao().getPendingTasks()
        val isLight = isWallpaperLight(context)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.tasks_widget)

            // Open app on root click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("destination", "tasks")
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                201,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.empty_state, openAppPendingIntent)

            // Setup adapter service
            val adapterIntent = Intent(context, TasksWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("is_wallpaper_light", isLight)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.tasks_list_view, adapterIntent)
            views.setEmptyView(R.id.tasks_list_view, R.id.empty_state)

            // Pending intent template for complete-task taps
            val completeIntent = Intent(context, TasksWidgetProvider::class.java).apply {
                action = ACTION_COMPLETE_TASK
            }
            val completePendingTemplate = PendingIntent.getBroadcast(
                context,
                202,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.tasks_list_view, completePendingTemplate)

            // Dynamic Styling based on Wallpaper Color Hints
            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                if (isLight) R.drawable.widget_glass_background_light else R.drawable.widget_glass_background_dark
            )

            if (isLight) {
                views.setTextColor(R.id.widget_title, Color.parseColor("#E6000000"))
                views.setTextColor(R.id.empty_state, Color.parseColor("#90000000"))
                views.setInt(R.id.widget_divider, "setBackgroundColor", Color.parseColor("#20000000"))
            } else {
                views.setTextColor(R.id.widget_title, Color.parseColor("#E0A855F7"))
                views.setTextColor(R.id.empty_state, Color.parseColor("#99FFFFFF"))
                views.setInt(R.id.widget_divider, "setBackgroundColor", Color.parseColor("#40FFFFFF"))
            }

            if (pendingTasks.isEmpty()) {
                views.setViewVisibility(R.id.empty_state, View.VISIBLE)
                views.setViewVisibility(R.id.tasks_list_view, View.GONE)
            } else {
                views.setViewVisibility(R.id.empty_state, View.GONE)
                views.setViewVisibility(R.id.tasks_list_view, View.VISIBLE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.tasks_list_view)
        }
    }
}
