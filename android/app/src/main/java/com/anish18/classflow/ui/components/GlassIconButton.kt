package com.anish18.classflow.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anish18.classflow.ui.glass.GlassIconButton

@Composable
fun GlassIconButton(
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    AndroidView(
        modifier = modifier.size(size.dp),
        factory = { context ->
            GlassIconButton(context).apply {
                setIcon(iconRes)
                setOnClickListener { onClick() }
            }
        },
        update = { view ->
            view.setIcon(iconRes)
            view.setOnClickListener { onClick() }
        }
    )
}
