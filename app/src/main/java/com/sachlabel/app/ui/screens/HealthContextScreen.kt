package com.sachlabel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.data.model.ProductScan
import com.sachlabel.app.data.model.UserContext
import com.sachlabel.app.health.HealthContextEngine
import com.sachlabel.app.ui.components.SachLabelHeader
import com.sachlabel.app.ui.theme.*

/**
 * Stitch Screen 8 — Secondary Health Context Screen
 * Strictly secondary, informational relevance only, non-dismissible mandatory disclaimer.
 */
@Composable
fun HealthContextScreen(
    scan: ProductScan?,
    onBack: () -> Unit
) {
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customInput by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<HealthContextEngine.HealthContextResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
    ) {
        // Sticky Header
        SachLabelHeader(
            title = "What This Means For You",
            subtitle = "Dietary Relevance Audit",
            showBackButton = true,
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SELECT YOUR DIETARY GOAL OR CONCERN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )

            // Preset chips row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(UserContext.PRESETS) { preset ->
                    val isSelected = selectedTags.contains(preset)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTags = if (isSelected) selectedTags - preset else selectedTags + preset
                            analysisResult = null
                        },
                        label = {
                            Text(
                                text = preset,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceContainerLowest
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryGreen else OutlineVariant.copy(alpha = 0.4f)
                        ),
                        shape = CircleShape
                    )
                }
            }

            // Free text input
            OutlinedTextField(
                value = customInput,
                onValueChange = {
                    customInput = it
                    analysisResult = null
                },
                placeholder = { Text("Or enter custom allergy, goal or ingredient...", fontSize = 13.sp, color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = OutlineVariant.copy(alpha = 0.4f),
                    focusedContainerColor = SurfaceContainerLowest,
                    unfocusedContainerColor = SurfaceContainerLowest,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 2
            )

            // Audit Button
            Button(
                onClick = {
                    val allTags = selectedTags + customInput.trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val label = scan?.structuredLabel
                    if (label != null && allTags.isNotEmpty()) {
                        analysisResult = HealthContextEngine.analyze(
                            label = label,
                            context = UserContext(tags = allTags.toList())
                        )
                    }
                },
                enabled = selectedTags.isNotEmpty() || customInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = PrimaryGreen.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    disabledContainerColor = SurfaceContainerLow
                )
            ) {
                Text("Check Ingredient Relevance", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Findings Section
            analysisResult?.let { result ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x0A151D1A), spotColor = Color(0x14151D1A))
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "STATUTORY INGREDIENT AUDIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            letterSpacing = 0.8.sp
                        )

                        Text(
                            text = result.note,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (result.relevantIngredients.isNotEmpty()) {
                            HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))
                            Text(
                                text = "Relevant ingredients found on back label:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            result.relevantIngredients.forEach { ingredient ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("•", color = CautionAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(ingredient, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // MANDATORY DISCLAIMER CARD — Non-dismissible, always rendered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CautionAmberLow)
                        .border(1.dp, CautionAmber.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CautionAmber,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "MANDATORY STATUTORY NOTICE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CautionAmber,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = UserContext.MANDATORY_DISCLAIMER,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
