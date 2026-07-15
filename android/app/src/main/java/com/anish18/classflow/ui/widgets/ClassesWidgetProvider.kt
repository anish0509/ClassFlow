package com.anish18.classflow.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.room.Room
import com.anish18.classflow.MainActivity
import com.anish18.classflow.R
import com.anish18.classflow.data.database.TimetableDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate
import java.util.Locale
import android.app.WallpaperManager
import android.app.WallpaperColors
import android.os.Build
import android.graphics.Color

class ClassesWidgetProvider : AppWidgetProvider() {

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
                Log.e("ClassesWidget", "Error reading wallpaper colors", e)
            }
        }
        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags != android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("ClassesWidget", "onUpdate called for IDs: ${appWidgetIds.joinToString()}")
        val db = Room.databaseBuilder(
            context.applicationContext,
            TimetableDatabase::class.java,
            "timetable.db"
        )
        .fallbackToDestructiveMigration()
        .build()

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                updateWidgetData(context, appWidgetManager, appWidgetIds, db)
            } catch (e: Exception) {
                Log.e("ClassesWidget", "Error updating classes widget", e)
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
        val activeSemester = db.semesterDao().getActiveSemester()

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

        val todayClasses = if (activeSemester != null) {
            db.classSessionDao().getClassesBySemester(activeSemester.id)
                .filter { normalizeDay(it.dayOfWeek).equals(targetDayNormalized, ignoreCase = true) }
        } else {
            emptyList()
        }

        val isLight = isWallpaperLight(context)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.classes_widget)

            // Open app on root click
            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("destination", "home")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                101,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Setup Adapter Service
            val adapterIntent = Intent(context, ClassesWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("is_wallpaper_light", isLight)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.classes_list_view, adapterIntent)
            views.setEmptyView(R.id.classes_list_view, R.id.empty_state)

            // Dynamic Styling based on Wallpaper Color Hints
            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                if (isLight) R.drawable.widget_glass_background_light else R.drawable.widget_glass_background_dark
            )

            if (isLight) {
                views.setTextColor(R.id.widget_title, Color.parseColor("#E6000000"))
                views.setTextColor(R.id.widget_date_label, Color.parseColor("#80000000"))
                views.setTextColor(R.id.empty_state, Color.parseColor("#90000000"))
                views.setInt(R.id.widget_divider, "setBackgroundColor", Color.parseColor("#20000000"))
            } else {
                views.setTextColor(R.id.widget_title, Color.parseColor("#E02DD4BF"))
                views.setTextColor(R.id.widget_date_label, Color.parseColor("#99FFFFFF"))
                views.setTextColor(R.id.empty_state, Color.parseColor("#99FFFFFF"))
                views.setInt(R.id.widget_divider, "setBackgroundColor", Color.parseColor("#40FFFFFF"))
            }

            if (todayClasses.isEmpty()) {
                views.setViewVisibility(R.id.empty_state, View.VISIBLE)
                views.setViewVisibility(R.id.classes_list_view, View.GONE)
            } else {
                views.setViewVisibility(R.id.empty_state, View.GONE)
                views.setViewVisibility(R.id.classes_list_view, View.VISIBLE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.classes_list_view)
        }
    }
}
