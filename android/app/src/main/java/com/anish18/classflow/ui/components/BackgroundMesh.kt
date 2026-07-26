package com.anish18.classflow.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.anish18.classflow.ui.theme.ThemeState

@Composable
fun BackgroundMesh(
    modifier: Modifier = Modifier
) {
    val isDark = ThemeState.isDark
    val wallpaperType = ThemeState.wallpaperType
    val colorHex = ThemeState.wallpaperColorHex
    val gradientId = ThemeState.wallpaperGradientId
    val imageUri = ThemeState.wallpaperImageUri

    Box(modifier = modifier.fillMaxSize()) {
        when (wallpaperType) {
            "solid" -> {
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(colorHex))
                } catch (e: Exception) {
                    if (isDark) Color(0xFF0D1117) else Color(0xFFF2F4F7)
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = parsedColor)
                }
            }

            "gradient" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    when (gradientId) {
                        "cyberpunk" -> {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F0C20), Color(0xFF2B1055), Color(0xFF591A73)),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                        "sunset" -> {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF2D0B1E), Color(0xFF6B1D38), Color(0xFFA83248), Color(0xFFD97736)),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                        "deep_space" -> {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF05050A), Color(0xFF0F172A), Color(0xFF1E1B4B)),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                        "ocean" -> {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF021B2B), Color(0xFF004E64), Color(0xFF25A18E)),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                        "emerald" -> {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF062016), Color(0xFF0B4F37), Color(0xFF137547)),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                        else -> { // "aurora"
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF3A506B), Color(0xFF5BC0BE)),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                    }
                }
            }

            "custom_image" -> {
                val context = LocalContext.current
                val bitmap = remember(imageUri) {
                    if (imageUri.isNullOrEmpty()) null else {
                        try {
                            val uri = android.net.Uri.parse(imageUri)
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val opts = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                                inMutable = false
                            }
                            val b = BitmapFactory.decodeStream(inputStream, null, opts)
                            inputStream?.close()
                            b
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                }

                if (bitmap != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val imageWidth = bitmap.width.toFloat()
                        val imageHeight = bitmap.height.toFloat()
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Calculate Center-Crop bounds
                        val scale = maxOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
                        val scaledWidth = imageWidth * scale
                        val scaledHeight = imageHeight * scale
                        val left = (canvasWidth - scaledWidth) / 2f
                        val top = (canvasHeight - scaledHeight) / 2f

                        drawImage(
                            image = bitmap.asImageBitmap(),
                            dstOffset = IntOffset(left.toInt(), top.toInt()),
                            dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt())
                        )

                        // Contrast scrim overlay to keep all glass cards & navbar crisp
                        drawRect(
                            color = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.20f)
                        )
                    }
                } else {
                    DefaultMesh(isDark = isDark)
                }
            }

            else -> { // "default"
                DefaultMesh(isDark = isDark)
            }
        }
    }
}

@Composable
private fun DefaultMesh(isDark: Boolean) {
    if (isDark) {
        // Premium Dark Mode iOS AMOLED Black
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF000000))
        }
    } else {
        // Clean Light Mode Pastel Static Mesh
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFFF2F2F7))

            // Soft Pastel Blue orb
            val x1 = size.width * 0.25f
            val y1 = size.height * 0.25f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFCBE0FF).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(x1, y1),
                    radius = size.width * 0.95f
                ),
                center = Offset(x1, y1),
                radius = size.width * 0.95f
            )

            // Soft Lavender Purple orb
            val x2 = size.width * 0.75f
            val y2 = size.height * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEADFFF).copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(x2, y2),
                    radius = size.width * 1.1f
                ),
                center = Offset(x2, y2),
                radius = size.width * 1.1f
            )

            // Soft Rose Pink orb
            val x3 = size.width * 0.3f
            val y3 = size.height * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD5F0).copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(x3, y3),
                    radius = size.width * 0.85f
                ),
                center = Offset(x3, y3),
                radius = size.width * 0.85f
            )
        }
    }
}
