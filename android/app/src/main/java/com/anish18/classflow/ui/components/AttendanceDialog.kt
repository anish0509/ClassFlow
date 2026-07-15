package com.anish18.classflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anish18.classflow.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import com.anish18.classflow.ui.components.AppTextField

@Composable
fun AttendanceDialog(
    currentStatus: String?,
    isFuture: Boolean,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onMarkAttendance: (status: String) -> Unit,
    onClearAttendance: () -> Unit,
    onShiftClick: () -> Unit
) {
    val drawDiagonalLineModifier = Modifier.drawWithContent {
        drawContent()
        drawLine(
            color = TextMuted.copy(alpha = 0.6f),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }

    GlassDialog(
        visible = visible,
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Mark Attendance",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    // 2x2 Capsule Pills Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1: Present & Absent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Present Pill
                            val presentBaseModifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(
                                    color = if (isFuture) CardBackground.copy(alpha = 0.15f) else if (currentStatus == "present") NeonGreen.copy(alpha = 0.15f) else CardBackground.copy(alpha = 0.5f), 
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isFuture) TextMuted.copy(alpha = 0.2f) else if (currentStatus == "present") NeonGreen else FrostedGlassBorder.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clip(RoundedCornerShape(24.dp))

                            Box(
                                modifier = if (isFuture) presentBaseModifier.then(drawDiagonalLineModifier) else presentBaseModifier.clickable {
                                    onMarkAttendance("present")
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isFuture) TextMuted.copy(alpha = 0.4f) else NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Present",
                                        color = if (isFuture) TextMuted.copy(alpha = 0.4f) else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Absent Pill
                            val absentBaseModifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(
                                    color = if (isFuture) CardBackground.copy(alpha = 0.15f) else if (currentStatus == "absent") NeonRed.copy(alpha = 0.15f) else CardBackground.copy(alpha = 0.5f), 
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isFuture) TextMuted.copy(alpha = 0.2f) else if (currentStatus == "absent") NeonRed else FrostedGlassBorder.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clip(RoundedCornerShape(24.dp))

                            Box(
                                modifier = if (isFuture) absentBaseModifier.then(drawDiagonalLineModifier) else absentBaseModifier.clickable {
                                    onMarkAttendance("absent")
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isFuture) TextMuted.copy(alpha = 0.4f) else NeonRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Absent",
                                        color = if (isFuture) TextMuted.copy(alpha = 0.4f) else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Row 2: Cancel & Shift
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(
                                        color = if (currentStatus == "canceled") NeonYellow.copy(alpha = 0.15f) else CardBackground.copy(alpha = 0.5f), 
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (currentStatus == "canceled") NeonYellow else FrostedGlassBorder.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable {
                                        onMarkAttendance("canceled")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = NeonYellow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cancel",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Shift Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .background(
                                        color = CardBackground.copy(alpha = 0.5f), 
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = FrostedGlassBorder.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable {
                                        onShiftClick()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = NeonPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Shift",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                    }
                }
            }
                    


                HorizontalDivider(color = CardBackground.copy(alpha = 0.2f))

                // Footer Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(46.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStatus != null) {
                        GlassDialogButton(
                            onClick = onClearAttendance,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("Clear", color = NeonRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                    
                    GlassDialogButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
