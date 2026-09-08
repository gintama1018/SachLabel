package com.sachlabel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.data.model.UserLanguage
import com.sachlabel.app.ui.theme.*

/**
 * Stitch signature curved dark-green gradient top header
 */
@Composable
fun SachLabelHeader(
    title: String,
    subtitle: String,
    selectedLanguage: UserLanguage? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
    contentBelow: (@Composable ColumnScope.() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(HeaderGradient)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: back button or avatar with title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PrimaryGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = if (showBackButton) 17.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = subtitle,
                            color = Color(0xFFA9F3C5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                // Right side actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedLanguage != null) {
                        // Language Chip (e.g., EN/हिं)
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable(onClick = onLanguageClick)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (selectedLanguage) {
                                    UserLanguage.HINDI -> "हिं/EN"
                                    UserLanguage.TAMIL -> "த/EN"
                                    UserLanguage.BENGALI -> "বা/EN"
                                    else -> "EN/हिं"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (trailingContent != null) {
                        trailingContent()
                    } else if (!showBackButton) {
                        // Notification Bell
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            // Alert dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-3).dp, y = 3.dp)
                                    .clip(CircleShape)
                                    .background(AlertCrimsonContainer)
                            )
                        }
                    }
                }
            }

            if (contentBelow != null) {
                Spacer(modifier = Modifier.height(14.dp))
                contentBelow()
            }
        }
    }
}
