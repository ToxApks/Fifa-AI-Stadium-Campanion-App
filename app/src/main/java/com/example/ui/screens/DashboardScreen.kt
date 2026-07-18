package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppState
import com.example.data.MatchPhase
import com.example.data.UserRole
import com.example.data.TtsManager
import com.example.data.FootballPhotos
import com.example.ui.components.GlassCard
import com.example.ui.components.MatchTicker
import com.example.ui.components.AnimatedStaggeredEntry
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val language by AppState.currentLanguage.collectAsState()
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()
    val isDarkMode by AppState.isDarkMode.collectAsState()
    val ecoPoints by AppState.ecoRewardPoints.collectAsState()
    val carbonSaved by AppState.carbonSavedKg.collectAsState()
    val currentPhase by AppState.currentMatchPhase.collectAsState()
    val currentRole by AppState.currentUserRole.collectAsState()
    val activeAlerts by AppState.proactiveAlerts.collectAsState()

    val isTtsSpeaking by TtsManager.isSpeaking.collectAsState()
    val currentSpokenText by TtsManager.currentSpokenText.collectAsState()

    val currentUser by com.example.data.DatabaseService.currentUser.collectAsState()
    val lostItems by com.example.data.DatabaseService.lostItems.collectAsState()
    val volunteerTasks by com.example.data.DatabaseService.volunteerTasks.collectAsState()

    val liveMatch by com.example.data.MatchService.liveMatchState.collectAsState()
    val competitionMatches by com.example.data.MatchService.competitionMatchesState.collectAsState()
    val liveStandings by com.example.data.MatchService.liveStandingsState.collectAsState()
    val activeStadium by com.example.data.StadiumDatabase.activeStadium.collectAsState()

    // Dashboard tabs to mirror the multi-screen options shown in the image reference
    var selectedDashboardTab by remember { mutableStateOf(0) } // 0: EXPLORE, 1: STATS, 2: LINEUP, 3: STANDINGS
    val dashboardTabs = listOf("EXPLORE", "STATS", "LINEUP", "STANDINGS")

    // Standings league selector
    var selectedStandingsLeague by remember { mutableStateOf(0) } // 0: ALL, 1: PREMIER LEAGUE, 2: LA LIGA, 3: CHAMPIONS LEAGUE
    val standingsLeagues = listOf("ALL", "PREMIER LEAGUE", "LA LIGA", "CHAMPIONS LEAGUE")

    // Lineup tactical formation state
    var selectedFormation by remember { mutableStateOf("4-3-3") }
    val formationsList = listOf("4-3-3", "4-4-2", "3-5-2")

    // Countdown clock till next major live matches events
    var countdownHours by remember { mutableStateOf(1) }
    var countdownMinutes by remember { mutableStateOf(24) }
    var countdownSeconds by remember { mutableStateOf(45) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            if (countdownSeconds > 0) {
                countdownSeconds--
            } else {
                countdownSeconds = 59
                if (countdownMinutes > 0) {
                    countdownMinutes--
                } else {
                    countdownMinutes = 59
                    if (countdownHours > 0) {
                        countdownHours--
                    } else {
                        countdownHours = 2
                    }
                }
            }
        }
    }

    // High quality themed gradient background mapping
    val appBackground = if (isDarkMode) Color(0xFF090A11) else Color(0xFFF1F5F9)
    val cardContainerColor = if (isDarkMode) Color(0xFF141624) else Color.White
    val cardOutlineColor = if (isDarkMode) Color(0xFF25283F) else Color(0xFFE2E8F0)
    val appTextColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val mutedTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
            .testTag("dashboard_screen")
    ) {
        // High quality pitch overlay for immersive luxury sports look
        AsyncImage(
            model = FootballPhotos.PITCH_TEXTURE,
            contentDescription = "Football Grass Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (isDarkMode) 0.05f else 0.02f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Leave clean space for floating bottom nav
        ) {
            // ==========================================
            // HEADER CAPSULE: Premium shahinurstk02 style top header bar
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDarkMode) {
                                listOf(Color(0xFF131525), Color(0xFF090A11))
                            } else {
                                listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9))
                            }
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Profile capsule at the very top (Matches shahinurstk02@gmail.com visual in image!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Badge Capsule
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isDarkMode) Color(0xFF1F2238) else Color.White)
                                .border(1.dp, cardOutlineColor, RoundedCornerShape(30.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentUser?.email ?: "shahinurstk02@gmail.com",
                                color = appTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = (12 * textScale).sp
                            )
                        }

                        // Search and Notification icon controls
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0xFF1F2238) else Color.White)
                                    .border(1.dp, cardOutlineColor, CircleShape)
                                    .clickable { onNavigate("ai_assistant") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = appTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0xFF1F2238) else Color.White)
                                    .border(1.dp, cardOutlineColor, CircleShape)
                                    .clickable { onNavigate("settings") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = appTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // TABS ROW: Explore, Stats, Lineup, Standings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dashboardTabs.forEachIndexed { index, tabName ->
                            val isSelected = selectedDashboardTab == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDashboardTab = index }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = tabName,
                                    color = if (isSelected) Color(0xFF8B5CF6) else mutedTextColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = (13 * textScale).sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isSelected) Color(0xFF8B5CF6) else Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Real-Time Match Ticker belt directly below tabs for matchday atmosphere
            MatchTicker(onNavigate = onNavigate)

            // ==========================================
            // DYNAMIC CONTENT ACCORDING TO SELECTED TAB WITH PREMIUM TRANSITIONS
            // ==========================================
            AnimatedContent(
                targetState = selectedDashboardTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn(animationSpec = tween(300)))
                            .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut(animationSpec = tween(300)))
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "dashboard_tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ExploreTabContent(
                        isDarkMode = isDarkMode,
                        textScale = textScale,
                        cardContainerColor = cardContainerColor,
                        cardOutlineColor = cardOutlineColor,
                        appTextColor = appTextColor,
                        mutedTextColor = mutedTextColor,
                        countdownHours = countdownHours,
                        countdownMinutes = countdownMinutes,
                        countdownSeconds = countdownSeconds,
                        lostItems = lostItems,
                        volunteerTasks = volunteerTasks,
                        onNavigate = onNavigate,
                        currentPhase = currentPhase,
                        currentRole = currentRole,
                        activeAlerts = activeAlerts,
                        isTtsSpeaking = isTtsSpeaking,
                        currentSpokenText = currentSpokenText,
                        language = language,
                        liveMatch = liveMatch,
                        competitionMatches = competitionMatches,
                        activeStadium = activeStadium
                    )
                    1 -> StatsTabContent(
                        isDarkMode = isDarkMode,
                        textScale = textScale,
                        cardContainerColor = cardContainerColor,
                        cardOutlineColor = cardOutlineColor,
                        appTextColor = appTextColor,
                        mutedTextColor = mutedTextColor
                    )
                    2 -> LineupTabContent(
                        isDarkMode = isDarkMode,
                        textScale = textScale,
                        cardContainerColor = cardContainerColor,
                        cardOutlineColor = cardOutlineColor,
                        appTextColor = appTextColor,
                        mutedTextColor = mutedTextColor,
                        selectedFormation = selectedFormation,
                        formationsList = formationsList,
                        onFormationChanged = { selectedFormation = it }
                    )
                    3 -> StandingsTabContent(
                        isDarkMode = isDarkMode,
                        textScale = textScale,
                        cardContainerColor = cardContainerColor,
                        cardOutlineColor = cardOutlineColor,
                        appTextColor = appTextColor,
                        mutedTextColor = mutedTextColor,
                        selectedLeague = selectedStandingsLeague,
                        leaguesList = standingsLeagues,
                        onLeagueChanged = { index ->
                            selectedStandingsLeague = index
                            val code = when (index) {
                                1 -> "PL"
                                2 -> "PD"
                                3 -> "CL"
                                else -> "WC"
                            }
                            com.example.data.MatchService.setCompetitionCode(code)
                        },
                        liveStandings = liveStandings
                    )
                }
            }
        }
    }
}

