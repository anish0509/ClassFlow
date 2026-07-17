package com.anish18.classflow.ui.screens.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.components.GlassCard
import com.anish18.classflow.ui.components.GlassHeader
import com.anish18.classflow.ui.components.LocalHazeState
import com.anish18.classflow.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.Image
import com.anish18.classflow.ui.glass.compose.GlassBox
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val isDark = ThemeState.isDark

    // Load App Icon safely as Bitmap to support Adaptive Icons without Compose crashes
    val appIconBitmap = remember(context) {
        try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    // Live Customizer parameters for the "Glass Lab" premium feature
    var blurRadius by remember { mutableStateOf(6f) }
    var displacement by remember { mutableStateOf(0.35f) }
    var thickness by remember { mutableStateOf(18f) }

    // Secret Tap Easter Egg state
    var logoTapCount by remember { mutableStateOf(0) }
    var isEasterEggUnlocked by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
            contentPadding = PaddingValues(top = 90.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Identity Header (Easter Egg click target)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Original App Launcher Icon (Safe Adaptive Icon Render)
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = "ClassFlow Logo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    logoTapCount++
                                    if (logoTapCount >= 5 && !isEasterEggUnlocked) {
                                        isEasterEggUnlocked = true
                                        Toast.makeText(context, "Premium Labs Unlocked! 🔮✨", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        )
                    } else {
                        // Fallback gradient box in case of loading issues
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(WaterBlue, NeonPurple)
                                    )
                                )
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    logoTapCount++
                                    if (logoTapCount >= 5 && !isEasterEggUnlocked) {
                                        isEasterEggUnlocked = true
                                        Toast.makeText(context, "Premium Labs Unlocked! 🔮✨", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ClassFlow",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )
                    Text(
                        text = "v1.0.0 (Release Build)",
                        fontSize = 13.sp,
                        color = if (isDark) Color.LightGray.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.6f)
                    )
                }
            }

            // General Info Cards
            item {
                AboutInfoCard(
                    icon = Icons.Default.Language,
                    iconTint = WaterBlue,
                    title = "Official website",
                    subtitle = "classflow.pages.dev",
                    blurRadius = blurRadius,
                    displacement = displacement,
                    thickness = thickness,
                    onClick = { openUrlHelper(context, "https://classflow.pages.dev") }
                )
            }

            item {
                AboutInfoCard(
                    icon = Icons.Default.Code,
                    iconTint = NeonGreen,
                    title = "Source code",
                    subtitle = "anish18/classflow-android",
                    blurRadius = blurRadius,
                    displacement = displacement,
                    thickness = thickness,
                    onClick = { openUrlHelper(context, "https://github.com/anish18/classflow-android") }
                )
            }

            // Developer Section
            item {
                Text(
                    text = "Developer",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.LightGray.copy(alpha = 0.7f) else Color.DarkGray.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                AboutInfoCard(
                    icon = Icons.Default.Person,
                    iconTint = NeonBlue,
                    title = "Anish Kumar",
                    subtitle = "Creator of ClassFlow",
                    blurRadius = blurRadius,
                    displacement = displacement,
                    thickness = thickness,
                    onClick = { openUrlHelper(context, "https://github.com/anish18") }
                )
            }

            item {
                AboutInfoCard(
                    icon = Icons.Default.AlternateEmail,
                    iconTint = NeonPink,
                    title = "Instagram",
                    subtitle = "@anish.___18__",
                    blurRadius = blurRadius,
                    displacement = displacement,
                    thickness = thickness,
                    onClick = { openUrlHelper(context, "https://www.instagram.com/anish.___18__?igsh=MXBmdGowbDFjdjV0cQ==") }
                )
            }

            item {
                AboutInfoCard(
                    icon = Icons.Default.Business,
                    iconTint = WaterBlue,
                    title = "LinkedIn",
                    subtitle = "anish-kumar-94331a324",
                    blurRadius = blurRadius,
                    displacement = displacement,
                    thickness = thickness,
                    onClick = { openUrlHelper(context, "https://www.linkedin.com/in/anish-kumar-94331a324?utm_source=share_via&utm_content=profile&utm_medium=member_android") }
                )
            }

            item {
                AboutInfoCard(
                    icon = Icons.Default.Mail,
                    iconTint = NeonOrange,
                    title = "Email",
                    subtitle = "anish.kmr0509@gmail.com",
                    blurRadius = blurRadius,
                    displacement = displacement,
                    thickness = thickness,
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:anish.kmr0509@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "ClassFlow Feedback")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Secret Easter Egg Labs: Premium Customizable Glass Lab
            if (isEasterEggUnlocked) {
                item {
                    Text(
                        text = "Premium Glass Lab (Experimental)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonPurple,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                    )
                }

                item {
                    val borderGradient = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.55f else 0.70f),
                            WaterBlue.copy(alpha = if (isDark) 0.30f else 0.25f),
                            Color.White.copy(alpha = if (isDark) 0.10f else 0.15f)
                        )
                    )
                    GlassBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = borderGradient,
                                shape = RoundedCornerShape(36.dp)
                            ),
                        cornerRadius = 36.dp,
                        blurRadius = blurRadius,
                        displacementScale = displacement,
                        thickness = thickness.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Tweak UI glass constants in real-time:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black
                            )

                            // Slider 1: Blur Radius
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Blur Radius", fontSize = 11.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
                                    Text(String.format("%.1f px", blurRadius), fontSize = 11.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = blurRadius,
                                    onValueChange = { blurRadius = it },
                                    valueRange = 1f..15f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonPurple,
                                        activeTrackColor = NeonPurple
                                    )
                                )
                            }

                            // Slider 2: Spec Displacement
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Displacement Scale", fontSize = 11.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
                                    Text(String.format("%.2f", displacement), fontSize = 11.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = displacement,
                                    onValueChange = { displacement = it },
                                    valueRange = 0.05f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonPurple,
                                        activeTrackColor = NeonPurple
                                    )
                                )
                            }

                            // Slider 3: Thickness
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Refractive Thickness", fontSize = 11.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
                                    Text(String.format("%.0f dp", thickness), fontSize = 11.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = thickness,
                                    onValueChange = { thickness = it },
                                    valueRange = 5f..40f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonPurple,
                                        activeTrackColor = NeonPurple
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Diagnostics Section
            item {
                Text(
                    text = "System Diagnostics",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.LightGray.copy(alpha = 0.7f) else Color.DarkGray.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                val borderGradient = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.55f else 0.70f),
                        WaterBlue.copy(alpha = if (isDark) 0.30f else 0.25f),
                        Color.White.copy(alpha = if (isDark) 0.10f else 0.15f)
                    )
                )
                GlassBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = borderGradient,
                            shape = RoundedCornerShape(36.dp)
                        ),
                    cornerRadius = 36.dp,
                    blurRadius = blurRadius,
                    displacementScale = displacement,
                    thickness = thickness.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticRow(label = "Device Model", value = "${Build.MANUFACTURER} ${Build.MODEL}", isDark = isDark)
                        DiagnosticRow(label = "Android SDK Level", value = "API ${Build.VERSION.SDK_INT}", isDark = isDark)
                        DiagnosticRow(label = "Theme Mode", value = if (isDark) "Dark Theme" else "Light Theme", isDark = isDark)
                        DiagnosticRow(label = "Render Engine", value = "Haze GLSL Shaders", isDark = isDark)
                    }
                }
            }

            // Footer App Info Description
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ClassFlow — your academic schedule companion, everywhere. Designed to keep your timetable, tasks, and attendance organized with premium glass aesthetics. Available on Android devices.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }

        // Back icon navigation & title
        GlassHeader(
            title = "About",
            hazeState = hazeState,
            navigationIcon = {
                com.anish18.classflow.ui.components.GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = if (isDark) Color.White else Color.Black
                )
            }
        )
    }
}
}

@Composable
private fun AboutInfoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    blurRadius: Float,
    displacement: Float,
    thickness: Float,
    onClick: () -> Unit
) {
    val isDark = ThemeState.isDark
    val borderGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.55f else 0.70f),
            WaterBlue.copy(alpha = if (isDark) 0.30f else 0.25f),
            Color.White.copy(alpha = if (isDark) 0.10f else 0.15f)
        )
    )

    GlassBox(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = borderGradient,
                shape = RoundedCornerShape(36.dp)
            ),
        cornerRadius = 36.dp,
        blurRadius = blurRadius,
        displacementScale = displacement,
        thickness = thickness.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isDark) Color.LightGray.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.6f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color.Black
        )
    }
}

private fun openUrlHelper(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
