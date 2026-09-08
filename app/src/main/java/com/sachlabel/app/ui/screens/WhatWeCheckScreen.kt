package com.sachlabel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.data.model.UserLanguage
import com.sachlabel.app.ui.components.SachLabelHeader
import com.sachlabel.app.ui.theme.*

/**
 * Stitch Screen 9 — Truth Standards / What We Check
 * Explicit bounded v1 taxonomy list (8 patterns) aligned with FSSAI regulations.
 */
@Composable
fun WhatWeCheckScreen(
    selectedLanguage: UserLanguage,
    onBack: () -> Unit,
    onLanguageClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
    ) {
        // Sticky Header
        SachLabelHeader(
            title = "Truth Standards",
            subtitle = "Defined Checklist • 8 Patterns",
            selectedLanguage = selectedLanguage,
            showBackButton = true,
            onBackClick = onBack,
            onLanguageClick = onLanguageClick
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryFixed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Rule, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "DEFINED SCOPE • NOT UNLIMITED AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "We check these specific front-label claims against statutory back-of-pack nutrition and ingredient tables. We don't guess arbitrary claims.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            itemsIndexed(TAXONOMY_ITEMS) { index, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x08151D1A), spotColor = Color(0x10151D1A))
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Number Badge
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(PrimaryFixed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.claimPattern,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.checkDescription,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                            Text("Why only these 8?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = "Adding a new claim type requires validating the verification logic against FSSAI food labeling regulations — not just writing an LLM prompt. We would rather show you our exact bounded list than pretend the system understands every possible claim.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

private data class TaxonomyItem(
    val claimPattern: String,
    val checkDescription: String
)

private val TAXONOMY_ITEMS = listOf(
    TaxonomyItem(
        claimPattern = "\"No Added Sugar\" / \"Sugar Free\" / \"Zero Sugar\"",
        checkDescription = "Checked against: nutrition table sugar value, and ingredient list for added-sugar keywords (glucose syrup, maltodextrin, dextrose, etc.)"
    ),
    TaxonomyItem(
        claimPattern = "\"100% Natural\" / \"100% Pure\"",
        checkDescription = "Checked against: fine print for qualifying disclaimers, and ingredient list for artificial/synthetic ingredients (artificial colour, flavour, preservatives)"
    ),
    TaxonomyItem(
        claimPattern = "\"No Preservatives\" / \"Preservative Free\"",
        checkDescription = "Checked against: ingredient list for preservative-class ingredients (sodium benzoate, potassium sorbate, INS 200–299 codes)"
    ),
    TaxonomyItem(
        claimPattern = "\"No Artificial Colors\"",
        checkDescription = "Checked against: ingredient list for synthetic color agents (tartrazine, sunset yellow, INS 100-series codes)"
    ),
    TaxonomyItem(
        claimPattern = "\"Organic\" / \"Certified Organic\"",
        checkDescription = "Checked for: presence of a recognized organic certification mark or certificate number on the label"
    ),
    TaxonomyItem(
        claimPattern = "\"High Protein\" / \"Protein Rich\"",
        checkDescription = "Checked against: nutrition table protein per 100g (≥20g threshold — verified against food labeling standards)"
    ),
    TaxonomyItem(
        claimPattern = "\"Zero Trans Fat\" / \"0g Trans Fat\"",
        checkDescription = "Checked against: ingredient list for partially hydrogenated oils (a trans fat source even when table claims 0g per serving)"
    ),
    TaxonomyItem(
        claimPattern = "\"Immunity Booster\" / Vague wellness claims",
        checkDescription = "Checked against: fine print for regulatory disclaimers, and ingredient list for ingredients associated with immune claims (Vitamin C, Zinc, etc.)"
    )
)
