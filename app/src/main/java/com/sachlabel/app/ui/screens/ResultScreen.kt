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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.R
import com.sachlabel.app.data.model.*
import com.sachlabel.app.tts.TtsManager
import com.sachlabel.app.ui.components.DualEvidenceCard
import com.sachlabel.app.ui.components.SachLabelHeader
import com.sachlabel.app.ui.components.VerdictBadge
import com.sachlabel.app.ui.components.VoiceVerdictCard
import com.sachlabel.app.ui.theme.*
import com.sachlabel.app.viewmodel.ScanViewModel

/**
 * Stitch Screen 7 — Product Verdict Screen
 * Exact visual hierarchy: Product card + Verdict badge -> Dual-pack evidence -> Explanation -> Audio -> Health CTA -> Scan another.
 */
@Composable
fun ResultScreen(
    uiState: ScanViewModel.ScanUiState,
    selectedLanguage: UserLanguage,
    onHealthContextClick: () -> Unit,
    onScanAnother: () -> Unit,
    onBack: () -> Unit = onScanAnother
) {
    val context = LocalContext.current
    val ttsManager = remember { TtsManager(context) }
    val ttsState by ttsManager.playbackState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { ttsManager.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSurface)
    ) {
        when (uiState) {
            is ScanViewModel.ScanUiState.Processing -> {
                ProcessingScreen(step = uiState.step)
            }

            is ScanViewModel.ScanUiState.Error -> {
                ErrorState(message = uiState.message, onScanAnother = onScanAnother)
            }

            is ScanViewModel.ScanUiState.Result -> {
                val scan = uiState.scan
                val result = scan.result

                if (result == null) {
                    ErrorState(
                        message = stringResource(R.string.result_error),
                        onScanAnother = onScanAnother
                    )
                } else {
                    ResultContent(
                        result = result,
                        selectedLanguage = selectedLanguage,
                        ttsState = ttsState,
                        onToggleListen = {
                            val text = if (selectedLanguage.code == "en") result.explanationEn
                                       else result.explanationLocalized.ifBlank { result.explanationEn }
                            if (ttsState == TtsManager.PlaybackState.SPEAKING) {
                                ttsManager.stop()
                            } else {
                                ttsManager.speak(text, selectedLanguage)
                            }
                        },
                        onHealthContextClick = onHealthContextClick,
                        onScanAnother = onScanAnother,
                        onBack = onBack
                    )
                }
            }

            else -> {
                // Idle
            }
        }
    }
}

@Composable
private fun ResultContent(
    result: ClaimResult,
    selectedLanguage: UserLanguage,
    ttsState: TtsManager.PlaybackState,
    onToggleListen: () -> Unit,
    onHealthContextClick: () -> Unit,
    onScanAnother: () -> Unit,
    onBack: () -> Unit
) {
    val explanation = if (selectedLanguage.code == "en") result.explanationEn
                      else result.explanationLocalized.ifBlank { result.explanationEn }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Stitch Header
        SachLabelHeader(
            title = stringResource(R.string.result_title),
            subtitle = stringResource(R.string.result_subtitle),
            selectedLanguage = selectedLanguage,
            showBackButton = true,
            onBackClick = onBack,
            trailingContent = {
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.common_share),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        // Scrollable Results Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 80.dp), // Clear bottom nav dock
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. PRODUCT SUMMARY HERO CARD (Stitch Green Gradient) ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10151D1A), spotColor = Color(0x1F151D1A))
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardGradientGreen)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Product Icon Thumbnail
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalCafe,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.result_packaged_food_verified),
                                fontSize = 11.sp,
                                color = PrimaryFixed,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (result.frontText.isNotBlank()) result.frontText else stringResource(R.string.result_scanned_product),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.result_evidence_backed),
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Verdict Badge at top right
                    VerdictBadge(verdict = result.verdict)
                }
            }

            // ── 2. DUAL-PACK CLAIM COMPARISON CARD ──────────────────────────────
            Text(
                text = stringResource(R.string.result_dual_pack_comparison),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 2.dp)
            )

            DualEvidenceCard(
                result = result,
                explanationText = explanation
            )

            // ── 3. VOICE VERDICT CARD (TTS) ─────────────────────────────────────
            VoiceVerdictCard(
                ttsState = ttsState,
                language = selectedLanguage,
                onToggleListen = onToggleListen
            )

            // ── 4. "WHAT DOES THIS MEAN FOR ME?" (Health Context CTA) ───────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onHealthContextClick)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CautionAmberLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = CautionAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.result_health_context),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.result_health_context_sub),
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── 5. BOTTOM ACTIONS ───────────────────────────────────────────────
            Button(
                onClick = onScanAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = PrimaryGreen.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.result_scan_next),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.result_report_forum),
                    fontSize = 11.sp,
                    color = AlertCrimson,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onScanAnother: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = message,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onScanAnother,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.result_try_again))
        }
    }
}
