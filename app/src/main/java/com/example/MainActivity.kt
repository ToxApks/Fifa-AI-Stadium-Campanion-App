package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppState
import com.example.data.TtsManager
import com.example.ui.screens.*
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.components.ComposeErrorBoundary
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TtsManager.initialize(this)
        
        // Initialize Notification system with application context
        com.example.data.NotificationService.appContext = applicationContext
        
        // Request POST_NOTIFICATIONS permission dynamically on Android 13+ (Tiramisu)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainNavigationContainer()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TtsManager.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationContainer() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "landing"

    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()
    val isDarkMode by AppState.isDarkMode.collectAsState()
    val headerContentColor = if (isDarkMode) Color.White else Color(0xFF131315)

    // Pulse animation for AI quick summon orb
    val infinitePulse = rememberInfiniteTransition(label = "summon")
    val summonPulseSize by infinitePulse.animateFloat(
        initialValue = 54f,
        targetValue = 62f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseSize"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_main_container"),
        topBar = {
            if (currentRoute != "landing") {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsSoccer,
                                contentDescription = "FIFA Stadium Companion",
                                tint = if (highContrast) Color.White else Color(0xFFC5A059), // Editorial Gold
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = getHeaderTitle(currentRoute),
                                fontWeight = FontWeight.Black,
                                fontSize = (16 * textScale).sp,
                                color = headerContentColor
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentRoute != "dashboard") {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("back_navigation_btn")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = headerContentColor)
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { AppState.isDarkMode.value = !isDarkMode },
                            modifier = Modifier.testTag("theme_mode_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) "Switch to Light Day Mode" else "Switch to Dark Night Mode",
                                tint = headerContentColor
                            )
                        }
                        IconButton(
                            onClick = { navController.navigate("settings") },
                            modifier = Modifier.testTag("settings_navigation_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = headerContentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = headerContentColor
                    )
                )
            }
        },
        floatingActionButton = {},
        bottomBar = {
            if (currentRoute != "landing") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(Color.Transparent),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .height(68.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF161618).copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        border = BorderStroke(1.dp, Color(0xFF242427))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val items = listOf(
                                Triple("dashboard", Icons.Default.Home, "Home"),
                                Triple("live_match_feed", Icons.Default.SportsSoccer, "Matches"),
                                Triple("navigation", Icons.Default.Map, "Navigator"),
                                Triple("ai_assistant", Icons.Default.Forum, "AI Chat"),
                                Triple("settings", Icons.Default.Person, "Profile")
                            )

                            items.forEach { (route, icon, label) ->
                                val isSelected = currentRoute == route
                                
                                val animatedTint by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF008E47) else Color(0xFF8E8E93),
                                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                    label = "tab_tint"
                                )
                                
                                val animatedScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.12f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "tab_scale"
                                )

                                val indicatorWidth by animateDpAsState(
                                    targetValue = if (isSelected) 16.dp else 0.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "tab_indicator"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable {
                                            if (currentRoute != route) {
                                                navController.navigate(route) {
                                                    popUpTo("dashboard") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                        .testTag("nav_btn_$route"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = animatedScale
                                            scaleY = animatedScale
                                        }
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = animatedTint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            color = animatedTint,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .width(indicatorWidth)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF008E47))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(if (currentRoute == "landing") PaddingValues(0.dp) else innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "landing",
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { 400 },
                        animationSpec = tween(450, easing = EaseInOutCubic)
                    ) + fadeIn(animationSpec = tween(450))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -400 },
                        animationSpec = tween(450, easing = EaseInOutCubic)
                    ) + fadeOut(animationSpec = tween(450))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -400 },
                        animationSpec = tween(450, easing = EaseInOutCubic)
                    ) + fadeIn(animationSpec = tween(450))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { 400 },
                        animationSpec = tween(450, easing = EaseInOutCubic)
                    ) + fadeOut(animationSpec = tween(450))
                }
            ) {
                composable("landing") {
                    ComposeErrorBoundary {
                        LandingScreen(onEnterHub = { navController.navigate("dashboard") })
                    }
                }
                composable("dashboard") {
                    ComposeErrorBoundary {
                        DashboardScreen(onNavigate = { route -> navController.navigate(route) })
                    }
                }
                composable("ai_assistant") {
                    ComposeErrorBoundary {
                        AiAssistantScreen()
                    }
                }
                composable("navigation") {
                    ComposeErrorBoundary {
                        NavigationScreen()
                    }
                }
                composable("transit") {
                    ComposeErrorBoundary {
                        TransportationScreen()
                    }
                }
                composable("crowd_monitor") {
                    ComposeErrorBoundary {
                        CrowdMonitorScreen()
                    }
                }
                composable("accessibility") {
                    ComposeErrorBoundary {
                        AccessibilityScreen()
                    }
                }
                composable("emergency") {
                    ComposeErrorBoundary {
                        EmergencyScreen()
                    }
                }
                composable("food_shops") {
                    ComposeErrorBoundary {
                        FoodShopsScreen()
                    }
                }
                composable("lost_found") {
                    ComposeErrorBoundary {
                        LostFoundScreen()
                    }
                }
                composable("volunteer_portal") {
                    ComposeErrorBoundary {
                        VolunteerScreen()
                    }
                }
                composable("organizer_dashboard") {
                    ComposeErrorBoundary {
                        OrganizerScreen()
                    }
                }
                composable("settings") {
                    ComposeErrorBoundary {
                        SettingsProfileScreen()
                    }
                }
                composable("live_match_feed") {
                    ComposeErrorBoundary {
                        LiveMatchFeedScreen()
                    }
                }
            }

            InAppNotificationBanner(
                modifier = Modifier.align(Alignment.TopCenter),
                onNavigateToMatchCenter = {
                    navController.navigate("live_match_feed") {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

private fun getHeaderTitle(route: String): String {
    return when (route) {
        "live_match_feed" -> "LIVE MATCH CENTER"
        "dashboard" -> "MATCH DAY HQ"
        "ai_assistant" -> "FIFA MATCHDAY ASSISTANT"
        "navigation" -> "PITCH NAVIGATOR"
        "transit" -> "MATCHDAY TRANSIT"
        "crowd_monitor" -> "SPECTATOR FLOW"
        "accessibility" -> "ACCESSIBILITY HUB"
        "emergency" -> "EMERGENCY RESPONSE"
        "food_shops" -> "ECO STADIUM FOOD"
        "lost_found" -> "LOST & FOUND CASES"
        "volunteer_portal" -> "VOLUNTEER SERVICES"
        "organizer_dashboard" -> "STADIUM MATCH COMMAND"
        "settings" -> "MY SETTINGS"
        else -> "FIFA MATCH CENTER"
    }
}
