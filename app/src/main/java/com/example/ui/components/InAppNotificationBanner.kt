package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationService
import kotlinx.coroutines.delay

@Composable
fun InAppNotificationBanner(
    modifier: Modifier = Modifier,
    onNavigateToMatchCenter: () -> Unit
) {
    val activeNotification by NotificationService.activeInAppNotification.collectAsState()

    LaunchedEffect(activeNotification) {
        if (activeNotification != null) {
            // Automatically dismiss after 4.5 seconds
            delay(4500)
            NotificationService.dismissActiveInAppNotification()
        }
    }

    AnimatedVisibility(
        visible = activeNotification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("in_app_notification_banner_visibility")
    ) {
        val notification = activeNotification ?: return@AnimatedVisibility

        // Dynamic theme styling based on event type
        val brush = when (notification.type) {
            "GOAL" -> Brush.horizontalGradient(listOf(Color(0xFF00796B), Color(0xFF008E47))) // Deep Teal to Emerald
            "CARD" -> Brush.horizontalGradient(listOf(Color(0xFFD84315), Color(0xFFFF8F00))) // Dark Orange to Gold
            "SUB" -> Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF0288D1))) // Royal Blue to Light Blue
            "EMERGENCY" -> Brush.horizontalGradient(listOf(Color(0xFFC62828), Color(0xFFE53935))) // Warning Reds
            else -> Brush.horizontalGradient(listOf(Color(0xFF37474F), Color(0xFF455A64))) // Slate Grey
        }

        val icon = when (notification.type) {
            "GOAL" -> Icons.Default.SportsSoccer
            "CARD" -> Icons.Default.Warning
            "SUB" -> Icons.Default.SwapHoriz
            "EMERGENCY" -> Icons.Default.Campaign
            else -> Icons.Default.NotificationsActive
        }

        val badgeText = when (notification.type) {
            "GOAL" -> "GOAL ALERT"
            "CARD" -> "MATCH EVENT"
            "SUB" -> "SUBSTITUTION"
            "EMERGENCY" -> "EMERGENCY ALERT"
            else -> "MATCH UPDATE"
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    onNavigateToMatchCenter()
                    NotificationService.dismissActiveInAppNotification()
                }
                .testTag("notification_banner_card"),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(brush)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Event Icon with Circular Ring
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = badgeText,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Banner text description
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = notification.teamCode,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notification.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = notification.message,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action Buttons
                IconButton(
                    onClick = { NotificationService.dismissActiveInAppNotification() },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("dismiss_notification_banner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Alert",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
