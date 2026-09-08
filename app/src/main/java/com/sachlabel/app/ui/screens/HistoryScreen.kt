package com.sachlabel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.data.mock.MockProducts
import com.sachlabel.app.data.mock.MockScenario
import com.sachlabel.app.data.model.UserLanguage
import com.sachlabel.app.data.model.Verdict
import com.sachlabel.app.ui.components.SachLabelHeader
import com.sachlabel.app.ui.components.VerdictBadge
import com.sachlabel.app.ui.theme.*

data class HistoryAuditItem(
    val id: String,
    val brandName: String,
    val packInfo: String,
    val timestamp: String,
    val frontClaim: String,
    val backTruth: String,
    val statutoryDiscrepancy: String,
    val verdict: Verdict,
    val mockScenario: MockScenario
)

private val SAMPLE_HISTORY = listOf(
    HistoryAuditItem(
        id = "1",
        brandName = "SuperFresh Whole Wheat Bread",
        packInfo = "400g Pack",
        timestamp = "Today, 11:20 AM",
        frontClaim = "100% Atta Bread",
        backTruth = "Only 38% Wheat Flour, 62% Refined Maida",
        statutoryDiscrepancy = "FSSAI: Exceeds Maida Threshold. INS 150a coloring used.",
        verdict = Verdict.MISLEADING,
        mockScenario = MockProducts.SCENARIO_B
    ),
    HistoryAuditItem(
        id = "2",
        brandName = "PureHarvest Cold Pressed Oil",
        packInfo = "1 Litre Tin",
        timestamp = "Yesterday",
        frontClaim = "100% Pure Kacchi Ghani",
        backTruth = "100% Single Ingredient (Clean Mustard Oil)",
        statutoryDiscrepancy = "True to Label • AGMARK Certified Quality",
        verdict = Verdict.CONSISTENT,
        mockScenario = MockProducts.SCENARIO_D
    ),
    HistoryAuditItem(
        id = "3",
        brandName = "ChocoMalt Daily Nutrition",
        packInfo = "500g Jar",
        timestamp = "3 days ago",
        frontClaim = "Immunity & Growth • No Preservatives",
        backTruth = "Actual: 38g Sugar / 100g • Sodium Benzoate detected",
        statutoryDiscrepancy = "Exceeds ICMR child sugar threshold. Preservative present.",
        verdict = Verdict.NEEDS_CONTEXT,
        mockScenario = MockProducts.SCENARIO_A
    )
)

/**
 * Stitch Screen 8 — History & Verified Grocery Audits Screen
 */
@Composable
fun HistoryScreen(
    selectedLanguage: UserLanguage,
    onAuditSelected: (MockScenario) -> Unit,
    onScanClick: () -> Unit,
    onLanguageClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filters = listOf("All", "⚠️ Alerts", "✅ Verified", "Atta vs Maida", "Sugar Checks")

    val filteredItems = remember(searchQuery, selectedFilter) {
        SAMPLE_HISTORY.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.brandName.contains(searchQuery, ignoreCase = true) ||
                item.frontClaim.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "⚠️ Alerts" -> item.verdict == Verdict.MISLEADING
                "✅ Verified" -> item.verdict == Verdict.CONSISTENT
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
    ) {
        // Sticky Stitch Header
        SachLabelHeader(
            title = "Scan History",
            subtitle = "SachLabel • पिछली जांचें",
            selectedLanguage = selectedLanguage,
            onLanguageClick = onLanguageClick
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar & Scan Trigger Shortcut
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search brand, claim or ingredient...", fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = OutlineColor, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceContainerLowest,
                            unfocusedContainerColor = SurfaceContainerLowest,
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = OutlineVariant.copy(alpha = 0.4f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerLowest)
                            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable(onClick = onScanClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan", tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryGreen else SurfaceContainerLowest)
                                .border(1.dp, if (isSelected) PrimaryGreen else OutlineVariant.copy(alpha = 0.35f), CircleShape)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }

            // Pantry Truth Score Banner
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                Text("Pantry Truth Summary", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("3 disguised ingredients detected this week", fontSize = 11.sp, color = TextSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PrimaryFixed)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("78% Clean", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }
                    }
                }
            }

            // History Audit Cards
            items(filteredItems) { audit ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x0A151D1A), spotColor = Color(0x14151D1A))
                        .clip(RoundedCornerShape(22.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .clickable { onAuditSelected(audit.mockScenario) }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SurfaceContainerLow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocalCafe, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text(audit.brandName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("${audit.timestamp} • ${audit.packInfo}", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            VerdictBadge(verdict = audit.verdict)
                        }

                        // Split Comparison
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceContainerLow)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("FRONT:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text("“${audit.frontClaim}”", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.25f))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("BACK:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AlertCrimson)
                                    Text(audit.backTruth, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                                }
                            }
                        }

                        // Bottom Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(audit.statutoryDiscrepancy, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Full Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
