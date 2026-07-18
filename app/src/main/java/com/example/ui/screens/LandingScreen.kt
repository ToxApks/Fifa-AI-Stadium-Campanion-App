package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.AppState
import com.example.data.DatabaseService
import com.example.data.FootballPhotos
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald

// Brand-consistent official color palette
val FifaGold = Color(0xFFD4AF37)
val BrandDarkNavy = Color(0xFF030712)
val SoftBlueAccent = Color(0x1F2563EB)
val TextLightGray = Color(0xFF94A3B8)

@Composable
fun FlipClockCard(
    value: String,
    label: String,
    textScale: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, FifaGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Horizontal Split Line for flip look
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BrandDarkNavy)
                    .align(Alignment.Center)
            )
            
            Text(
                text = value,
                fontSize = (26 * textScale).sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextLightGray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LandingScreen(
    onEnterHub: () -> Unit
) {
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()

    val currentUser by DatabaseService.currentUser.collectAsState()
    val authError by DatabaseService.authError.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Spectator (Fan)") }
    var mode by remember { mutableStateOf("LOGIN") } // LOGIN, SIGNUP, FORGOT
    var loading by remember { mutableStateOf(false) }
    var successMsg by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var expandedRoleMenu by remember { mutableStateOf(false) }

    // --- Onboarding Form State ---
    var onboardFullName by remember { mutableStateOf("") }
    var onboardCountry by remember { mutableStateOf("") }
    var onboardFavTeam by remember { mutableStateOf("") }
    var onboardFavStadium by remember { mutableStateOf("MetLife Stadium") }
    var onboardTicketInfo by remember { mutableStateOf("") }
    var onboardPrefLanguage by remember { mutableStateOf("English") }
    var onboardHighContrast by remember { mutableStateOf(false) }
    var onboardVoiceOutput by remember { mutableStateOf(false) }
    var onboardRole by remember { mutableStateOf("Spectator (Fan)") }
    var onboardExpandedRole by remember { mutableStateOf(false) }
    var onboardExpandedLang by remember { mutableStateOf(false) }

    // --- Simulated Match Stats State for animations ---
    var hScore by remember { mutableStateOf(2) }
    var aScore by remember { mutableStateOf(1) }
    var mMin by remember { mutableStateOf(78) }
    var hPoss by remember { mutableStateOf(54f) }
    var hShots by remember { mutableStateOf(12) }
    var aShots by remember { mutableStateOf(9) }
    var hCorners by remember { mutableStateOf(6) }
    var aCorners by remember { mutableStateOf(4) }
    var hYellow by remember { mutableStateOf(1) }
    var aYellow by remember { mutableStateOf(2) }
    var hRed by remember { mutableStateOf(0) }
    var aRed by remember { mutableStateOf(0) }
    var goalScorer by remember { mutableStateOf("C. Pulisic 43' (P)") }
    var isPulsingGoal by remember { mutableStateOf(false) }

    LaunchedEffect(isPulsingGoal) {
        if (isPulsingGoal) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(150)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            if (!it.onboardingCompleted) {
                onboardFullName = it.name
                onboardCountry = it.country
                onboardFavTeam = it.favouriteTeam
                onboardFavStadium = it.favouriteStadium.ifEmpty { "MetLife Stadium" }
                onboardTicketInfo = it.ticketInfo
                onboardPrefLanguage = it.language
                onboardHighContrast = it.highContrast
                onboardVoiceOutput = it.voiceOutput
                onboardRole = it.role
            }
        }
    }

    // Dynamic stats simulation loop for animated score updates
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(8000) // Update every 8 seconds
            mMin += 1
            if (mMin > 90) {
                mMin = 78
                hScore = 2
                aScore = 1
                goalScorer = "C. Pulisic 43' (P)"
                hPoss = 54f
                hShots = 12
                aShots = 9
                hCorners = 6
                aCorners = 4
                hYellow = 1
                aYellow = 2
            }
            
            // Random fluctuations
            hPoss = (50f + (Math.sin(mMin.toDouble() * 0.5) * 6).toFloat()).coerceIn(40f, 65f)
            
            if (mMin == 81) {
                aScore = 2
                goalScorer = "R. Jiménez 81' ⚽"
                isPulsingGoal = true
                aShots += 1
                aYellow += 1
            } else if (mMin == 86) {
                hScore = 3
                goalScorer = "T. Weah 86' ⚽"
                isPulsingGoal = true
                hShots += 1
                hCorners += 1
            } else {
                isPulsingGoal = false
            }
        }
    }

    // --- Premium Simulated Kickoff Loading States ---
    var enteringHubWithLoader by remember { mutableStateOf(false) }
    var loaderProgress by remember { mutableStateOf(0f) }
    var loaderMessage by remember { mutableStateOf("Preparing Match Day...") }

    LaunchedEffect(enteringHubWithLoader) {
        if (enteringHubWithLoader) {
            val messages = listOf(
                "Preparing Match Day...",
                "Loading Stadium Intelligence...",
                "Analyzing Crowd...",
                "Connecting Match Data...",
                "Almost Ready for Kickoff..."
            )
            val steps = 100
            val delayMs = 30L
            for (step in 1..steps) {
                loaderProgress = step / 100f
                val messageIndex = ((step - 1) / 20).coerceIn(0, messages.size - 1)
                loaderMessage = messages[messageIndex]
                kotlinx.coroutines.delay(delayMs)
            }
            onEnterHub()
            enteringHubWithLoader = false
        }
    }

    // --- Dynamic Matchday Animations ---
    val infiniteTransition = rememberInfiniteTransition(label = "stadium_matchday")
    
    // Rotating official World Cup gold ball
    val ballRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ballRotation"
    )

    // Ball hovering over the pitch
    val ballFloatY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ballFloatY"
    )

    // Player floating parallax animation
    val playerFloatY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playerFloatY"
    )

    // Trophy subtle floating
    val trophyFloatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyFloatY"
    )

    // Stadium crowd dynamic wave pulse
    val crowdWavePulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crowdWavePulse"
    )

    // Scoreboard status flashing light (Live dot)
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )

    // Entrance card animation trigger
    var entranceTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entranceTriggered = true
    }
    
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entranceTriggered) 1f else 0f,
        animationSpec = tween(750, easing = EaseOutCubic),
        label = "entranceAlpha"
    )
    val entranceOffsetY by animateDpAsState(
        targetValue = if (entranceTriggered) 0.dp else 30.dp,
        animationSpec = tween(750, easing = EaseOutCubic),
        label = "entranceOffsetY"
    )
    val entranceScale by animateFloatAsState(
        targetValue = if (entranceTriggered) 1f else 0.95f,
        animationSpec = tween(750, easing = EaseOutCubic),
        label = "entranceScale"
    )

    if (enteringHubWithLoader) {
        FifaWorldCupLoader(
            progress = loaderProgress,
            currentMessage = loaderMessage,
            highContrast = highContrast
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrandDarkNavy)
                .testTag("landing_screen")
        ) {
            // 0. High Resolution Premium Stadium Backdrop Image for Landing Page
            AsyncImage(
                model = FootballPhotos.HERO_STADIUM,
                contentDescription = "Stadium Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.15f // Subtle atmospheric backdrop
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
                    .graphicsLayer(
                        alpha = entranceAlpha,
                        translationY = entranceOffsetY.value,
                        scaleX = entranceScale,
                        scaleY = entranceScale
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- Official Brand Badge ---
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF0C1424))
                        .border(1.5.dp, FifaGold, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trophy icon",
                            tint = FifaGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "FIFA WORLD CUP 2026 • OFFICIAL MATCH CENTER",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (10 * textScale).sp,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // --- Premium Hero Stadium Backdrop (Primary Focal Point with Live Match Card) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, FifaGold.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Custom Stadium at Night Canvas representation (Premium Visual)
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        
                        // 1. Dark green grass pitch radial glow at bottom
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF0D4F2F), Color(0xFF03100A)),
                                center = Offset(cx, h),
                                radius = h * 1.5f
                            )
                        )

                        // 2. Beautiful floodlight spotlight beams radiating from top corners
                        // Left Spotlight
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(w * 0.45f, h)
                                lineTo(w * 0.15f, h)
                                close()
                            },
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                                start = Offset(0f, 0f),
                                end = Offset(w * 0.3f, h)
                            )
                        )
                        // Right Spotlight
                        drawPath(
                            path = Path().apply {
                                moveTo(w, 0f)
                                lineTo(w * 0.85f, h)
                                lineTo(w * 0.55f, h)
                                close()
                            },
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                                start = Offset(w, 0f),
                                end = Offset(w * 0.7f, h)
                            )
                        )

                        // 3. Faint elegant concentric stadium ring lines (abstract stadium bowl architecture)
                        drawArc(
                            color = Color.White.copy(alpha = 0.04f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(-w * 0.1f, h * 0.05f),
                            size = Size(w * 1.2f, h * 0.5f),
                            style = Stroke(width = 4f)
                        )
                        drawArc(
                            color = Color.White.copy(alpha = 0.02f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(-w * 0.15f, h * 0.08f),
                            size = Size(w * 1.3f, h * 0.52f),
                            style = Stroke(width = 3f)
                        )

                        // 4. Smooth cinematic vignette overlay
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF030712).copy(alpha = 0.2f), Color(0xFF030712).copy(alpha = 0.85f)),
                                startY = 0f,
                                endY = h
                            )
                        )
                    }

                    // --- Live Match Broadcast Card (Layered on top of the Stadium Backdrop using Glassmorphism) ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Broadcast Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .alpha(liveDotAlpha)
                                )
                                Text(
                                    text = "GROUP STAGE • GROUP A",
                                    color = Color.Red,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            // Live Game Minute
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(0.5.dp, FifaGold.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                AnimatedContent(
                                    targetState = mMin,
                                    transitionSpec = {
                                        slideInVertically { h -> h } + fadeIn() togetherWith
                                                slideOutVertically { h -> -h } + fadeOut()
                                    },
                                    label = "MinuteCounter"
                                ) { minVal ->
                                    Text(
                                        text = "$minVal'",
                                        color = FifaGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Stadium location & broadcast details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "METLIFE STADIUM, EAST RUTHERFORD",
                                color = TextLightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ATTENDANCE: 82,500",
                                color = TextLightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Teams Score Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Team USA
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Stylized Flag
                                Canvas(modifier = Modifier.size(24.dp, 16.dp)) {
                                    drawRect(Color(0xFF0A2540)) // Navy canton
                                    drawRect(Color(0xFFEF4444), topLeft = Offset(0f, 6f), size = Size(size.width, 4f)) // Red stripes
                                    drawRect(Color.White, topLeft = Offset(0f, 10f), size = Size(size.width, 3f))
                                }
                                Text("USA", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                            }

                            // Score Blocks with numbers animating elegantly
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = hScore,
                                        transitionSpec = {
                                            slideInVertically { h -> h } + fadeIn() togetherWith
                                                    slideOutVertically { h -> -h } + fadeOut()
                                        },
                                        label = "HomeScore"
                                    ) { score ->
                                        Text(
                                            text = "$score",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = FifaGold
                                        )
                                    }
                                }
                                
                                Text(":", fontSize = 20.sp, fontWeight = FontWeight.Black, color = FifaGold)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = aScore,
                                        transitionSpec = {
                                            slideInVertically { h -> h } + fadeIn() togetherWith
                                                    slideOutVertically { h -> -h } + fadeOut()
                                        },
                                        label = "AwayScore"
                                    ) { score ->
                                        Text(
                                            text = "$score",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Team MEX
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("MEX", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                                // Stylized Flag
                                Canvas(modifier = Modifier.size(24.dp, 16.dp)) {
                                    drawRect(Color(0xFF047857), size = Size(size.width * 0.33f, size.height))
                                    drawRect(Color.White, topLeft = Offset(size.width * 0.33f, 0f), size = Size(size.width * 0.33f, size.height))
                                    drawRect(Color(0xFFEF4444), topLeft = Offset(size.width * 0.66f, 0f), size = Size(size.width * 0.34f, size.height))
                                }
                            }
                        }

                        // Pulsing Goal Scorer Notice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                        ) {
                            if (isPulsingGoal) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(FifaGold)
                                        .alpha(liveDotAlpha)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "RECENT ACTION: $goalScorer",
                                color = if (isPulsingGoal) FifaGold else TextLightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("live_ticker_goal")
                            )
                        }

                        // Possession Bar with Smooth Slider Animation
                        val possessionAnim by animateFloatAsState(
                            targetValue = hPoss / 100f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                            label = "possessionAnim"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Possession USA: ${hPoss.toInt()}%", fontSize = 11.sp, color = TextLightGray, fontWeight = FontWeight.SemiBold)
                                Text("MEX: ${(100f - hPoss).toInt()}%", fontSize = 11.sp, color = TextLightGray, fontWeight = FontWeight.SemiBold)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(possessionAnim)
                                            .background(FifaGold)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(1f - possessionAnim)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }

                        // Additional Broadcast Statistics (Shots, Corners, Cards)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shots
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SHOTS", fontSize = 9.sp, color = TextLightGray, fontWeight = FontWeight.Bold)
                                Text("$hShots - $aShots", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF1E293B)))
                            // Corners
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CORNERS", fontSize = 9.sp, color = TextLightGray, fontWeight = FontWeight.Bold)
                                Text("$hCorners - $aCorners", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF1E293B)))
                            // Yellow Cards
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("YELLOWS", fontSize = 9.sp, color = TextLightGray, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(8.dp, 12.dp).background(Color(0xFFFFB300)).clip(RoundedCornerShape(1.dp)))
                                    Text("$hYellow - $aYellow", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // --- Countdown to Kickoff Section (Premium Flip Clock Block Design) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlipClockCard(value = "32", label = "DAYS", textScale = textScale)
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(FifaGold.copy(alpha = 0.2f)))
                    FlipClockCard(value = "14", label = "HOURS", textScale = textScale)
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(FifaGold.copy(alpha = 0.2f)))
                    FlipClockCard(value = "45", label = "MINUTES", textScale = textScale)
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(FifaGold.copy(alpha = 0.2f)))
                    FlipClockCard(value = "26", label = "SECONDS", textScale = textScale)
                }

                if (currentUser == null) {
                    // --- SPORTS BROADCAST ACCESS CARD (AUTH CARD) ---
                    // Designed as high-end glassmorphism with custom gold outline borders
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, FifaGold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .testTag("auth_card"),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Security icon",
                                    tint = FifaGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = when (mode) {
                                        "LOGIN" -> "FIFA OFFICIAL GATEWAY LOGIN"
                                        "REGISTER" -> "CREATE OFFICIAL MATCHDAY PROFILE"
                                        else -> "ACCESS RECOVERY SERVICES"
                                    },
                                    fontWeight = FontWeight.Black,
                                    color = FifaGold,
                                    fontSize = (12 * textScale).sp,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            // Error Card
                            if (errorMsg.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x22EF4444))
                                        .border(1.dp, Color.Red, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Text(errorMsg, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Success Card
                            if (successMsg.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x2210B981))
                                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Text(successMsg, color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (mode == "REGISTER") {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Ticket Holder Full Name", color = TextLightGray) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = TextLightGray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = FifaGold,
                                        unfocusedBorderColor = Color(0xFF1E293B),
                                        focusedLabelColor = FifaGold
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_name_field")
                                )

                                // Role selection box
                                Box {
                                    OutlinedTextField(
                                        value = role,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Operational Vibe Role", color = TextLightGray) },
                                        leadingIcon = { Icon(Icons.Default.Sports, contentDescription = "Role", tint = TextLightGray) },
                                        trailingIcon = {
                                            IconButton(onClick = { expandedRoleMenu = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = FifaGold,
                                            unfocusedBorderColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("auth_role_field")
                                    )
                                    DropdownMenu(
                                        expanded = expandedRoleMenu,
                                        onDismissRequest = { expandedRoleMenu = false },
                                        modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, FifaGold.copy(alpha = 0.3f))
                                    ) {
                                        val roles = listOf("Spectator (Fan)", "Field Support (Volunteer)", "Stadium Director (Organizer)")
                                        roles.forEach { r ->
                                            DropdownMenuItem(
                                                text = { Text(r, color = Color.White, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    role = r
                                                    expandedRoleMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Official Registered Email Address", color = TextLightGray) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = TextLightGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = FifaGold,
                                    unfocusedBorderColor = Color(0xFF1E293B),
                                    focusedLabelColor = FifaGold
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_field")
                            )

                            if (mode != "FORGOT") {
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Match Access Code (Password)", color = TextLightGray) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = TextLightGray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = FifaGold,
                                        unfocusedBorderColor = Color(0xFF1E293B),
                                        focusedLabelColor = FifaGold
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_password_field")
                                )
                            }

                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp).align(Alignment.CenterHorizontally), color = FifaGold)
                            } else {
                                // 1. Primary CTA Button: ENTER MATCH CENTER
                                Button(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        loading = true
                                        errorMsg = ""
                                        successMsg = ""
                                        when (mode) {
                                            "LOGIN" -> {
                                                DatabaseService.login(email, password,
                                                    onSuccess = {
                                                        loading = false
                                                        successMsg = "Kickoff Authorized!"
                                                    },
                                                    onFailure = {
                                                        loading = false
                                                        errorMsg = it
                                                    }
                                                )
                                            }
                                            "REGISTER" -> {
                                                DatabaseService.signUp(email, password, name, role,
                                                    onSuccess = {
                                                        loading = false
                                                        successMsg = "Welcome to the world cup squad!"
                                                    },
                                                    onFailure = {
                                                        loading = false
                                                        errorMsg = it
                                                    }
                                                )
                                            }
                                            "FORGOT" -> {
                                                DatabaseService.forgotPassword(email,
                                                    onSuccess = {
                                                        loading = false
                                                        successMsg = "Dispatching code recovery instructions..."
                                                    },
                                                    onFailure = {
                                                        loading = false
                                                        errorMsg = it
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FifaGold, contentColor = BrandDarkNavy),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("auth_submit_button")
                                ) {
                                    Text(
                                        text = when (mode) {
                                            "LOGIN" -> "ENTER MATCH CENTER"
                                            "REGISTER" -> "SUBMIT PROVISION DETAILS"
                                            else -> "DISPATCH ACCESS RECOVERY"
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                if (mode == "LOGIN") {
                                    // 2. Secondary CTA Button: CONTINUE AS GUEST (Bypasses login instantly as spectator fan)
                                    OutlinedButton(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            loading = true
                                            errorMsg = ""
                                            DatabaseService.login("spectator@fifa.com", "password123",
                                                onSuccess = {
                                                    loading = false
                                                    successMsg = "Welcome, Spectator Guest!"
                                                },
                                                onFailure = {
                                                    DatabaseService.signUp("spectator@fifa.com", "password123", "FIFA Spectator Guest", "Spectator (Fan)",
                                                        onSuccess = {
                                                            DatabaseService.login("spectator@fifa.com", "password123", { loading = false }, { loading = false; errorMsg = it })
                                                        },
                                                        onFailure = {
                                                            loading = false
                                                            errorMsg = it
                                                        }
                                                    )
                                                }
                                            )
                                        },
                                        border = BorderStroke(1.5.dp, Color.White),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("google_login_simulator")
                                    ) {
                                        Icon(Icons.Default.DirectionsRun, contentDescription = "Guest Pass", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("CONTINUE AS GUEST", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Google Secure Button
                                    OutlinedButton(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            loading = true
                                            errorMsg = ""
                                            DatabaseService.loginWithGoogle(
                                                context = context,
                                                coroutineScope = scope,
                                                onSuccess = {
                                                    loading = false
                                                    successMsg = "Successfully Authenticated with Google!"
                                                },
                                                onFailure = { err ->
                                                    loading = false
                                                    errorMsg = err
                                                }
                                            )
                                        },
                                        border = BorderStroke(1.dp, FifaGold.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FifaGold),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("google_signin_button")
                                    ) {
                                        Text("SIGN IN WITH GOOGLE (SECURE)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (mode) {
                                            "LOGIN" -> "New Squad Member? Register"
                                            else -> "Already on team sheet? Login"
                                        },
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.clickable {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            mode = if (mode == "LOGIN") "REGISTER" else "LOGIN"
                                            errorMsg = ""
                                            successMsg = ""
                                        }
                                    )

                                    if (mode == "LOGIN") {
                                        Text(
                                            text = "Forgot Access Code?",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.clickable {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                mode = "FORGOT"
                                                errorMsg = ""
                                                successMsg = ""
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (currentUser?.onboardingCompleted == false) {
                        // --- COMPREHENSIVE ONBOARDING FLOW CARD ---
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, FifaGold, RoundedCornerShape(24.dp))
                                .testTag("onboarding_flow_card"),
                            cornerRadius = 24.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = "Onboarding Passport",
                                        tint = FifaGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "PROVISION OFFICIAL MATCH PASSPORT",
                                        fontWeight = FontWeight.Black,
                                        color = FifaGold,
                                        fontSize = (12 * textScale).sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Text(
                                    text = "Welcome! Before kickoff, configure your official companion profile. These preferences synchronize across Firestore and optimize your AI Match Assistant in real-time.",
                                    color = Color.LightGray,
                                    fontSize = (11 * textScale).sp,
                                    lineHeight = 15.sp
                                )

                                Divider(color = Color(0x1AFFFFFF))

                                // 1. Full Name
                                OutlinedTextField(
                                    value = onboardFullName,
                                    onValueChange = { onboardFullName = it },
                                    label = { Text("Passport Holder Name", color = TextLightGray) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = TextLightGray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = FifaGold,
                                        unfocusedBorderColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("onboard_name_field")
                                )

                                // 2. Country of Origin
                                OutlinedTextField(
                                    value = onboardCountry,
                                    onValueChange = { onboardCountry = it },
                                    label = { Text("Country represented", color = TextLightGray) },
                                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = "Country", tint = TextLightGray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = FifaGold,
                                        unfocusedBorderColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("onboard_country_field")
                                )

                                // 3. Favourite Team & Stadium
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = onboardFavTeam,
                                        onValueChange = { onboardFavTeam = it },
                                        label = { Text("Fav Team", color = TextLightGray, fontSize = 11.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = FifaGold,
                                            unfocusedBorderColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).testTag("onboard_team_field")
                                    )
                                    OutlinedTextField(
                                        value = onboardFavStadium,
                                        onValueChange = { onboardFavStadium = it },
                                        label = { Text("Fav Stadium", color = TextLightGray, fontSize = 11.sp) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = FifaGold,
                                            unfocusedBorderColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).testTag("onboard_stadium_field")
                                    )
                                }

                                // 4. Ticket Info
                                OutlinedTextField(
                                    value = onboardTicketInfo,
                                    onValueChange = { onboardTicketInfo = it },
                                    label = { Text("Ticket Seating Ref (e.g. Sec 110, Row A)", color = TextLightGray) },
                                    leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Ticket", tint = TextLightGray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = FifaGold,
                                        unfocusedBorderColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("onboard_ticket_field")
                                )

                                // 5. Preferred Dialect / Language
                                Box {
                                    OutlinedTextField(
                                        value = onboardPrefLanguage,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Preferred Language Preset", color = TextLightGray) },
                                        leadingIcon = { Icon(Icons.Default.Translate, contentDescription = "Language", tint = TextLightGray) },
                                        trailingIcon = {
                                            IconButton(onClick = { onboardExpandedLang = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = FifaGold,
                                            unfocusedBorderColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("onboard_lang_field")
                                    )
                                    DropdownMenu(
                                        expanded = onboardExpandedLang,
                                        onDismissRequest = { onboardExpandedLang = false },
                                        modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, FifaGold.copy(alpha = 0.3f))
                                    ) {
                                        val dialects = listOf("English", "Español", "Français", "Português", "Deutsch", "Italiano", "العربية")
                                        dialects.forEach { dialect ->
                                            DropdownMenuItem(
                                                text = { Text(dialect, color = Color.White, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    onboardPrefLanguage = dialect
                                                    onboardExpandedLang = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // 6. Role Selection
                                Box {
                                    OutlinedTextField(
                                        value = onboardRole,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Match Day Operational Role", color = TextLightGray) },
                                        leadingIcon = { Icon(Icons.Default.Sports, contentDescription = "Role", tint = TextLightGray) },
                                        trailingIcon = {
                                            IconButton(onClick = { onboardExpandedRole = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = FifaGold,
                                            unfocusedBorderColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("onboard_role_field")
                                    )
                                    DropdownMenu(
                                        expanded = onboardExpandedRole,
                                        onDismissRequest = { onboardExpandedRole = false },
                                        modifier = Modifier.background(Color(0xFF0F172A)).border(1.dp, FifaGold.copy(alpha = 0.3f))
                                    ) {
                                        val roles = listOf("Spectator (Fan)", "Field Support (Volunteer)", "Stadium Director (Organizer)")
                                        roles.forEach { roleOption ->
                                            DropdownMenuItem(
                                                text = { Text(roleOption, color = Color.White, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    onboardRole = roleOption
                                                    onboardExpandedRole = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Divider(color = Color(0x1AFFFFFF))

                                // Accessibility Preferences
                                Text(
                                    text = "ACCESSIBILITY QUICK PRESETS",
                                    color = FifaGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Contrast, contentDescription = "Contrast", tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("High Contrast Mode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Switch(
                                        checked = onboardHighContrast,
                                        onCheckedChange = { onboardHighContrast = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = FifaGold,
                                            checkedTrackColor = Color(0xFF1E293B)
                                        ),
                                        modifier = Modifier.testTag("onboard_highcontrast_switch")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Voice", tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("AI Voice Narration Assistance", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Switch(
                                        checked = onboardVoiceOutput,
                                        onCheckedChange = { onboardVoiceOutput = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = FifaGold,
                                            checkedTrackColor = Color(0xFF1E293B)
                                        ),
                                        modifier = Modifier.testTag("onboard_voice_switch")
                                    )
                                }

                                Divider(color = Color(0x1AFFFFFF))

                                // Submit Button
                                Button(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        loading = true
                                        DatabaseService.completeOnboarding(
                                            name = onboardFullName,
                                            country = onboardCountry,
                                            language = onboardPrefLanguage,
                                            favouriteTeam = onboardFavTeam,
                                            favouriteStadium = onboardFavStadium,
                                            ticketInfo = onboardTicketInfo.ifEmpty { "Sec 114, Row M, Seat 8" },
                                            role = onboardRole,
                                            onSuccess = {
                                                loading = false
                                                enteringHubWithLoader = true
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FifaGold, contentColor = BrandDarkNavy),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("onboard_submit_btn")
                                ) {
                                    Text("COMPLETE PROFILE & KICKOFF ENTRY", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        DatabaseService.logout {
                                            successMsg = "Session terminated."
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = BorderStroke(1.dp, Color.Red),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Text("CANCEL ENTRY (SIGN OUT)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // LOGGED IN WELCOME CARD
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, FifaGold, RoundedCornerShape(20.dp)),
                            cornerRadius = 20.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("MATCHDAY ACCESS AUTHORIZED", color = FifaGold, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
                                    Text(currentUser?.name ?: "David Beckham", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    Text("Match Day Role: ${currentUser?.role ?: "Spectator"}", color = Color.LightGray, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        DatabaseService.logout {
                                            successMsg = "Logged out successfully"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("SIGN OUT", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        // --- Launch Stadium Control Center CTA ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(FifaGold, Color(0xFF9E7E1D))
                                        )
                                    )
                                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                                    .clickable { enteringHubWithLoader = true }
                                    .testTag("launch_portal_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = "Launch icon", tint = BrandDarkNavy)
                                    Text(
                                        text = "ENTER AI STADIUM MATCH CENTER",
                                        fontWeight = FontWeight.Black,
                                        color = BrandDarkNavy,
                                        fontSize = (14 * textScale).sp,
                                        letterSpacing = 1.2.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Enter Dashboard",
                                        tint = BrandDarkNavy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Redesigned Broadcast-style Feature Showcases ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "MATCH DAY LIVE FEATURES",
                        fontSize = (11 * textScale).sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Feature 1
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sports, contentDescription = "AI Intel", tint = FifaGold, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Match Intelligence Hub", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Text("Official World Cup AI Assistant trained on MetLife Stadium layout. Use text or live photos to report lost items, request crowd densities, or call medical support.", fontSize = 12.sp, color = TextLightGray, lineHeight = 16.sp)
                            }
                        }
                    }

                    // Feature 2
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SportsScore, contentDescription = "Wayfinding", tint = FifaGold, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("High-Contrast Pitch Wayfinding", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Text("Real-time responsive vector stadium route navigation with custom toggle settings for wheelchair accessible ramps, elevators, and sensory room access paths.", fontSize = 12.sp, color = TextLightGray, lineHeight = 16.sp)
                            }
                        }
                    }

                    // Feature 3
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = "Crowd flow", tint = FifaGold, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Spectator Flow Analytics", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Text("Monitor live gate wait times, security flow rates, and noise levels. Automatically link volunteer coordinates with wait times for real-time dispatch optimization.", fontSize = 12.sp, color = TextLightGray, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Premium FIFA Footer ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(color = Color(0x1AFFFFFF))
                    Text(
                        text = "FIFA World Cup 2026™ AI Innovation Lab",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Fully integrated with real-time stadium sensors, Gemini Pro 1.5, and local Android security architectures. Built for MetLife Stadium, New Jersey.",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun FifaWorldCupLoader(
    progress: Float,
    currentMessage: String,
    highContrast: Boolean
) {
    val textScale by AppState.textScale.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "loader_anim")
    
    // Smooth pulse for stadium vibe
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val floodlightAlpha = when {
        progress < 0.15f -> 0f
        progress in 0.15f..0.30f -> (progress - 0.15f) / 0.15f
        else -> 1f
    }

    val crowdCheerAlpha = when {
        progress < 0.30f -> 0f
        progress in 0.30f..0.45f -> (progress - 0.30f) / 0.15f
        else -> 1f
    }

    val ballRollProgress = when {
        progress < 0.45f -> 0f
        progress in 0.45f..0.60f -> (progress - 0.45f) / 0.15f
        else -> 1f
    }

    val scoreboardAlpha = when {
        progress < 0.60f -> 0f
        progress in 0.60f..0.75f -> (progress - 0.60f) / 0.15f
        else -> 1f
    }

    val welcomeAlpha = when {
        progress < 0.75f -> 0f
        progress in 0.75f..0.90f -> (progress - 0.75f) / 0.15f
        progress > 0.90f -> 1f - ((progress - 0.90f) / 0.10f).coerceIn(0f, 1f)
        else -> 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandDarkNavy)
            .testTag("cinematic_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Draw minimal pitch and stadium rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Clean pitch center circle (very subtle)
            drawCircle(
                color = FifaGold.copy(alpha = 0.08f),
                radius = w * 0.25f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
            drawLine(
                color = FifaGold.copy(alpha = 0.08f),
                start = Offset(0f, cy),
                end = Offset(w, cy),
                strokeWidth = 2f
            )

            // 2. Floodlights power on
            if (floodlightAlpha > 0f) {
                // Left Floodlight Beam
                val leftBeam = Path().apply {
                    moveTo(w * 0.15f, h * 0.15f)
                    lineTo(cx - 50f, cy)
                    lineTo(cx + 100f, cy)
                    close()
                }
                drawPath(
                    path = leftBeam,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = floodlightAlpha * 0.2f * glowAlpha), Color.Transparent),
                        startY = h * 0.15f,
                        endY = cy
                    )
                )

                // Right Floodlight Beam
                val rightBeam = Path().apply {
                    moveTo(w * 0.85f, h * 0.15f)
                    lineTo(cx - 100f, cy)
                    lineTo(cx + 50f, cy)
                    close()
                }
                drawPath(
                    path = rightBeam,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = floodlightAlpha * 0.2f * glowAlpha), Color.Transparent),
                        startY = h * 0.15f,
                        endY = cy
                    )
                )

                // Floodlight beacons
                drawCircle(Color.White.copy(alpha = floodlightAlpha), radius = 10f, center = Offset(w * 0.15f, h * 0.15f))
                drawCircle(Color.White.copy(alpha = floodlightAlpha), radius = 10f, center = Offset(w * 0.85f, h * 0.15f))
            }

            // 3. Crowd start cheering ambient wave
            if (crowdCheerAlpha > 0f) {
                drawCircle(
                    color = FifaGold.copy(alpha = crowdCheerAlpha * 0.1f * glowAlpha),
                    center = Offset(cx, cy),
                    radius = w * 0.4f
                )
            }
        }

        // Layout container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Scoreboard fade in
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .alpha(scoreboardAlpha),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier.width(340.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, FifaGold.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "METLIFE STADIUM",
                                fontSize = (10 * textScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93),
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FifaGold)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BrandDarkNavy
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FIFA WORLD CUP 2026™",
                                fontSize = (13 * textScale).sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "NJ / NY",
                                fontSize = (12 * textScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = FifaGold
                            )
                        }
                    }
                }
            }

            // Center: Football roll & Welcome
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (ballRollProgress > 0f) {
                    val ballRotation = ballRollProgress * 720f
                    val ballX = -150f + (ballRollProgress * 150f)

                    AsyncImage(
                        model = FootballPhotos.MATCH_BALL,
                        contentDescription = "Match Ball",
                        modifier = Modifier
                            .offset(x = ballX.dp)
                            .size(64.dp)
                            .clip(CircleShape)
                            .rotate(ballRotation),
                        contentScale = ContentScale.Crop
                    )
                }

                if (welcomeAlpha > 0f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.alpha(welcomeAlpha)
                    ) {
                        Text(
                            text = "WELCOME TO",
                            fontSize = (12 * textScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = FifaGold,
                            letterSpacing = 3.sp
                        )
                        Text(
                            text = "FIFA WORLD CUP",
                            fontSize = (32 * textScale).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "2026",
                            fontSize = (44 * textScale).sp,
                            fontWeight = FontWeight.Black,
                            color = FifaGold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            // Bottom: Broadcast progress indicator
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val statusText = when {
                    progress < 0.15f -> "PRE-LIGHTS BLACKOUT..."
                    progress in 0.15f..0.30f -> "POWERING METLIFE FLOODLIGHTS..."
                    progress in 0.30f..0.45f -> "STADIUM ACOUSTICS CONNECTING..."
                    progress in 0.45f..0.60f -> "PITCH READY • KICKOFF INITIATING..."
                    progress in 0.60f..0.75f -> "TV SCOREBOARD SYNCING..."
                    progress in 0.75f..0.90f -> "SQUAD WELCOME AUTHORIZED..."
                    else -> "ENTER HUB SECURE..."
                }

                Text(
                    text = statusText,
                    fontSize = (11 * textScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(FifaGold)
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}% READY",
                    fontSize = (10 * textScale).sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
