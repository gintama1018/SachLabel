package com.sachlabel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sachlabel.app.ui.theme.*

import androidx.compose.ui.res.stringResource
import com.sachlabel.app.R

enum class BottomTab(val route: String, val labelRes: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Default.Home),
    HISTORY("history", R.string.nav_history, Icons.Default.History),
    SCAN("scan", R.string.nav_scan, Icons.Default.CameraAlt),
    STANDARDS("what_we_check", R.string.nav_standards, Icons.Default.VerifiedUser),
    MORE("more", R.string.nav_more, Icons.Default.GridView)
}

/**
 * Stitch Floating Pill Bottom Navigation Dock
 * Recreated with exact geometry, padding, elevation and central pop action.
 */
@Composable
fun SachLabelBottomNav(
    currentRoute: String,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating pill container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = Color(0x1A151D1A), spotColor = Color(0x26151D1A))
                .border(1.dp, OutlineVariant.copy(alpha = 0.35f), CircleShape),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.98f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                NavTabItem(
                    tab = BottomTab.HOME,
                    isSelected = currentRoute == BottomTab.HOME.route,
                    onClick = { onTabSelected(BottomTab.HOME) },
                    modifier = Modifier.weight(1f)
                )

                // History
                NavTabItem(
                    tab = BottomTab.HISTORY,
                    isSelected = currentRoute == BottomTab.HISTORY.route,
                    onClick = { onTabSelected(BottomTab.HISTORY) },
                    modifier = Modifier.weight(1f)
                )

                // Space placeholder for central elevated scan button
                Spacer(modifier = Modifier.weight(1f))

                // Standards
                NavTabItem(
                    tab = BottomTab.STANDARDS,
                    isSelected = currentRoute == BottomTab.STANDARDS.route,
                    onClick = { onTabSelected(BottomTab.STANDARDS) },
                    modifier = Modifier.weight(1f)
                )

                // More
                NavTabItem(
                    tab = BottomTab.MORE,
                    isSelected = currentRoute == BottomTab.MORE.route,
                    onClick = { onTabSelected(BottomTab.MORE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Center Floating Scan Button Pop (-18dp offset above pill)
        Column(
            modifier = Modifier
                .offset(y = (-14).dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onTabSelected(BottomTab.SCAN) }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(elevation = 10.dp, shape = CircleShape, spotColor = PrimaryGreen.copy(alpha = 0.5f))
                    .border(3.5.dp, BackgroundSurface, CircleShape)
                    .clip(CircleShape)
                    .background(PrimaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Scan",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.nav_scan),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
private fun NavTabItem(
    tab: BottomTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = PrimaryGreen
    val inactiveColor = TextSecondary
    val labelText = stringResource(tab.labelRes)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = labelText,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = labelText,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}
