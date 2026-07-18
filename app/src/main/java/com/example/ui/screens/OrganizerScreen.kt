package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppState
import com.example.data.GeminiRepository
import com.example.data.IncidentStatus
import com.example.ui.components.BeautifulAiMessage
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen() {
    val coroutineScope = rememberCoroutineScope()
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()

    val incidents by com.example.data.DatabaseService.incidents.collectAsState()
    val activeIncidentsCount = incidents.filter { it.status != IncidentStatus.RESOLVED }.size

    // Dynamic Live Health Score calculation
    val healthScore = maxOf(0, 100 - (activeIncidentsCount * 12))

    // Interactive staff sliders (linked directly to AppState wait times!)
    val volA by AppState.gateAVolunteers.collectAsState()
    val volB by AppState.gateBVolunteers.collectAsState()
    val volC by AppState.gateCVolunteers.collectAsState()

    val waitA = AppState.getGateWaitTime("Gate A (North)")
    val waitB = AppState.getGateWaitTime("Gate B (South)")
    val waitC = AppState.getGateWaitTime("Gate C (East)")

    var tacticalAdvice by remember { mutableStateOf("") }
    var isGeneratingAdvice by remember { mutableStateOf(false) }

    // AI Incident Copilot state variables
    var activeCopilotIncidentId by remember { mutableStateOf<String?>(null) }
    var copilotAnalysisText by remember { mutableStateOf("") }
    var isGeneratingCopilot by remember { mutableStateOf(false) }

    fun requestTacticalRecommendations() {
        isGeneratingAdvice = true
        coroutineScope.launch {
            val globalPrompt = AppState.getGlobalContextPrompt()
            val prompt = """
                $globalPrompt
                
                Analyze our current MetLife stadium operations details:
                - Dynamic Health Score: $healthScore%
                - Active Unresolved Incidents Count: $activeIncidentsCount cases
                - Gate A Staff Assigned: $volA, Gate B Staff Assigned: $volB, Gate C Staff Assigned: $volC
                - Current Queue Wait Times -> Gate A: ${waitA}m, Gate B: ${waitB}m, Gate C: ${waitC}m
                
                Please generate a highly structured strategic crowd control decision support layout. 
                You must format your response exactly like this:
                
                **1. SITUATIONAL ANALYSIS**
                [A rigorous summary of current stadium operations based on the match phase, active incidents, and gate wait times.]
                
                **2. DEEP NEURAL REASONING**
                [Step-by-step reasoning explaining why bottlenecks are occurring and the mathematical or logical reasons for your recommendations.]
                
                **3. ACTIONABLE STRATEGIC RECOMMENDATIONS**
                [Clear, bulleted directions on how to re-allocate staff, re-route crowd flows, and solve unresolved incidents.]
                
                **4. EXPECTED TOURNAMENT IMPACT**
                [The expected congestion decrease, volunteer efficiency gain, and carbon savings from re-routing.]
                
                **5. DECISION CONFIDENCE SCORE**
                [An analytical confidence score percentage (e.g. 96%) with brief backing logic.]
            """.trimIndent()
            val advice = GeminiRepository.generateResponse(prompt)
            tacticalAdvice = advice
            isGeneratingAdvice = false
        }
    }

    fun runIncidentCopilot(incident: com.example.data.IncidentReport) {
        activeCopilotIncidentId = incident.id
        isGeneratingCopilot = true
        coroutineScope.launch {
            val prompt = """
                ${AppState.getGlobalContextPrompt()}
                
                Analyze this reported stadium incident and act as the AI Incident Copilot:
                - Incident Title: ${incident.title}
                - Location: ${incident.location}
                - Description: ${incident.description}
                - Severity Level: ${incident.severity}
                
                Please provide the following formatted analysis:
                • **Severity Validation & Summary**: [Brief explanation of why this severity level is appropriate.]
                • **Optimal Incident Responder Team**: [Suggest the exact, most optimized emergency team to dispatch.]
                • **Standard Operating Emergency Plan**: [3 bullet points of tactical actions.]
                • **Broadcast Announcement**: [A concise public system announcement translated to our current language.]
            """.trimIndent()
            copilotAnalysisText = GeminiRepository.generateResponse(prompt)
            isGeneratingCopilot = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("organizer_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Column {
            Text(
                text = "FIFA TOURNAMENT OPERATIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * textScale).sp
                )
            )
            Text(
                text = "Organizer Operations Command",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textScale).sp,
                    color = Color.White
                )
            )
        }

        // --- Live Health Score Indicator ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE STADIUM HEALTH SCORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MutedColor(highContrast),
                            fontSize = (10 * textScale).sp
                        )
                    )
                    Text(
                        text = "$healthScore%",
                        fontSize = (38 * textScale).sp,
                        fontWeight = FontWeight.Black,
                        color = if (healthScore >= 80) AccentEmerald else WarningOrange
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (healthScore >= 80) AccentEmerald.copy(alpha = 0.2f) else EmergencyRed.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (healthScore >= 85) "Optimal Flow" else "Elevated Incidents",
                        fontWeight = FontWeight.Bold,
                        color = if (healthScore >= 85) AccentEmerald else EmergencyRed,
                        fontSize = (12 * textScale).sp
                    )
                }
            }
        }

        // --- Interactive Resource Re-Allocation Sliders ---
        Text(
            text = "STAFF ALLOCATION & CONGESTION CONTROLS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Slide to allocate volunteers instantly (Wait times will adjust)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.LightGray,
                        fontSize = (12 * textScale).sp
                    )
                )

                // Gate A
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gate A (North) Staff: $volA", color = Color.White, fontSize = (13 * textScale).sp)
                        Text("Est. Queue Wait: ${waitA}m", color = if (waitA <= 6) AccentEmerald else WarningOrange, fontWeight = FontWeight.Bold, fontSize = (12 * textScale).sp)
                    }
                    Slider(
                        value = volA.toFloat(),
                        onValueChange = { AppState.gateAVolunteers.value = it.toInt() },
                        valueRange = 1f..6f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = AccentCyan),
                        modifier = Modifier.testTag("gate_a_slider")
                    )
                }

                // Gate B
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gate B (South) Staff: $volB", color = Color.White, fontSize = (13 * textScale).sp)
                        Text("Est. Queue Wait: ${waitB}m", color = if (waitB <= 6) AccentEmerald else WarningOrange, fontWeight = FontWeight.Bold, fontSize = (12 * textScale).sp)
                    }
                    Slider(
                        value = volB.toFloat(),
                        onValueChange = { AppState.gateBVolunteers.value = it.toInt() },
                        valueRange = 1f..6f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = AccentCyan),
                        modifier = Modifier.testTag("gate_b_slider")
                    )
                }

                // Gate C
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gate C (East) Staff: $volC", color = Color.White, fontSize = (13 * textScale).sp)
                        Text("Est. Queue Wait: ${waitC}m", color = if (waitC <= 6) AccentEmerald else WarningOrange, fontWeight = FontWeight.Bold, fontSize = (12 * textScale).sp)
                    }
                    Slider(
                        value = volC.toFloat(),
                        onValueChange = { AppState.gateCVolunteers.value = it.toInt() },
                        valueRange = 1f..6f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = AccentCyan),
                        modifier = Modifier.testTag("gate_c_slider")
                    )
                }
            }
        }

        // --- Tactical Command Recommendations ---
        Button(
            onClick = { requestTacticalRecommendations() },
            modifier = Modifier.fillMaxWidth().testTag("get_tactical_advice_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (highContrast) Color.White else AccentCyan,
                contentColor = Color.Black
            )
        ) {
            if (isGeneratingAdvice) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
            } else {
                Icon(Icons.Default.Psychology, contentDescription = "Intelligence", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get AI Operational Tactical Recommendations", fontWeight = FontWeight.Bold)
            }
        }

        if (tacticalAdvice.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = "Strategic Suggestions", tint = AccentEmerald)
                        Text(
                            text = "AI TACTICAL DECISION SUPPORT REPORT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = (10 * textScale).sp
                            )
                        )
                    }
                    BeautifulAiMessage(
                        text = tacticalAdvice,
                        textScale = textScale
                    )
                }
            }
        }

        // --- Dynamical Incident Response Desk (AI Incident Copilot) ---
        Text(
            text = "DYNAMICAL INCIDENT RESPONSE DESK (AI COPILOT)",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        incidents.forEach { incident ->
            GlassCard(
                modifier = Modifier.fillMaxWidth().testTag("incident_item_${incident.id}")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(incident.severity.color))
                            )
                            Text(
                                text = incident.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = (14 * textScale).sp
                            )
                        }

                        // Severity Tag
                        Text(
                            text = incident.severity.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color(incident.severity.color)
                        )
                    }

                    Text(
                        text = "Location: ${incident.location} • Status: ${incident.status}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = incident.description,
                        fontSize = (12 * textScale).sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )

                    // Control Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (incident.status != IncidentStatus.RESOLVED) {
                                Button(
                                    onClick = {
                                        val nextStatus = if (incident.status == IncidentStatus.REPORTED) IncidentStatus.INVESTIGATING else IncidentStatus.RESOLVED
                                        com.example.data.DatabaseService.updateIncidentStatus(incident.id, nextStatus)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF), contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = if (incident.status == IncidentStatus.REPORTED) "Investigate" else "Resolve",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // AI Copilot Assist Button
                            Button(
                                onClick = { runIncidentCopilot(incident) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f), contentColor = AccentCyan),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp).testTag("incident_copilot_${incident.id}")
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = "AI Assist", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Copilot Assist", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Display active copilot suggestion for this incident
                    if (activeCopilotIncidentId == incident.id) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentCyan.copy(alpha = 0.08f))
                                .border(1.dp, AccentCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            if (isGeneratingCopilot) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentCyan)
                                    Text("Copilot reading telemetry and generating disaster response plan...", color = AccentCyan, fontSize = 11.sp)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = "Copilot Active", tint = AccentCyan, modifier = Modifier.size(14.dp))
                                        Text("AI COPILOT DECISION DISPATCH", color = AccentCyan, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                    }
                                    BeautifulAiMessage(
                                        text = copilotAnalysisText,
                                        textScale = textScale
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- LIVE MATCH SCOREBOARD & STATISTICS EDITOR (REAL-TIME COMMAND) ---
        Text(
            text = "LIVE MATCH SCOREBOARD & STATS COMMAND",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        val liveMatchState by com.example.data.MatchService.liveMatchState.collectAsState()
        val currentMatch = liveMatchState ?: com.example.data.LiveMatch()

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-time Score & Stats Control",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (14 * textScale).sp
                    )
                    Text(
                        text = "ORGANIZER VERIFIED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                    )
                }

                // Match Status & Minute
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentMatch.matchStatus,
                        onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(matchStatus = it)) },
                        label = { Text("Match Status") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = AccentCyan,
                            focusedBorderColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    OutlinedTextField(
                        value = currentMatch.currentMinute,
                        onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(currentMinute = it)) },
                        label = { Text("Current Minute") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = AccentCyan,
                            focusedBorderColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }

                // Home & Away Scores
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentMatch.homeScore.toString(),
                        onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(homeScore = it.toIntOrNull() ?: 0)) },
                        label = { Text("USA Score") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = AccentCyan,
                            focusedBorderColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    OutlinedTextField(
                        value = currentMatch.awayScore.toString(),
                        onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(awayScore = it.toIntOrNull() ?: 0)) },
                        label = { Text("MEX Score") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = AccentCyan,
                            focusedBorderColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }

                // Possession Sliders
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Possession USA: ${currentMatch.possessionHome}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("MEX: ${currentMatch.possessionAway}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = currentMatch.possessionHome.toFloat(),
                        onValueChange = {
                            val home = it.toInt()
                            val away = 100 - home
                            com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(possessionHome = home, possessionAway = away))
                        },
                        valueRange = 10f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = AccentCyan,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )
                }

                // Goalscorers
                OutlinedTextField(
                    value = currentMatch.goalscorers.joinToString(", "),
                    onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(goalscorers = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })) },
                    label = { Text("Goalscorers (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = AccentCyan,
                        focusedBorderColor = AccentCyan,
                        unfocusedLabelColor = Color.Gray,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                // Cards & Bookings
                OutlinedTextField(
                    value = currentMatch.cards.joinToString(", "),
                    onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(cards = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })) },
                    label = { Text("Bookings & Cards (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = AccentCyan,
                        focusedBorderColor = AccentCyan,
                        unfocusedLabelColor = Color.Gray,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                // Substitutions
                OutlinedTextField(
                    value = currentMatch.substitutions.joinToString(", "),
                    onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(substitutions = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })) },
                    label = { Text("Substitutions (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = AccentCyan,
                        focusedBorderColor = AccentCyan,
                        unfocusedLabelColor = Color.Gray,
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                // Shots
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentMatch.shotsHome.toString(),
                        onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(shotsHome = it.toIntOrNull() ?: 0)) },
                        label = { Text("Shots USA") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = AccentCyan,
                            focusedBorderColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    OutlinedTextField(
                        value = currentMatch.shotsAway.toString(),
                        onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(shotsAway = it.toIntOrNull() ?: 0)) },
                        label = { Text("Shots MEX") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = AccentCyan,
                            focusedBorderColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }

                // Expected Goals
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentMatch.expectedGoalsHome.toString(),
                        onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(expectedGoalsHome = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("xG USA") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = AccentCyan,
                            focusedBorderColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    OutlinedTextField(
                        value = currentMatch.expectedGoalsAway.toString(),
                        onValueChange = { com.example.data.MatchService.updateLiveMatchStatistics(currentMatch.copy(expectedGoalsAway = it.toDoubleOrNull() ?: 0.0)) },
                        label = { Text("xG MEX") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = AccentCyan,
                            focusedBorderColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            }
        }
    }
}
