# ProGuard rules for ClassFlow

# ─── Room / Hilt ─────────────────────────────────────────────────────────────
-keep class com.anish18.classflow.data.model.** { *; }
-keepclassmembers class com.anish18.classflow.data.model.** { *; }

# ─── Backup / Restore payload models (GSON serialised) ───────────────────────
-keep class com.anish18.classflow.ui.screens.settings.BackupMetadata { *; }
-keep class com.anish18.classflow.ui.screens.settings.MinifiedBackupData { *; }
-keep class com.anish18.classflow.ui.screens.settings.UnifiedBackupPayload { *; }
-keepclassmembers class com.anish18.classflow.ui.screens.settings.MinifiedBackupData { <fields>; }
-keepclassmembers class com.anish18.classflow.ui.screens.settings.UnifiedBackupPayload { <fields>; }
-keepclassmembers class com.anish18.classflow.ui.screens.settings.BackupMetadata { <fields>; }

# ─── BroadcastReceivers & Services (referenced by AlarmManager / AppWidgetManager) ──
# R8 must never rename or remove these — the system resolves them by class name.
-keep class com.anish18.classflow.utils.NotificationReceiver { *; }
-keep class com.anish18.classflow.utils.BootReceiver { *; }
-keep class com.anish18.classflow.utils.AlarmScheduler { *; }
-keep class com.anish18.classflow.ui.widgets.ClassesWidgetProvider { *; }
-keep class com.anish18.classflow.ui.widgets.ClassesWidgetService { *; }
-keep class com.anish18.classflow.ui.widgets.TasksWidgetProvider { *; }
-keep class com.anish18.classflow.ui.widgets.TasksWidgetService { *; }

# ─── GSON generic type token workaround ──────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ─── ZXing QR library ────────────────────────────────────────────────────────
-keep class com.google.zxing.** { *; }

# ─── Hilt generated components ───────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ─── Compose / Kotlin metadata (required for R8 full mode) ───────────────────
-keepattributes RuntimeVisibleAnnotations
-keepattributes InnerClasses
-keep class kotlin.Metadata { *; }