// ============================================================================
// TAB 1: EXPLORE HUB VIEW
// ============================================================================
@Composable
fun ExploreTabContent(
    isDarkMode: Boolean,
    textScale: Float,
    cardContainerColor: Color,
    cardOutlineColor: Color,
    appTextColor: Color,
    mutedTextColor: Color,
    countdownHours: Int,
    countdownMinutes: Int,
    countdownSeconds: Int,
    lostItems: List<com.example.data.AppState.LostItem>,
    volunteerTasks: List<com.example.data.VolunteerTask>,
    onNavigate: (String) -> Unit,
    currentPhase: MatchPhase,
    currentRole: UserRole,
    activeAlerts: List<com.example.data.ProactiveAlert>,
    isTtsSpeaking: Boolean,
    currentSpokenText: String?,
    language: com.example.data.Language,
    liveMatch: com.example.data.LiveMatch?,
    competitionMatches: List<com.example.data.MatchFeedItem>,
    activeStadium: com.example.data.Stadium
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HERO BANNER: MATCH DAY LIVE (Matches Screen 2 Hero Graphics) ---
        AnimatedStaggeredEntry(index = 0) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131524))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                // High Quality Stadium Background
                AsyncImage(
                    model = FootballPhotos.HERO_STADIUM,
                    contentDescription = "Matchday Stadium Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )

                // Neon overlay gradient matching image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0F101E).copy(alpha = 0.95f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                    Color(0xFF00F5D4).copy(alpha = 0.2f)
                                )
                            )
                        )
                )

                // Layout details inside hero
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Text Column
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Text("LIVE NOW", color = Color(0xFFEF4444), fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeStadium.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                fontSize = (18 * textScale).sp
                            )
                        )
                        Text(
                            text = "${activeStadium.city.uppercase()}, ${activeStadium.country.uppercase()}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00F5D4),
                                letterSpacing = 1.sp,
                                fontSize = (14 * textScale).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "FIFA WORLD CUP 2026™ HOST VENUE",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10 * textScale).sp
                        )
                    }

                    // Right Graphic: Dual players avatar graphics overlaid beautifully
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        // Background glowing circle
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.3f))
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-20).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.offset(x = (-10).dp)
                        ) {
                            // Player 1
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF8B5CF6), CircleShape)
                                    .background(Color.Gray)
                            ) {
                                AsyncImage(
                                    model = FootballPhotos.PLAYER_ACTION_1,
                                    contentDescription = "Player 1",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            // Player 2
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF00F5D4), CircleShape)
                                    .background(Color.LightGray)
                            ) {
                                AsyncImage(
                                    model = FootballPhotos.PLAYER_ACTION_1,
                                    contentDescription = "Player 2",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // --- MATCHES LIST (Matches Screen 2 Bottom Layout) ---
        AnimatedStaggeredEntry(index = 1) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val liveMatches = remember(competitionMatches) {
                competitionMatches.filter { it.status == "LIVE" || it.status == "IN_PLAY" || it.status == "PAUSED" }
            }
            val upcomingMatches = remember(competitionMatches) {
                competitionMatches.filter { it.status == "SCHEDULED" }
            }
            val completedMatches = remember(competitionMatches) {
                competitionMatches.filter { it.status == "FINISHED" || it.status == "FT" }
            }

            Text(
                text = "MATCH SCHEDULE",
                color = appTextColor,
                fontWeight = FontWeight.Black,
                fontSize = (14 * textScale).sp,
                letterSpacing = 0.5.sp
            )

            if (liveMatches.isNotEmpty()) {
                Text(
                    text = "LIVE NOW",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                liveMatches.forEach { match ->
                    MatchItemCard(
                        match = match,
                        cardContainerColor = cardContainerColor,
                        cardOutlineColor = cardOutlineColor,
                        appTextColor = appTextColor,
                        mutedTextColor = mutedTextColor,
                        onNavigate = onNavigate
                    )
                }
            } else {
                Text(
                    text = "UPCOMING",
                    color = Color(0xFF8B5CF6),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                if (upcomingMatches.isNotEmpty()) {
                    upcomingMatches.forEach { match ->
                        MatchItemCard(
                            match = match,
                            cardContainerColor = cardContainerColor,
                            cardOutlineColor = cardOutlineColor,
                            appTextColor = appTextColor,
                            mutedTextColor = mutedTextColor,
                            onNavigate = onNavigate
                        )
                    }
                } else {
                    Text(
                        text = "No upcoming matches scheduled.",
                        color = mutedTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (completedMatches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "RECENT RESULTS",
                    color = mutedTextColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                completedMatches.forEach { match ->
                    MatchItemCard(
                        match = match,
                        cardContainerColor = cardContainerColor,
                        cardOutlineColor = cardOutlineColor,
                        appTextColor = appTextColor,
                        mutedTextColor = mutedTextColor,
                        onNavigate = onNavigate
                    )
                }
            }
        }
        }

        // --- WEATHER & FIELD CONDITION CARD ---
        AnimatedStaggeredEntry(index = 2) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardOutlineColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Sun weather status",
                        tint = WarningOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("FIELD METEOROLOGY", fontSize = 8.sp, color = mutedTextColor, fontWeight = FontWeight.Black)
                        Text("Clear sky • 24°C • Opened Roof", fontSize = 12.sp, fontWeight = FontWeight.Black, color = appTextColor)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("OPTIMAL GRASS", color = Color(0xFF8B5CF6), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        }

        // --- COMPANION SHORTCUT SERVICES ROW ---
        AnimatedStaggeredEntry(index = 3) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickButtons = listOf(
                Triple("🧭 Transit", "transit", Color(0xFF3B82F6)),
                Triple("🍔 Eco Dine", "food_shops", Color(0xFF10B981)),
                Triple("🤖 Co-Pilot", "ai_assistant", Color(0xFF00F5D4)),
                Triple("🚨 SOS Emergency", "emergency", Color(0xFFEF4444))
            )

            quickButtons.forEach { (title, route, accentColor) ->
                Button(
                    onClick = { onNavigate(route) },
                    colors = ButtonDefaults.buttonColors(containerColor = cardContainerColor),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = title.uppercase(),
                        color = appTextColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        }

        // --- IN-APP NOTIFICATIONS & OFFICIAL ALERTS HUB ---
        AnimatedStaggeredEntry(index = 4) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val topAlert = activeAlerts.firstOrNull()
            if (topAlert != null) {
                val bannerColor = when (topAlert.type) {
                    "CRITICAL" -> EmergencyRed
                    "WARNING" -> WarningOrange
                    else -> Color(0xFF10B981)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bannerColor.copy(alpha = 0.1f))
                        .border(1.dp, bannerColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (topAlert.type == "CRITICAL") Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = "Active Alert Notification",
                            tint = bannerColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OFFICIAL NOTICE: ${topAlert.title.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = bannerColor,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = topAlert.message,
                                fontSize = 11.sp,
                                color = appTextColor,
                                lineHeight = 14.sp
                            )
                        }

                        val alertSpokenText = "Official Notice: ${topAlert.title}. ${topAlert.message}"
                        val isThisAlertSpeaking = isTtsSpeaking && currentSpokenText == alertSpokenText
                        IconButton(
                            onClick = {
                                if (isThisAlertSpeaking) {
                                    TtsManager.stop()
                                } else {
                                    TtsManager.speak(alertSpokenText, language)
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("narrate_alert_btn")
                        ) {
                            Icon(
                                imageVector = if (isThisAlertSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isThisAlertSpeaking) "Stop Narration" else "Narrate Update",
                                tint = bannerColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        }

        // --- BARCODE MATCHPASS TICKET WALLET ---
        AnimatedStaggeredEntry(index = 5) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = "Active Ticket ID", tint = Color(0xFFF59E0B))
                        Text("ACTIVE MATCHDAY PASS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("VERIFIED GOLD", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SPECTATOR ENTRANCE", fontSize = 8.sp, color = mutedTextColor, fontWeight = FontWeight.Black)
                        Text("Gate C East Ramp", fontSize = 13.sp, fontWeight = FontWeight.Black, color = appTextColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SEAT ALLOCATION", fontSize = 8.sp, color = mutedTextColor, fontWeight = FontWeight.Black)
                        Text("Sec 114, Row M, Seat 8", fontSize = 13.sp, fontWeight = FontWeight.Black, color = appTextColor)
                    }
                }

                // Simulated Barcode Elements
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val barWidths = listOf(2, 4, 1, 3, 2, 4, 1, 2, 3, 1, 4, 2, 3, 1, 2, 4, 1, 3, 2, 1, 3, 1)
                            barWidths.forEach { width ->
                                Box(
                                    modifier = Modifier
                                        .width(width.dp)
                                        .fillMaxHeight()
                                        .background(Color.Black)
                                )
                            }
                        }
                        Text("FIFA-SEC114-ROW-M-88127-USAX", fontSize = 7.sp, color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        }

        // --- ADMINISTRATIVE SIMULATOR FLOW CONTROLS ---
        AnimatedStaggeredEntry(index = 6) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardOutlineColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ADMINISTRATIVE LAB SIMULATION PANEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF8B5CF6),
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { onNavigate("organizer_dashboard") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Open Command", tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                    }
                }

                // Match phase configuration
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ACTIVE MATCHDAY PHASE", fontSize = 8.sp, color = mutedTextColor, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MatchPhase.values().forEach { phase ->
                            val isSelected = currentPhase == phase
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF8B5CF6) else cardOutlineColor)
                                    .clickable { AppState.currentMatchPhase.value = phase }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = phase.displayName.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.White else mutedTextColor
                                )
                            }
                        }
                    }
                }

                // Persona switcher
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("USER INTERFACE DEMO PERSONA", fontSize = 8.sp, color = mutedTextColor, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UserRole.values().forEach { role ->
                            val isSelected = currentRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color.White else cardOutlineColor)
                                    .clickable { AppState.currentUserRole.value = role }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (role) {
                                        UserRole.FAN -> "SPECTATOR"
                                        UserRole.VOLUNTEER -> "SUPPORT"
                                        UserRole.ORGANIZER -> "DIRECTOR"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) Color.Black else mutedTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

// ============================================================================
// TAB 2: DETAILED COMPARE STATS (Matches Screen 6 Visual Stats Rows)
// ============================================================================
@Composable
fun StatsTabContent(
    isDarkMode: Boolean,
    textScale: Float,
    cardContainerColor: Color,
    cardOutlineColor: Color,
    appTextColor: Color,
    mutedTextColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Match Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            border = BorderStroke(1.dp, cardOutlineColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CONCACAF CHAMPIONS LEAGUE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B),
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF8B5CF6)), contentAlignment = Alignment.Center) {
                            Text("SHU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("SHEFFIELD UTD", color = appTextColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2", color = appTextColor, fontWeight = FontWeight.Black, fontSize = 28.sp)
                        Text("vs", color = mutedTextColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("3", color = appTextColor, fontWeight = FontWeight.Black, fontSize = 28.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF00F5D4)), contentAlignment = Alignment.Center) {
                            Text("MNJ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("MANCHESTER JR", color = appTextColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("MATCH FINISHED • FULL TIME", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Stats Comparison Belt Wrapper Card (Matches Screen 6 exactly!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            border = BorderStroke(1.dp, cardOutlineColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "OFFICIAL SOFASCORE MATCH STATISTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8B5CF6),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // List of Comparative Statistics
                val statistics = listOf(
                    Triple("POSSESSION", 55, 45),
                    Triple("SHOTS ON TARGET", 3, 12),
                    Triple("TOTAL SHOTS", 17, 21),
                    Triple("PASS ACCURACY", 82, 89),
                    Triple("CORNER KICKS", 6, 8),
                    Triple("FOULS COMMITTED", 9, 12),
                    Triple("YELLOW CARDS", 2, 3),
                    Triple("RED CARDS", 0, 1),
                    Triple("OFFSIDES", 2, 1)
                )

                statistics.forEach { (statName, valA, valB) ->
                    val isPercentage = statName.contains("POSSESSION") || statName.contains("ACCURACY")
                    val labelA = if (isPercentage) "$valA%" else String.format("%02d", valA)
                    val labelB = if (isPercentage) "$valB%" else String.format("%02d", valB)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(labelA, fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6), fontSize = 14.sp)
                            Text(statName, fontWeight = FontWeight.Bold, color = appTextColor, fontSize = 10.sp, letterSpacing = 0.5.sp)
                            Text(labelB, fontWeight = FontWeight.Black, color = Color(0xFF00F5D4), fontSize = 14.sp)
                        }

                        // Elegant dual progress bar matching the layout shown in image
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isDarkMode) Color(0xFF1E1E2C) else Color(0xFFE2E8F0))
                        ) {
                            val total = valA + valB
                            val fractionA = if (total > 0) valA.toFloat() / total else 0.5f

                            if (fractionA > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(fractionA)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF8B5CF6))
                                )
                            }
                            if (1f - fractionA > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f - fractionA)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF00F5D4))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: LIVE TACTICAL LINEUP (Matches Screen 5 Visual Pitch with jersey badges)
