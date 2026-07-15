package com.anish18.classflow.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anish18.classflow.ui.glass.GlassSwitch

@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Capture initial value so factory can seed the thumb position instantly,
    // preventing a visible on→off flash when the dialog entry animation runs.
    val initialChecked = remember { checked }

    AndroidView(
        modifier = modifier.size(width = 64.dp, height = 28.dp),
        factory = { context ->
            GlassSwitch(context).apply {
                // Seed state without animation so the switch is correct from frame 1
                setOn(initialChecked, animate = false)
                setOnToggleChangedListener { on ->
                    onCheckedChange(on)
                }
            }
        },
        update = { view ->
            // Use animate = false: the spring is already driven by touch gestures
            // inside onTouchEvent. A programmatic setOn here with animate = true
            // would double-trigger the spring and cause the thumb to bounce.
            if (view.isOn() != checked) {
                view.setOn(checked, animate = false)
            }
        }
    )
}
