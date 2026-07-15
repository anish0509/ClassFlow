package com.anish18.classflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.anish18.classflow.ui.theme.ThemeState

@Composable
fun BackgroundMesh(
    modifier: Modifier = Modifier
) {
    val isDark = ThemeState.isDark

    if (isDark) {
        // ── Premium Dark Mode iOS AMOLED Black ─────────────────────────────
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF000000))
        }
    } else {
        // ── Clean Light Mode Pastel Static Mesh ─────────────────────────────────
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(color = Color(0xFFF2F2F7))

            // Soft Pastel Blue orb
            val x1 = size.width * 0.25f
            val y1 = size.height * 0.25f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFCBE0FF).copy(alpha = 0.35f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(x1, y1),
                    radius = size.width * 0.95f
                ),
                center = androidx.compose.ui.geometry.Offset(x1, y1),
                radius = size.width * 0.95f
            )

            // Soft Lavender Purple orb
            val x2 = size.width * 0.75f
            val y2 = size.height * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEADFFF).copy(alpha = 0.38f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(x2, y2),
                    radius = size.width * 1.1f
                ),
                center = androidx.compose.ui.geometry.Offset(x2, y2),
                radius = size.width * 1.1f
            )

            // Soft Rose Pink orb
            val x3 = size.width * 0.3f
            val y3 = size.height * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD5F0).copy(alpha = 0.32f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(x3, y3),
                    radius = size.width * 0.85f
                ),
                center = androidx.compose.ui.geometry.Offset(x3, y3),
                radius = size.width * 0.85f
            )
        }
    }
}
