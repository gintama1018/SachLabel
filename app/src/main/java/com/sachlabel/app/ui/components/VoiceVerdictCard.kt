package com.sachlabel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.sachlabel.app.tts.TtsManager
import com.sachlabel.app.ui.theme.*

/**
 * Stitch Voice Verdict Card with audio playback
 */
@Composable
fun VoiceVerdictCard(
    ttsState: TtsManager.PlaybackState,
    language: UserLanguage,
    onToggleListen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSpeaking = ttsState == TtsManager.PlaybackState.SPEAKING

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x0D151D1A), spotColor = Color(0x14151D1A))
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable(onClick = onToggleListen)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Voice Verdict",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Voice Verdict",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSpeaking) "Speaking now in ${language.displayName}..."
                               else "Listen in ${language.displayName} (${language.displayNameEn})",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Play / Pause Pill Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSpeaking) AlertCrimsonLow else PrimaryFixed)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSpeaking) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AlertCrimson)
                        )
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = AlertCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Stop",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertCrimson
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Play",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }
    }
}
