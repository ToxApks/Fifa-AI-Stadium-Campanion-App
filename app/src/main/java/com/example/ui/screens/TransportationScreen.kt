package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

data class TransitMode(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val frequency: String,
    val waitTimeMinutes: Int,
    val co2Savings: String,
    val cost: String,
    val rating: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportationScreen() {
    val coroutineScope = rememberCoroutineScope()
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()

    var destination by remember { mutableStateOf("Manhattan Penn Station, NY") }
    var travelAdvice by remember { mutableStateOf("") }
    var isGeneratingAdvice by remember { mutableStateOf(false) }

    val transits = listOf(
        TransitMode("NJ Transit Express Rail", Icons.Default.DirectionsTransit, "Every 8 mins", 6, "1.4 kg CO2 Saved", "$4.25", "Eco-Favorite"),
        TransitMode("Zero-Emission Shuttle", Icons.Default.DirectionsBus, "Every 5 mins", 3, "1.1 kg CO2 Saved", "Free", "Accessible"),
        TransitMode("Eco-Rideshare Hub", Icons.Default.LocalTaxi, "Continuous", 18, "0.0 kg CO2 (Surge)", "$48.00+", "Delayed")
    )

    fun requestAiTravelPlan() {
        isGeneratingAdvice = true
        coroutineScope.launch {
            val prompt = "Provide a highly efficient, sustainable exit and transit guide from MetLife Stadium Sec 114 to $destination after a crowded FIFA World Cup match. Prioritize green transit options and carbon reduction."
            val advice = GeminiRepository.generateResponse(prompt)
            travelAdvice = advice
            isGeneratingAdvice = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("transportation_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Column {
            Text(
                text = "MATCH DAY ECO-LOGISTICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFFBBF24), // World Cup Gold
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * textScale).sp
                )
            )
            Text(
                text = "Stadium Match Day Travel Planner",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textScale).sp,
                    color = Color.White
                )
            )
        }

        // --- Destination Configuration ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Configure Departure Route:",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = (14 * textScale).sp
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    modifier = Modifier.fillMaxWidth().testTag("destination_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981), // FIFA Green
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Button(
                    onClick = { requestAiTravelPlan() },
                    modifier = Modifier.fillMaxWidth().testTag("generate_travel_plan_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highContrast) Color.White else Color(0xFF10B981), // FIFA Green
                        contentColor = Color.White
                    )
                ) {
                    if (isGeneratingAdvice) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optimizing Transit...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.DirectionsTransit, contentDescription = "AI", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Match Day Travel Plan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Live AI Travel Plan Response Banner ---
        if (isGeneratingAdvice) {
            com.example.ui.components.TravelRouteSkeleton()
        }

        if (travelAdvice.isNotEmpty() && !isGeneratingAdvice) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = "Verified AI Plan", tint = Color(0xFF10B981))
                        Text(
                            text = "AI MATCH CO-PILOT TRAVEL PLAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF10B981), // FIFA Green
                                fontWeight = FontWeight.Bold,
                                fontSize = (10 * textScale).sp
                            )
                        )
                    }
                    Text(
                        text = travelAdvice,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            lineHeight = 18.sp,
                            fontSize = (13 * textScale).sp
                        )
                    )
                }
            }
        }

        // --- Live Schedules & Timetable ---
        Text(
            text = "LIVE TRANSIT MODES & SCHEDULES",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        transits.forEach { mode ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (mode.name.contains("Rideshare")) Color(0xFFFBBF24).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.name,
                                tint = if (mode.name.contains("Rideshare")) Color(0xFFFBBF24) else Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = mode.name,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = (14 * textScale).sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = mode.frequency,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MutedColor(highContrast),
                                        fontSize = (12 * textScale).sp
                                    )
                                )
                                Text(
                                    text = "• ${mode.cost}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MutedColor(highContrast),
                                        fontSize = (12 * textScale).sp
                                    )
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (mode.waitTimeMinutes <= 6) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFFBBF24).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Wait: ${mode.waitTimeMinutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (mode.waitTimeMinutes <= 6) Color(0xFF10B981) else Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (10 * textScale).sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mode.co2Savings,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF10B981), // FIFA Green
                                fontWeight = FontWeight.Bold,
                                fontSize = (10 * textScale).sp
                            )
                        )
                    }
                }
            }
        }
    }
}
