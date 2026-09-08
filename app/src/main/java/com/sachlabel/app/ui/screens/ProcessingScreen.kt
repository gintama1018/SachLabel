package com.sachlabel.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.sachlabel.app.R
import com.sachlabel.app.ui.theme.*
import com.sachlabel.app.viewmodel.ScanViewModel

/**
 * Stitch Screen 6 — Processing Screen
 * Calming, step-by-step progress feedback with Stitch visual polish.
 */
@Composable
fun ProcessingScreen(step: ScanViewModel.ProcessingStep) {
    val steps = ScanViewModel.ProcessingStep.entries.toList()
    val currentIndex = steps.indexOf(step)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Pulsing scanner icon container
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10151D1A), spotColor = Color(0x20151D1A))
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.5.dp, PrimaryFixed, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = PrimaryGreen,
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.processing_auditing_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = stringResource(R.string.processing_auditing_sub),
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Step List Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x0A151D1A), spotColor = Color(0x14151D1A))
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    steps.forEachIndexed { index, stepItem ->
                        val isComplete = index < currentIndex
                        val isCurrent = index == currentIndex

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isComplete -> PrimaryGreen
                                            isCurrent -> PrimaryFixed
                                            else -> SurfaceContainerLow
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isComplete) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryGreen)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = stringResource(stepItem.labelResId),
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) TextPrimary else if (isComplete) PrimaryGreen else TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.processing_privacy_footer),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}
