package com.sachlabel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.data.model.ClaimResult
import com.sachlabel.app.data.model.Evidence
import com.sachlabel.app.data.model.Verdict
import com.sachlabel.app.ui.theme.*

/**
 * Stitch signature Dual-Pack Claim Comparison card (Front Assertion vs Back Reality)
 */
@Composable
fun DualEvidenceCard(
    result: ClaimResult,
    explanationText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10151D1A), spotColor = Color(0x1A151D1A))
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Front Assertion Section
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Balance,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FRONT CLAIM ASSERTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\u201C${result.frontText.ifBlank { result.claim?.rawText ?: "No claim detected" }}\u201D",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Back Statutory Evidence Section
            if (result.evidence.sourceField != Evidence.SourceField.ABSENT && result.evidence.quote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (result.evidence.sourceField) {
                                    Evidence.SourceField.INGREDIENTS -> "STATUTORY INGREDIENTS LIST"
                                    Evidence.SourceField.NUTRITION_TABLE -> "NUTRITIONAL DECLARATION"
                                    Evidence.SourceField.FINE_PRINT -> "MANDATORY FINE PRINT"
                                    else -> "STATUTORY BACK REALITY"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (result.verdict == Verdict.MISLEADING) AlertCrimson else PrimaryGreen,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Statutory Pack Declaration",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = result.evidence.quote,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )

                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.25f))

                        Text(
                            text = "Matched Source: ${result.evidence.key}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Why This Matters (Plain-Language Explanation Callout)
            val calloutBg = when (result.verdict) {
                Verdict.MISLEADING -> AlertCrimsonLow
                Verdict.NEEDS_CONTEXT -> CautionAmberLow
                Verdict.CONSISTENT -> PrimaryFixed.copy(alpha = 0.35f)
                else -> SurfaceContainerLow
            }
            val calloutTint = when (result.verdict) {
                Verdict.MISLEADING -> AlertCrimson
                Verdict.NEEDS_CONTEXT -> CautionAmber
                Verdict.CONSISTENT -> PrimaryGreen
                else -> TextSecondary
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(calloutBg)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = calloutTint,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                    )
                    Column {
                        Text(
                            text = "WHY THIS MATTERS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = calloutTint,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = explanationText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