// ============================================================================
@Composable
fun LineupTabContent(
    isDarkMode: Boolean,
    textScale: Float,
    cardContainerColor: Color,
    cardOutlineColor: Color,
    appTextColor: Color,
    mutedTextColor: Color,
    selectedFormation: String,
    formationsList: List<String>,
    onFormationChanged: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Formation Control Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            border = BorderStroke(1.dp, cardOutlineColor)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SELECT SHEFFIELD UTD FORMATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8B5CF6),
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    formationsList.forEach { formation ->
                        val isSel = selectedFormation == formation
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF8B5CF6) else cardOutlineColor)
                                .clickable { onFormationChanged(formation) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formation,
                                color = if (isSel) Color.White else mutedTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // --- DRAWN TACTICAL SOCCER PITCH (Matches Screen 5) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(2.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                .background(Color(0xFF0F1E16)) // Immersive deep turf grass green
        ) {
            // Draw Football Pitch Grid Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Pitch Border Outline
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                    size = size.copy(width = size.width - 20.dp.toPx(), height = size.height - 20.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )

                // Halfway Line
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(10.dp.toPx(), h / 2),
                    end = Offset(w - 10.dp.toPx(), h / 2),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Center Circle
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = 45.dp.toPx(),
                    center = Offset(w / 2, h / 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )

                // Bottom Penalty Area
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(w / 4, h - 80.dp.toPx()),
                    size = size.copy(width = w / 2, height = 70.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )

                // Top Penalty Area
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(w / 4, 10.dp.toPx()),
                    size = size.copy(width = w / 2, height = 70.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }

            // Interactive Plotted Jersey Badges (Sheffield Utd 4-3-3 Home vs Manchester 4-2-3-1 Away)
            // Goalkeeper (GK)
            LineupPlayerBadge(
                number = "1", name = "F. Bero", role = "GK",
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-18).dp),
                color = Color(0xFFF59E0B)
            )

            // 4 Defenders
            LineupPlayerBadge(
                number = "4", name = "A. Ahmed", role = "CB",
                modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-55).dp, y = (-75).dp)
            )
            LineupPlayerBadge(
                number = "5", name = "J. Stones", role = "CB",
                modifier = Modifier.align(Alignment.BottomCenter).offset(x = (55).dp, y = (-75).dp)
            )
            LineupPlayerBadge(
                number = "3", name = "N. Ake", role = "LB",
                modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-120).dp, y = (-95).dp)
            )
            LineupPlayerBadge(
                number = "2", name = "K. Walker", role = "RB",
                modifier = Modifier.align(Alignment.BottomCenter).offset(x = (120).dp, y = (-95).dp)
            )

            // Midfielders (Change based on selected formation for delightful animated look!)
            if (selectedFormation == "4-3-3") {
                LineupPlayerBadge(
                    number = "16", name = "Rodri", role = "CDM",
                    modifier = Modifier.align(Alignment.Center).offset(y = (45).dp)
                )
                LineupPlayerBadge(
                    number = "17", name = "K. De Bruyne", role = "CM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (-60).dp, y = (10).dp)
                )
                LineupPlayerBadge(
                    number = "20", name = "B. Silva", role = "CM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (60).dp, y = (10).dp)
                )
            } else if (selectedFormation == "4-4-2") {
                LineupPlayerBadge(
                    number = "16", name = "Rodri", role = "CM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (-40).dp, y = (35).dp)
                )
                LineupPlayerBadge(
                    number = "20", name = "B. Silva", role = "CM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (40).dp, y = (35).dp)
                )
                LineupPlayerBadge(
                    number = "10", name = "J. Grealish", role = "LM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (-115).dp, y = (15).dp)
                )
                LineupPlayerBadge(
                    number = "47", name = "P. Foden", role = "RM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (115).dp, y = (15).dp)
                )
            } else { // 3-5-2
                LineupPlayerBadge(
                    number = "16", name = "Rodri", role = "CM",
                    modifier = Modifier.align(Alignment.Center).offset(y = (45).dp)
                )
                LineupPlayerBadge(
                    number = "17", name = "K. De Bruyne", role = "CM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (-50).dp, y = (15).dp)
                )
                LineupPlayerBadge(
                    number = "20", name = "B. Silva", role = "CM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (50).dp, y = (15).dp)
                )
                LineupPlayerBadge(
                    number = "10", name = "J. Grealish", role = "LM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (-120).dp, y = (5).dp)
                )
                LineupPlayerBadge(
                    number = "47", name = "P. Foden", role = "RM",
                    modifier = Modifier.align(Alignment.Center).offset(x = (120).dp, y = (5).dp)
                )
            }

            // Forwards
            if (selectedFormation == "4-4-2") {
                LineupPlayerBadge(
                    number = "9", name = "E. Haaland", role = "ST",
                    modifier = Modifier.align(Alignment.TopCenter).offset(x = (-40).dp, y = (70).dp)
                )
                LineupPlayerBadge(
                    number = "19", name = "J. Alvarez", role = "ST",
                    modifier = Modifier.align(Alignment.TopCenter).offset(x = (40).dp, y = (70).dp)
                )
            } else {
                LineupPlayerBadge(
                    number = "9", name = "E. Haaland", role = "ST",
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (55).dp)
                )
                LineupPlayerBadge(
                    number = "11", name = "J. Doku", role = "LW",
                    modifier = Modifier.align(Alignment.TopCenter).offset(x = (-110).dp, y = (75).dp)
                )
                LineupPlayerBadge(
                    number = "47", name = "P. Foden", role = "RW",
                    modifier = Modifier.align(Alignment.TopCenter).offset(x = (110).dp, y = (75).dp)
                )
            }

            // Top half Away team Goalkeeper silhouette just to represent opposite side
            LineupPlayerBadge(
                number = "31", name = "Ederson", role = "GK",
                modifier = Modifier.align(Alignment.TopCenter).offset(y = 15.dp),
                color = Color(0xFF00F5D4)
            )
        }

        // Substitutes Bench Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            border = BorderStroke(1.dp, cardOutlineColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SUBSTITUTES & BENCH SQUAD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8B5CF6),
                    letterSpacing = 1.sp
                )

                val substitutes = listOf(
                    Pair("18", "Stefan Ortega (GK)"),
                    Pair("82", "Rico Lewis (DF)"),
                    Pair("21", "Sergio Gomez (DF)"),
                    Pair("8", "Mateo Kovacic (MF)"),
                    Pair("27", "Matheus Nunes (MF)"),
                    Pair("52", "Oscar Bobb (FW)")
                )

                substitutes.forEach { (number, playerName) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(cardOutlineColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(number, color = appTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(playerName, color = appTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Substitution Option Available",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LineupPlayerBadge(
    number: String,
    name: String,
    role: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF8B5CF6)
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable {
            Toast.makeText(context, "$name ($role) Heatmap Loaded!", Toast.LENGTH_SHORT).show()
        }
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = if (color == Color(0xFF00F5D4)) Color.Black else Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

// ============================================================================
// TAB 4: LEAGUE STANDINGS (Matches Screen 8 Standings List Table)
// ============================================================================
@Composable
fun StandingsTabContent(
    isDarkMode: Boolean,
    textScale: Float,
    cardContainerColor: Color,
    cardOutlineColor: Color,
    appTextColor: Color,
    mutedTextColor: Color,
    selectedLeague: Int,
    leaguesList: List<String>,
    onLeagueChanged: (Int) -> Unit,
    liveStandings: List<com.example.data.TeamStanding>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Horizontal league capsule tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leaguesList.forEachIndexed { idx, leagueName ->
                val isSel = selectedLeague == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) Color(0xFF8B5CF6) else cardContainerColor)
                        .border(1.dp, if (isSel) Color(0xFF8B5CF6) else cardOutlineColor, RoundedCornerShape(20.dp))
                        .clickable { onLeagueChanged(idx) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = leagueName,
                        color = if (isSel) Color.White else mutedTextColor,
                        fontWeight = FontWeight.Black,
                        fontSize = (10 * textScale).sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Main Standings Leaderboard Table Card (Matches Screen 8 exactly!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            border = BorderStroke(1.dp, cardOutlineColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Table header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Black, color = mutedTextColor, fontSize = 10.sp)
                    Text("CLUB TEAM", modifier = Modifier.weight(1f), fontWeight = FontWeight.Black, color = mutedTextColor, fontSize = 10.sp)
                    Row(
                        modifier = Modifier.width(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("P", fontWeight = FontWeight.Black, color = mutedTextColor, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                        Text("W", fontWeight = FontWeight.Black, color = mutedTextColor, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                        Text("D", fontWeight = FontWeight.Black, color = mutedTextColor, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                        Text("GD", fontWeight = FontWeight.Black, color = mutedTextColor, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                        Text("PTS", fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6), fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.width(30.dp))
                    }
                }

                Divider(color = cardOutlineColor, thickness = 1.dp)

                // Standings rows data (Screen 8 styling)
                val leaderboard = remember(liveStandings) {
                    liveStandings.map { s ->
                        StandingsEntry(
                            pos = s.position,
                            code = s.teamCode,
                            name = s.teamName,
                            p = s.playedGames,
                            w = s.won,
                            d = s.draw,
                            l = s.lost,
                            gd = if (s.goalDifference > 0) "+${s.goalDifference}" else s.goalDifference.toString(),
                            pts = s.points,
                            avatarColor = getTeamColor(s.teamCode)
                        )
                    }
                }

                leaderboard.forEach { entry ->
                    val isLeader = entry.pos == 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLeader) Color(0xFF8B5CF6).copy(alpha = 0.08f) else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isLeader) Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Position
                        Text(
                            text = entry.pos.toString(),
                            modifier = Modifier.width(24.dp),
                            fontWeight = FontWeight.Black,
                            color = if (isLeader) Color(0xFF8B5CF6) else appTextColor,
                            fontSize = 12.sp
                        )

                        // Team Code & Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(entry.avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(entry.code, color = Color.White, fontWeight = FontWeight.Black, fontSize = 8.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.name,
                                color = appTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Statistics
                        Row(
                            modifier = Modifier.width(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.p.toString(), fontWeight = FontWeight.Bold, color = appTextColor, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text(entry.w.toString(), fontWeight = FontWeight.Bold, color = appTextColor, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text(entry.d.toString(), fontWeight = FontWeight.Bold, color = appTextColor, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text(entry.gd, fontWeight = FontWeight.Bold, color = if (entry.gd.startsWith("+")) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                            Text(entry.pts.toString(), fontWeight = FontWeight.Black, color = if (isLeader) Color(0xFF8B5CF6) else appTextColor, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.width(30.dp))
                        }
                    }
                }
            }
        }
    }
}

data class StandingsEntry(
    val pos: Int,
    val code: String,
    val name: String,
    val p: Int,
    val w: Int,
    val d: Int,
    val l: Int,
    val gd: String,
    val pts: Int,
    val avatarColor: Color = Color(0xFF1E293B)
)

@Composable
fun MutedColor(isHighContrast: Boolean): Color {
    return if (isHighContrast) Color.White else Color(0xFF94A3B8)
}

@Composable
fun MatchItemCard(
    match: com.example.data.MatchFeedItem,
    cardContainerColor: Color,
    cardOutlineColor: Color,
    appTextColor: Color,
    mutedTextColor: Color,
    onNavigate: (String) -> Unit
) {
    val isLive = match.status == "LIVE" || match.status == "IN_PLAY" || match.status == "PAUSED"
    val isFinished = match.status == "FINISHED" || match.status == "FT"
    val isScheduled = match.status == "SCHEDULED"

    Card(
        onClick = { onNavigate("live_match_feed") },
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardOutlineColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Teams Column
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                // Home Team Row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(getTeamColor(match.homeTeamCode)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(match.homeTeamCode.take(3).uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = match.homeTeam.uppercase(),
                        fontWeight = FontWeight.Black,
                        color = appTextColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Away Team Row
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(getTeamColor(match.awayTeamCode)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(match.awayTeamCode.take(3).uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = match.awayTeam.uppercase(),
                        fontWeight = FontWeight.Black,
                        color = appTextColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Score and Status Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scores (only show scores if the match has started or finished)
                if (!isScheduled) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = match.homeScore.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = appTextColor
                        )
                        Text(
                            text = match.awayScore.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = appTextColor
                        )
                    }
                    Divider(modifier = Modifier.width(1.dp).height(40.dp), color = cardOutlineColor)
                }

                // Time/Status Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
                    if (isLive) {
                        Text(
                            text = match.minute,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    } else if (isFinished) {
                        Text(
                            text = "FT",
                            color = mutedTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(cardOutlineColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("FINISHED", color = mutedTextColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        // Scheduled / Upcoming
                        Text(
                            text = match.minute,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SCHEDULED", color = Color(0xFF8B5CF6), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

fun getTeamColor(code: String): Color {
    return when (code.uppercase()) {
        "USA" -> Color(0xFF002868)
        "MEX" -> Color(0xFF006847)
        "CAN" -> Color(0xFFD80621)
        "PAN" -> Color(0xFF0038A8)
        "ARG" -> Color(0xFF75AADB)
        "POL" -> Color(0xFFDC143C)
        "NED" -> Color(0xFFFF4F00)
        "KSA" -> Color(0xFF006C35)
        "FRA" -> Color(0xFF002395)
        "ENG" -> Color(0xFFCF142B)
        "GER" -> Color(0xFF000000)
        "BRA" -> Color(0xFFFEDF00)
        "ARS" -> Color(0xFFEF4444)
        "MCI" -> Color(0xFF6CABDD)
        "LIV" -> Color(0xFFC8102E)
        "CHE" -> Color(0xFF034694)
        "TOT" -> Color(0xFF132257)
        "AVL" -> Color(0xFF95BFE5)
        "RMA" -> Color(0xFFFEBE10)
        "FCB" -> Color(0xFF004170)
        "PSG" -> Color(0xFF004170)
        "BVB" -> Color(0xFFFDE100)
        "ATL", "ATM" -> Color(0xFFCB3524)
        "INT" -> Color(0xFF0066B2)
        "SHU" -> Color(0xFFE21A22)
        "MNJ" -> Color(0xFFDA291C)
        "IPT" -> Color(0xFF0000FF)
        else -> Color(0xFF475569)
    }
}
