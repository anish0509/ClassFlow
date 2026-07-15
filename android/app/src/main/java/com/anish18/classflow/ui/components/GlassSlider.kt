package com.anish18.classflow.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anish18.classflow.ui.glass.GlassSlider
import com.anish18.classflow.ui.theme.NeonBlue

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonBlue,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val span = (max - min).coerceAtLeast(1f)

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        factory = { context ->
            GlassSlider(context).apply {
                setAccentColor(accentColor.toArgb())
                setMaxValue(span)
                setOnValueChangedListener { valNew ->
                    onValueChange(valNew + min)
                }
            }
        },
        update = { view ->
            view.setAccentColor(accentColor.toArgb())
            view.setMaxValue(span)
            val mappedValue = (value - min).coerceIn(0f, span)
            if (!view.isDragging() && view.getValue() != mappedValue) {
                view.setValue(mappedValue)
            }
        }
    )
}
