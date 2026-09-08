package com.sachlabel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.data.model.UserLanguage
import com.sachlabel.app.ui.components.SachLabelHeader
import com.sachlabel.app.ui.theme.*

/**
 * Stitch Screen 10 — More / Settings Screen
 */
@Composable
fun MoreScreen(
    selectedLanguage: UserLanguage,
    onLanguageClick: () -> Unit,
    onStandardsClick: () -> Unit,
    onDemoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
    ) {
        SachLabelHeader(
            title = "Settings & More",
            subtitle = "Preferences & Truth Standards",
            selectedLanguage = selectedLanguage,
            onLanguageClick = onLanguageClick
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Language Selection Card
            item {
                SettingsActionCard(
                    icon = Icons.Default.Translate,
                    iconBg = PrimaryFixed,
                    iconTint = PrimaryGreen,
                    title = "App Language",
                    subtitle = "${selectedLanguage.displayName} (${selectedLanguage.displayNameEn})",
                    onClick = onLanguageClick
                )
            }

            // Standards / What We Check Card
            item {
                SettingsActionCard(
                    icon = Icons.Default.VerifiedUser,
                    iconBg = PrimaryFixed,
                    iconTint = PrimaryGreen,
                    title = "Truth Standards",
                    subtitle = "8 Bounded claim audit categories",
                    onClick = onStandardsClick
                )
            }

            // Demo Scenarios Card
            item {
                SettingsActionCard(
                    icon = Icons.Default.PlayCircle,
                    iconBg = CautionAmberLow,
                    iconTint = CautionAmber,
                    title = "Demo Scenarios",
                    subtitle = "5 Pre-built product packages (no camera needed)",
                    onClick = onDemoClick
                )
            }

            // On-Device Privacy Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x08151D1A), spotColor = Color(0x10151D1A))
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Text("100% On-Device & Private", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = "SachLabel processes product photos entirely on your device using ML Kit OCR. No photos or label data are uploaded to the cloud.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // About App Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLow)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("SachLabel v1.0 • India", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            "An independent food claim verification utility created to cross-reference front promotional assertions against statutory back-of-pack declarations.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x08151D1A), spotColor = Color(0x10151D1A))
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(subtitle, fontSize = 11.sp, color = TextSecondary)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
