package com.anish18.classflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * GlassHeader — Full-width frosted glass header bar matching iOS layout.
 *
 * Uses hazeChild for a clean background blur, giving a frosted appearance
 * without refraction artifacts from the Glass shader.
 *
 * @param title          The main title text.
 * @param subtitle       Optional subtitle text.
 * @param hazeState      The [HazeState] used to blur content scrolling underneath.
 * @param navigationIcon Optional navigation icon/button on the left.
 * @param actions        Optional row of action icon buttons on the right.
 */
@Composable
fun GlassHeader(
    title: String,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val isDark = ThemeState.isDark

    val headerBgColor = if (isDark) {
        CardBackground.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }

    val headerBorderColor = if (isDark) {
        FrostedGlassBorder.copy(alpha = 0.35f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .hazeChild(state = hazeState)
            .background(headerBgColor)
            .border(
                width = 0.8.dp,
                color = headerBorderColor
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}
