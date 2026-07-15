package com.anish18.classflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anish18.classflow.ui.theme.FrostedGlassBorder
import com.anish18.classflow.ui.theme.TextMuted
import com.anish18.classflow.ui.theme.TextPrimary
import com.anish18.classflow.ui.theme.ThemeState
import com.anish18.classflow.ui.theme.WaterBlue

/**
 * AppTextField — compact styled OutlinedTextField used throughout the app.
 *
 * - Height: 44dp (up from 40dp to prevent vertical clipping while keeping compact size)
 * - Input text: 13sp
 * - Placeholder text: 11sp
 * Uses BasicTextField + DecorationBox for custom padding to prevent vertical clipping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    shape: Shape = CircleShape,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    focusedBorderColor: Color = WaterBlue,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    val interactionSource = remember { MutableInteractionSource() }

    val compactPlaceholder: (@Composable () -> Unit)? = placeholder?.let { ph ->
        {
            CompositionLocalProvider(LocalTextStyle provides TextStyle(fontSize = 11.sp, color = TextMuted)) {
                ph()
            }
        }
    }

    val finalHeight = if (singleLine) 44.dp else androidx.compose.ui.unit.Dp.Unspecified
    val verticalPadding = if (singleLine) 0.dp else 12.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = if (singleLine) modifier.height(finalHeight) else modifier,
        textStyle = TextStyle(
            fontSize = fontSize,
            color = if (enabled) TextPrimary else TextMuted
        ),
        keyboardOptions = keyboardOptions,
        readOnly = readOnly,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(if (ThemeState.isDark) Color.White else WaterBlue),
        decorationBox = @Composable { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = compactPlaceholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = focusedBorderColor,
                    unfocusedBorderColor = FrostedGlassBorder.copy(alpha = if (ThemeState.isDark) 0.22f else 0.14f),
                    focusedContainerColor = if (ThemeState.isDark) Color.White.copy(alpha = 0.05f)
                                            else Color.Black.copy(alpha = 0.03f),
                    unfocusedContainerColor = if (ThemeState.isDark) Color.White.copy(alpha = 0.03f)
                                              else Color.Black.copy(alpha = 0.02f),
                ),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = verticalPadding
                ),
                container = {
                    OutlinedTextFieldDefaults.ContainerBox(
                        enabled = enabled,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = FrostedGlassBorder.copy(alpha = if (ThemeState.isDark) 0.22f else 0.14f),
                            focusedContainerColor = if (ThemeState.isDark) Color.White.copy(alpha = 0.05f)
                                                    else Color.Black.copy(alpha = 0.03f),
                            unfocusedContainerColor = if (ThemeState.isDark) Color.White.copy(alpha = 0.03f)
                                                      else Color.Black.copy(alpha = 0.02f),
                        ),
                        shape = shape
                    )
                }
            )
        }
    )
}
