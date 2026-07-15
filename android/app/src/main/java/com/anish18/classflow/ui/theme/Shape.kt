package com.anish18.classflow.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

// Premium Bento asymmetric shape: 32dp on top-left and bottom-right, 12dp on top-right and bottom-left
fun createAsymmetricShape(density: Float): Shape {
    val tl = 32.dp.value * density
    val tr = 12.dp.value * density
    val br = 32.dp.value * density
    val bl = 12.dp.value * density
    return GenericShape { size, _ ->
        addRoundRect(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(tl),
                topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(tr),
                bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(br),
                bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(bl)
            )
        )
    }
}
