package com.sachlabel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.data.model.UserLanguage
import com.sachlabel.app.ui.components.SachLabelHeader
import com.sachlabel.app.ui.theme.*

/**
 * Stitch Screen 3 — Home Screen
 * Recreated with exact Stitch layout, proportions, Hero split cards, Action Center, and Latest Investigation.
 */
@Composable
fun HomeScreen(
    selectedLanguage: UserLanguage,
    onScanClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMockDemoClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onWhatWeCheckClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
    ) {
        // Sticky Signature Stitch Curved Header
        SachLabelHeader(
            title = "Namaskar, Amit",
            subtitle = "SachLabel • सच परखें",
            selectedLanguage = selectedLanguage,
            onLanguageClick = onLanguageClick,
            contentBelow = {
                // Verified Status Pill inside header
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = PrimaryFixed,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "14 Products Verified this Month",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• 3 flagged",
                        color = OnPrimaryContainer,
                        fontSize = 11.sp
                    )
                }
            }
        )

        // Scrollable Body Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 80.dp), // Clear persistent bottom nav
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── HERO SPLIT BLOCK (Left 58% dominant + Right 42% 2-stacked) ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Dominant Card: Scan New Product (Weight 1.4f)
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x0D151D1A), spotColor = Color(0x1A151D1A))
                        .clip(RoundedCornerShape(22.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .clickable(onClick = onScanClick)
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "SMART SCAN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan Product",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Front claim vs back fine print verification.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp,
                                maxLines = 2
                            )
                        }

                        // Viewfinder graphic
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Front", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.SyncAlt, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Text("Back", fontSize = 10.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        // CTA Button
                        Button(
                            onClick = onScanClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Right 2-Stacked Cards (Weight 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Stacked: Recent Scans
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x0A151D1A), spotColor = Color(0x14151D1A))
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceContainerLowest)
                            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                            .clickable(onClick = onHistoryClick)
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent Scans", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                            }
                            Text("3 analyzed today", fontSize = 10.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(AlertCrimsonLow)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("1 Alert", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AlertCrimson)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(PrimaryFixed.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("2 Safe", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                }
                            }
                        }
                    }

                    // Bottom Stacked: Audio Verdict
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x0A151D1A), spotColor = Color(0x14151D1A))
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceContainerLowest)
                            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                            .clickable(onClick = onMockDemoClick)
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Audio Verdict", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = CautionAmber, modifier = Modifier.size(15.dp))
                            }
                            Text("हिन्दी • தமிழ் • বাংলা", fontSize = 10.sp, color = TextSecondary)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryGreen))
                                Text("Listen (42s)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                    }
                }
            }

            // ── ACTION CENTER (3x2 Quick Action Matrix) ────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACTION CENTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Essential food transparency checks",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    TextButton(onClick = onWhatWeCheckClick) {
                        Text("View Rules", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                }

                // 3x2 Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionMatrixItem(
                        title = "Sugar & Oil",
                        subtitle = "Hidden fats",
                        badge = "ALERT",
                        badgeColor = AlertCrimson,
                        badgeBg = AlertCrimsonLow,
                        icon = Icons.Default.OilBarrel,
                        iconTint = AlertCrimson,
                        onClick = onMockDemoClick,
                        modifier = Modifier.weight(1f)
                    )
                    ActionMatrixItem(
                        title = "Claim Match",
                        subtitle = "Fact compare",
                        badge = "CORE",
                        badgeColor = PrimaryGreen,
                        badgeBg = PrimaryFixed,
                        icon = Icons.Default.Balance,
                        iconTint = PrimaryGreen,
                        onClick = onScanClick,
                        modifier = Modifier.weight(1f)
                    )
                    ActionMatrixItem(
                        title = "Atta / Maida",
                        subtitle = "Grain purity",
                        badge = null,
                        badgeColor = CautionAmber,
                        badgeBg = CautionAmberLow,
                        icon = Icons.Default.Grain,
                        iconTint = CautionAmber,
                        onClick = onMockDemoClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionMatrixItem(
                        title = "Kids Drinks",
                        subtitle = "Growth myths",
                        badge = "FSSAI",
                        badgeColor = AlertCrimson,
                        badgeBg = AlertCrimsonLow,
                        icon = Icons.Default.ChildCare,
                        iconTint = AlertCrimson,
                        onClick = onMockDemoClick,
                        modifier = Modifier.weight(1f)
                    )
                    ActionMatrixItem(
                        title = "Offline OCR",
                        subtitle = "Aisle ready",
                        badge = null,
                        badgeColor = PrimaryGreen,
                        badgeBg = PrimaryFixed,
                        icon = Icons.Default.CloudOff,
                        iconTint = TextSecondary,
                        onClick = onScanClick,
                        modifier = Modifier.weight(1f)
                    )
                    ActionMatrixItem(
                        title = "Demo Cases",
                        subtitle = "5 Scenarios",
                        badge = "LIVE",
                        badgeColor = PrimaryGreen,
                        badgeBg = PrimaryFixed,
                        icon = Icons.Default.PlayCircle,
                        iconTint = PrimaryGreen,
                        onClick = onMockDemoClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── LATEST INVESTIGATION CARD ──────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LATEST INVESTIGATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Text("Updated 10m ago", fontSize = 11.sp, color = TextMuted)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x0F151D1A), spotColor = Color(0x1A151D1A))
                        .clip(RoundedCornerShape(22.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceContainerLow),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocalCafe, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Malted Milk Chocolate Drink", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("400g • Supermarket Brand", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AlertCrimsonContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    Text("MISLEADING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        // Split Box
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerLow)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("FRONT ASSERTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text("“Power Packed with 100% Real California Almonds”", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AlertCrimsonLow)
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("STATUTORY BACK TRUTH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AlertCrimson)
                                    Text("Contains only 0.8% Almond Powder, 42.4% Added Refined Sugar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Rank #1 Added Sugar • Rank #6 Almonds", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }

                        // Bottom Delta
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Delta: 99.2% Non-Almond Fillers", fontSize = 11.sp, color = AlertCrimson, fontWeight = FontWeight.SemiBold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(onClick = onMockDemoClick)
                            ) {
                                Text("Full Breakdown", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionMatrixItem(
    title: String,
    subtitle: String,
    badge: String?,
    badgeColor: Color,
    badgeBg: Color,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x08151D1A), spotColor = Color(0x10151D1A))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(CircleShape)
                        .background(badgeBg)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(badge, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
