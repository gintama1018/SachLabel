package com.sachlabel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.data.model.Verdict
import com.sachlabel.app.ui.theme.*

@Composable
fun VerdictBadge(
    verdict: Verdict,
    modifier: Modifier = Modifier
) {
    val (label, icon, bgColor, contentColor) = when (verdict) {
        Verdict.MISLEADING -> Quadruple(
            "MISLEADING",
            Icons.Default.Warning,
            AlertCrimsonContainer,
            Color.White
        )
        Verdict.NEEDS_CONTEXT -> Quadruple(
            "NEEDS CONTEXT",
            Icons.Default.Info,
            CautionAmber,
            Color.White
        )
        Verdict.CONSISTENT -> Quadruple(
            "VERIFIED ACCURATE",
            Icons.Default.CheckCircle,
            PrimaryContainer,
            Color.White
        )
        Verdict.NOT_ENOUGH_EVIDENCE -> Quadruple(
            "UNCERTAIN",
            Icons.Default.Info,
            SurfaceContainerHigh,
            TextSecondary
        )
        Verdict.NO_CLAIM_DETECTED -> Quadruple(
            "NO CLAIM",
            Icons.Default.Info,
            SurfaceContainerHigh,
            TextSecondary
        )
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
