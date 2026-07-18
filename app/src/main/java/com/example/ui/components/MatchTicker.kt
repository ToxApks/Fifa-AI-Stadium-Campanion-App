package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppState
import com.example.data.MatchService
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.EmergencyRed

@Composable
fun MatchTicker(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveMatch by MatchService.liveMatchState.collectAsState()
    val competitionMatches by MatchService.competitionMatchesState.collectAsState()
    val isDarkMode by AppState.isDarkMode.collectAsState()
    val textScale by AppState.textScale.collectAsState()

    // Determine the active match to show: prioritize live matches, fallback to the next real scheduled match
    val match = remember(liveMatch, competitionMatches) {
        val lm = liveMatch
        if (lm != null && (lm.matchStatus == "LIVE" || lm.matchStatus == "IN_PLAY" || lm.matchStatus == "PAUSED")) {
            lm
        } else {
            val firstScheduled = competitionMatches.firstOrNull { it.status == "SCHEDULED" }
            if (firstScheduled != null) {
                com.example.data.LiveMatch(
                    matchId = firstScheduled.id,
                    competition = firstScheduled.competitionName,
                    matchStatus = "SCHEDULED",
                    currentMinute = firstScheduled.minute,
                    homeTeam = firstScheduled.homeTeam,
                    homeTeamCode = firstScheduled.homeTeamCode,
                    homeScore = 0,
                    awayTeam = firstScheduled.awayTeam,
                    awayTeamCode = firstScheduled.awayTeamCode,
                    awayScore = 0,
                    goalscorers = emptyList(),
                    cards = emptyList(),
                    substitutions = emptyList(),
                    kickoffTime = firstScheduled.minute
                )
            } else {
                lm
            }
        }
    }

    if (match == null) return

    val isLive = match.matchStatus == "LIVE" || match.matchStatus == "IN_PLAY" || match.matchStatus == "PAUSED"
    val isFinished = match.matchStatus == "FINISHED" || match.matchStatus == "FT"

    // Collect all match events in chronologically sorted timeline
    val events = remember(match) {
        val list = mutableListOf<TickerEvent>()
        
        // Add goals
        match.goalscorers.forEach { scoreStr ->
            val minute = scoreStr.substringAfter("(").substringBefore(")", "10'")
            val player = scoreStr.substringBefore("(")
            list.add(TickerEvent(minute, "⚽ GOAL! $player", EventType.GOAL))
        }

        // Add cards
        match.cards.forEach { cardStr ->
            val minute = cardStr.substringAfter("Yellow ").substringBefore(")", "30'")
            val playerWithTeam = cardStr.substringBefore("(")
            list.add(TickerEvent(minute, "🟨 Yellow Card: $playerWithTeam", EventType.CARD))
        }

        // Add substitutions
        match.substitutions.forEach { subStr ->
            val minute = subStr.substringAfter("(").substringBefore(")", "60'")
            val details = subStr.substringBefore("(")
            list.add(TickerEvent(minute, "🔄 Sub: $details", EventType.SUBSTITUTION))
        }

        // Sort events by minute (ascending)
        list.sortedBy { 
            it.minute.replace("'", "").trim().toIntOrNull() ?: 0 
        }
    }

    // Cycle through events
    var currentEventIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(4000)
                currentEventIndex = (currentEventIndex + 1) % events.size
            }
        }
    }

    val currentEvent = if (events.isNotEmpty()) events[currentEventIndex] else null

    // Pulsing live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigate("live_match_feed") }
            .testTag("match_ticker_card"),
        cornerRadius = 16.dp
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Live/Upcoming/Completed Indicator, Title, Navigation Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isLive -> EmergencyRed.copy(alpha = pulseAlpha)
                                    isFinished -> Color.Gray
                                    else -> Color(0xFF8B5CF6)
                                }
                            )
                    )
                    Text(
                        text = when {
                            isLive -> "WORLD CUP LIVE TICKER"
                            isFinished -> "COMPLETED MATCH"
                            else -> "UPCOMING MATCH"
                        },
                        fontWeight = FontWeight.Black,
                        color = when {
                            isLive -> EmergencyRed
                            isFinished -> Color.Gray
                            else -> Color(0xFF8B5CF6)
                        },
                        fontSize = (11 * textScale).sp,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Match Center",
                        fontSize = (11 * textScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go to Match Center",
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Middle Row: Scoreboard (Big Teams and Score/VS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    TeamBadge(match.homeTeamCode)
                    Text(
                        text = match.homeTeam.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * textScale).sp,
                        color = if (isDarkMode) Color.White else Color(0xFF131315),
                        maxLines = 1
                    )
                }

                // Score Card / VS
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0xFF1A1A1C) else Color(0xFFE2E8F0))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val scoreText = if (match.matchStatus == "SCHEDULED") "VS" else "${match.homeScore} - ${match.awayScore}"
                    AnimatedContent(
                        targetState = scoreText,
                        transitionSpec = {
                            if (targetState != initialState) {
                                (slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut())
                                    .using(SizeTransform(clip = false))
                            } else {
                                fadeIn() togetherWith fadeOut()
                            }
                        },
                        label = "ScoreAnimation"
                    ) { displayText ->
                        Text(
                            text = displayText,
                            fontWeight = FontWeight.Black,
                            fontSize = (16 * textScale).sp,
                            color = if (isDarkMode) Color.White else Color(0xFF131315),
                        )
                    }
                }

                // Away Team
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = match.awayTeam.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * textScale).sp,
                        color = if (isDarkMode) Color.White else Color(0xFF131315),
                        maxLines = 1
                    )
                    TeamBadge(match.awayTeamCode)
                }
            }

            // Bottom Divider
            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(if (isDarkMode) Color(0x11FFFFFF) else Color(0x11000000)))

            // Bottom Row: Live Match events ticker marquee or scheduled details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Event ticker",
                        tint = if (isLive) AccentEmerald else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Crossfade(
                        targetState = currentEvent,
                        animationSpec = tween(500),
                        label = "ticker"
                    ) { event ->
                        if (event != null && isLive) {
                            Text(
                                text = "[${event.minute}] ${event.description}",
                                fontSize = (12 * textScale).sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
                            )
                        } else {
                            Text(
                                text = when {
                                    isLive -> "Kickoff! Match in progress..."
                                    isFinished -> "Match ended. View full statistics and highlights."
                                    else -> "Upcoming scheduled match. Get ready for kickoff!"
                                },
                                fontSize = (12 * textScale).sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
                            )
                        }
                    }
                }

                // Match time/status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isLive -> AccentEmerald.copy(alpha = 0.15f)
                                isFinished -> Color.Gray.copy(alpha = 0.15f)
                                else -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val badgeText = when {
                        isLive -> match.currentMinute
                        isFinished -> "FT"
                        else -> match.kickoffTime.ifBlank { "TBD" }
                    }
                    AnimatedContent(
                        targetState = badgeText,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut())
                                .using(SizeTransform(clip = false))
                        },
                        label = "MinuteAnimation"
                    ) { minuteText ->
                        Text(
                            text = minuteText,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isLive -> AccentEmerald
                                isFinished -> Color.Gray
                                else -> Color(0xFF8B5CF6)
                            },
                            fontSize = (11 * textScale).sp
                        )
                    }
                }
            }
        }
    }
}

enum class EventType {
    GOAL, CARD, SUBSTITUTION
}

data class TickerEvent(
    val minute: String,
    val description: String,
    val type: EventType
)

@Composable
fun TeamBadge(code: String) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                when (code) {
                    "USA" -> Color(0xFF002868)
                    "MEX" -> Color(0xFF006847)
                    "CAN" -> Color(0xFFD80621)
                    "PAN" -> Color(0xFF0038A8)
                    "ARG" -> Color(0xFF75AADB)
                    "FRA" -> Color(0xFF002395)
                    else -> Color(0xFF008E47)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code.take(2),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp
        )
    }
}
