package com.anish18.classflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anish18.classflow.data.repository.AppSettings
import com.anish18.classflow.data.repository.TimetableRepository
import com.anish18.classflow.ui.MainScreen
import com.anish18.classflow.ui.theme.UniTimetableTheme
import com.anish18.classflow.utils.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.core.view.WindowCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var repository: TimetableRepository

    private val destinationFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destinationFlow.value = intent.getStringExtra("destination")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        super.onCreate(savedInstanceState)
        
        destinationFlow.value = intent?.getStringExtra("destination")

        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }

        // Trigger widget update broadcast on app launch
        sendBroadcast(android.content.Intent(this, com.anish18.classflow.ui.widgets.ClassesWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this@MainActivity)
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this@MainActivity, com.anish18.classflow.ui.widgets.ClassesWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        })
        sendBroadcast(android.content.Intent(this, com.anish18.classflow.ui.widgets.TasksWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this@MainActivity)
            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this@MainActivity, com.anish18.classflow.ui.widgets.TasksWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        })

        setContent {
            val backgroundStyle by appSettings.backgroundStyle.collectAsState()
            val destination by destinationFlow.collectAsState()
            
            androidx.compose.runtime.LaunchedEffect(backgroundStyle) {
                com.anish18.classflow.ui.theme.ThemeState.isDark = (backgroundStyle == "Dark")
            }
            
            UniTimetableTheme {
                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.LocalOverscrollConfiguration provides null
                ) {
                    MainScreen(
                        backgroundStyle = backgroundStyle,
                        initialRoute = destination,
                        onRouteConsumed = { destinationFlow.value = null }
                    )
                }
            }
        }
    }
}
