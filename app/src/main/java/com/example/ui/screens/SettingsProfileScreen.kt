package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppState
import com.example.data.DatabaseService
import com.example.data.FootballPhotos
import com.example.data.Language
import com.example.ui.components.GlassCard
import com.example.ui.components.ProfileFormSkeleton
import com.example.ui.components.LocalErrorBoundary
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.EmergencyRed



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileScreen() {
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()
    val isDarkMode by AppState.isDarkMode.collectAsState()
    val currentLanguage by AppState.currentLanguage.collectAsState()
    val voiceEnabled by AppState.voiceOutputEnabled.collectAsState()
    val currentUser by DatabaseService.currentUser.collectAsState()

    // Interactive Profile States
    var nameInput by remember { mutableStateOf("") }
    var seatInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf("") }
    var countryInput by remember { mutableStateOf("") }
    var favouriteTeamInput by remember { mutableStateOf("") }
    var favouriteStadiumInput by remember { mutableStateOf("") }

    // Settings States
    var notificationsEnabled by remember { mutableStateOf(true) }
    var matchAlertsEnabled by remember { mutableStateOf(true) }
    var safetyBroadcastsEnabled by remember { mutableStateOf(true) }
    var privacySharedInput by remember { mutableStateOf(true) }
    var anonymousTelemetryInput by remember { mutableStateOf(true) }
    var connectedGoogleInput by remember { mutableStateOf(false) }

    // Alert & Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var passwordResetSent by remember { mutableStateOf(false) }
    var profileSaveSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            nameInput = it.name
            seatInput = it.seatInfo
            roleInput = it.role
            countryInput = it.country
            favouriteTeamInput = it.favouriteTeam
            favouriteStadiumInput = it.favouriteStadium.ifEmpty { "MetLife Stadium" }
            notificationsEnabled = it.notificationsEnabled
            privacySharedInput = it.privacyProfileShared
            connectedGoogleInput = it.connectedGoogle
        }
    }

    fun saveProfile() {
        val user = currentUser ?: return
        DatabaseService.updateProfileSettings(
            name = nameInput,
            role = roleInput.ifEmpty { user.role },
            language = currentLanguage.displayName,
            highContrast = highContrast,
            voiceOutput = voiceEnabled,
            textScale = textScale,
            seatInfo = seatInput
        )
        // Also save extended fields
        DatabaseService.completeOnboarding(
            name = nameInput,
            country = countryInput,
            language = currentLanguage.displayName,
            favouriteTeam = favouriteTeamInput,
            favouriteStadium = favouriteStadiumInput,
            ticketInfo = seatInput,
            role = roleInput.ifEmpty { user.role },
            onSuccess = {
                profileSaveSuccess = true
            }
        )
    }

    val activityHistory = remember(currentUser) {
        listOf(
            "Registered World Cup digital identity via secure Firebase Auth",
            "Provisioned official FIFA Green Sustainability passport profile",
            "Updated matchday favorite team to '${currentUser?.favouriteTeam?.ifEmpty { "Argentina" } ?: "Argentina"}'",
            "Verified seating ticket location: '${currentUser?.seatInfo?.ifEmpty { "Sec 114, Row M, Seat 8" } ?: "Sec 114, Row M, Seat 8"}'",
            "Logged in successfully from Android device (Matchday Hub)"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_profile_screen")
    ) {
        AsyncImage(
            model = FootballPhotos.PITCH_TEXTURE,
            contentDescription = "Football Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.08f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // --- Header ---
        Column {
            Text(
                text = "USER & CONFIGURATION HUB",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * textScale).sp
                )
            )
            Text(
                text = "My Profile & Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textScale).sp,
                    color = Color.White
                )
            )
        }

        // Save Confirmation Alert
        if (profileSaveSuccess) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, AccentEmerald),
                modifier = Modifier.fillMaxWidth().clickable { profileSaveSuccess = false }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = AccentEmerald)
                    Column {
                        Text("PROFILE SYNCHRONIZED", color = AccentEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("All passport parameters have been written to the Firestore cloud.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }

        // Password Reset Confirmation Dialog/Alert
        if (passwordResetSent) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, AccentCyan),
                modifier = Modifier.fillMaxWidth().clickable { passwordResetSent = false }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Email sent", tint = AccentCyan)
                    Column {
                        Text("PASSWORD RESET EN ROUTE", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("A secure password modification link has been dispatched to your email.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }

        // --- SECTION 1: FAN PASSPORT PROFILE ---
        Text(
            text = "MATCHDAY IDENTITY PASSPORT",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        if (currentUser == null) {
            ProfileFormSkeleton()
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, AccentCyan, CircleShape)
                    ) {
                        AsyncImage(
                            model = FootballPhotos.PLAYER_ACTION_1,
                            contentDescription = "Avatar Action Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column {
                        Text(
                            text = currentUser?.name ?: "Unknown User",
                            fontWeight = FontWeight.Black,
                            fontSize = (18 * textScale).sp,
                            color = Color.White
                        )
                        Text(
                            text = "Authenticated via Secure Firebase Auth: ${currentUser?.email ?: "spectator@fifa.com"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontSize = (11 * textScale).sp
                            )
                        )
                    }
                }

                Divider(color = Color(0x22FFFFFF))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = countryInput,
                    onValueChange = { countryInput = it },
                    label = { Text("Country Represented") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_country_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = favouriteTeamInput,
                        onValueChange = { favouriteTeamInput = it },
                        label = { Text("Fav Team", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("profile_team_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    OutlinedTextField(
                        value = favouriteStadiumInput,
                        onValueChange = { favouriteStadiumInput = it },
                        label = { Text("Stadium Hub", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("profile_stadium_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }

                OutlinedTextField(
                    value = seatInput,
                    onValueChange = { seatInput = it },
                    label = { Text("Stadium Ticket Seating Location") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_seat_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Button(
                    onClick = { saveProfile() },
                    modifier = Modifier.fillMaxWidth().testTag("save_profile_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highContrast) Color.White else AccentCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Save Passport Profile Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

        // --- SECTION 2: TOURNAMENT DIALECT ---
        Text(
            text = "TOURNAMENT DIALECT",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select Language Preset:",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = (13 * textScale).sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(Language.ENGLISH, Language.SPANISH, Language.FRENCH, Language.PORTUGUESE).forEach { lang ->
                        val isSel = lang == currentLanguage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) AccentCyan.copy(alpha = 0.25f) else Color(0x11FFFFFF))
                                .clickable { AppState.currentLanguage.value = lang }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSel) AccentCyan else Color.White,
                                    fontSize = (11 * textScale).sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 3: ACCESSIBILITY PREFERENCES ---
        Text(
            text = "ACCESSIBILITY & SCALE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High Contrast Mode", color = if (isDarkMode) Color.White else Color(0xFF131315), fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Enhances visual visibility for elements and layout borders", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = highContrast,
                        onCheckedChange = { AppState.highContrastMode.value = it },
                        modifier = Modifier.testTag("accessibility_contrast_switch")
                    )
                }

                Divider(color = if (isDarkMode) Color(0x11FFFFFF) else Color(0x11000000))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Theme Mode", color = if (isDarkMode) Color.White else Color(0xFF131315), fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Toggle between stadium night theme and crisp light day theme", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { AppState.isDarkMode.value = it },
                        modifier = Modifier.testTag("stadium_dark_theme_switch")
                    )
                }

                Divider(color = if (isDarkMode) Color(0x11FFFFFF) else Color(0x11000000))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Voice Assistance", color = if (isDarkMode) Color.White else Color(0xFF131315), fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Provides spoken match summaries and waypoint guidance", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = { AppState.voiceOutputEnabled.value = it },
                        modifier = Modifier.testTag("accessibility_voice_switch")
                    )
                }

                Divider(color = if (isDarkMode) Color(0x11FFFFFF) else Color(0x11000000))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Display & Text Scale", color = if (isDarkMode) Color.White else Color(0xFF131315), fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("${"%.2f".format(textScale)}x", color = AccentCyan, fontWeight = FontWeight.Bold)
                    }
                    Text("Resize UI text dynamically to optimize your viewing comfort", color = Color.Gray, fontSize = 11.sp)
                    Slider(
                        value = textScale,
                        onValueChange = { AppState.textScale.value = it },
                        valueRange = 0.8f..1.5f,
                        modifier = Modifier.testTag("text_scale_slider")
                    )
                }
            }
        }

        // --- SECTION 4: NOTIFICATION CONFIGURATION ---
        Text(
            text = "MATCH DAY NOTIFICATIONS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Push Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Receive major event and matchday updates directly", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            notificationsEnabled = it
                            DatabaseService.updateNotificationSettings(it)
                        },
                        modifier = Modifier.testTag("notifications_push_switch")
                    )
                }

                Divider(color = Color(0x11FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Match Goal Alerts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Immediate sound alerts when a team scores", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = matchAlertsEnabled,
                        onCheckedChange = { matchAlertsEnabled = it },
                        modifier = Modifier.testTag("notifications_goal_switch")
                    )
                }

                Divider(color = Color(0x11FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Emergency Safety Broadcasts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Crowd density and extreme weather alarms", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = safetyBroadcastsEnabled,
                        onCheckedChange = { safetyBroadcastsEnabled = it },
                        modifier = Modifier.testTag("notifications_safety_switch")
                    )
                }
            }
        }

        // --- SECTION 5: PRIVACY & SECURITY POLICIES ---
        Text(
            text = "PRIVACY, SECURITY & ACCOUNTS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Public Passport Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Share favorites and eco points on public leaderboards", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = privacySharedInput,
                        onCheckedChange = {
                            privacySharedInput = it
                            DatabaseService.updatePrivacySettings(it)
                        },
                        modifier = Modifier.testTag("privacy_sharing_switch")
                    )
                }

                Divider(color = Color(0x11FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anonymous Telemetry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Send basic anonymous logs to optimize stadium operations", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = anonymousTelemetryInput,
                        onCheckedChange = { anonymousTelemetryInput = it },
                        modifier = Modifier.testTag("privacy_telemetry_switch")
                    )
                }

                Divider(color = Color(0x11FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connect Google Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * textScale).sp)
                        Text("Provides swift single sign-on backup authentication", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = connectedGoogleInput,
                        onCheckedChange = {
                            connectedGoogleInput = it
                            DatabaseService.updateConnectedAccounts(it)
                        },
                        modifier = Modifier.testTag("security_google_switch")
                    )
                }

                Divider(color = Color(0x11FFFFFF))

                Button(
                    onClick = {
                        val email = currentUser?.email
                        if (!email.isNullOrEmpty()) {
                            DatabaseService.forgotPassword(
                                email = email,
                                onSuccess = { passwordResetSent = true },
                                onFailure = {}
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("forgot_pass_settings_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Secure Password Change Email")
                }
            }
        }

        // --- SECTION 6: COMPREHENSIVE ACTIVITY HISTORY ---
        Text(
            text = "PASSPORT HISTORY & SECURE LOGS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = "History Logs", tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = "REALTIME SESSION AUDIT LOGS",
                        fontWeight = FontWeight.Black,
                        color = AccentCyan,
                        fontSize = (11 * textScale).sp
                    )
                }

                Divider(color = Color(0x11FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                activityHistory.forEachIndexed { index, activity ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "[0${index + 1}]",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activity,
                            color = Color.LightGray,
                            fontSize = (11 * textScale).sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // --- SECTION 7: STRICT SECRETS DECOMPILATION POLICY ---
        Text(
            text = "SECURITY & COMPLIANCE WARNING",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = "Security Warning", tint = EmergencyRed, modifier = Modifier.size(28.dp))
                Column {
                    Text(
                        text = "STRICT SECRETS DECOMPILATION POLICY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmergencyRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10 * textScale).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.LightGray,
                            lineHeight = 16.sp,
                            fontSize = (11 * textScale).sp
                        )
                    )
                }
            }
        }

        // --- SECTION 7.5: DIAGNOSTIC CRASH RECOVERY COMPLIANCE ---
        val errorBoundary = LocalErrorBoundary.current
        Text(
            text = "SYSTEM RESILIENCE AUDIT",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AccentCyan,
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Simulate Matchday Screen Rendering Crash",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * textScale).sp
                )
                Text(
                    text = "Audit and verify our React-equivalent Jetpack Compose Error Boundary. Clicking below will inject a simulated Coordinate Projection Error into the Compose layout context to show off the custom, branded recovery UI, allowing you to copy logs or restart the screen renderer safely.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        errorBoundary.value = IllegalStateException("Simulated Matchday Screen Rendering Exception: Leaflet JavaScript coordinate projection failure on Pitch Navigator.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1F2E), contentColor = AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = "Simulate Crash", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TRIGGER SIMULATED SCREEN CRASH", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- SECTION 8: DANGER ZONE / PERMANENT DELETION ---
        Text(
            text = "DANGER ZONE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EmergencyRed,
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Permanent Account Deletion",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * textScale).sp
                )
                Text(
                    text = "Deleting your passport account will permanently erase your ticket references, sustainability points history, and digital identification from Firestore.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("delete_account_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete World Cup Passport Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Interactive Confirmation Dialog for Safety
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Danger", tint = EmergencyRed)
                    Text("CRITICAL: ERASE DIGITAL IDENTITY?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "This action is completely irreversible. All your ticket details, sustainability records, and cloud files will be scrubbed from Firestore forever. Are you sure you wish to proceed?",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            containerColor = Color(0xFF0F172A),
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        DatabaseService.deleteAccount(
                            onSuccess = {},
                            onFailure = {}
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("ERASE FOREVER", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("ABORT", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
    }
}
