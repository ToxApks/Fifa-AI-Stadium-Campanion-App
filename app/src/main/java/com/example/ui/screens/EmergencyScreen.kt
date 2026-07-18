package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.Severity
import com.example.data.TtsManager
import com.example.data.Language
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen() {
    val isDarkMode by AppState.isDarkMode.collectAsState()
    val textColor = if (isDarkMode) Color.White else Color(0xFF131315)
    val subTextColor = if (isDarkMode) Color.LightGray else Color(0xFF4B5563)
    val dividerColor = if (isDarkMode) Color(0x22FFFFFF) else Color(0x22000000)
    val optionUnselectedBg = if (isDarkMode) Color(0x11FFFFFF) else Color(0x11000000)

    val coroutineScope = rememberCoroutineScope()
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()
    val currentLanguage by AppState.currentLanguage.collectAsState()
    val isTtsSpeaking by TtsManager.isSpeaking.collectAsState()
    val currentSpokenText by TtsManager.currentSpokenText.collectAsState()

    var incidentTitle by remember { mutableStateOf("") }
    var incidentDesc by remember { mutableStateOf("") }
    var incidentLocation by remember { mutableStateOf("Concourse Sec 114") }
    var incidentSeverity by remember { mutableStateOf(Severity.MEDIUM) }

    var isReporting by remember { mutableStateOf(false) }
    var reportFeedback by remember { mutableStateOf("") }

    // First Aid instructions
    var selectedCondition by remember { mutableStateOf("") }
    var medicalInstructions by remember { mutableStateOf("") }
    var isGeneratingInstructions by remember { mutableStateOf(false) }

    fun submitIncident() {
        if (incidentTitle.isBlank()) return
        isReporting = true
        com.example.data.DatabaseService.addIncident(incidentTitle, incidentDesc, incidentLocation, incidentSeverity)
        incidentTitle = ""
        incidentDesc = ""
        isReporting = false
        reportFeedback = "🚨 Incident successfully transmitted to MetLife Command Center and active Volunteers!"
    }

    fun requestMedicalInstructions(condition: String) {
        selectedCondition = condition
        isGeneratingInstructions = true
        coroutineScope.launch {
            val prompt = "Provide clear, professional, step-by-step first-aid guidance for treating $condition in a highly congested stadium seating section. Focus on safety and instructions while waiting for professional medical teams."
            medicalInstructions = GeminiRepository.generateResponse(prompt)
            isGeneratingInstructions = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("emergency_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Column {
            Text(
                text = "METLIFE CRISIS COMMAND",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = EmergencyRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * textScale).sp
                )
            )
            Text(
                text = "Incident Report & Assistance",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textScale).sp,
                    color = textColor
                )
            )
        }

        // --- Instant Reporting Form ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Report Active Incident",
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = (15 * textScale).sp
                )

                OutlinedTextField(
                    value = incidentTitle,
                    onValueChange = { incidentTitle = it },
                    label = { Text("Incident Title (e.g. Lost child, Slip hazard)") },
                    modifier = Modifier.fillMaxWidth().testTag("incident_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = EmergencyRed,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = incidentDesc,
                    onValueChange = { incidentDesc = it },
                    label = { Text("Details & Description") },
                    modifier = Modifier.fillMaxWidth().testTag("incident_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = EmergencyRed,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = incidentLocation,
                    onValueChange = { incidentLocation = it },
                    label = { Text("Specific Location (e.g. Concourse Sec 114)") },
                    modifier = Modifier.fillMaxWidth().testTag("incident_location_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = EmergencyRed,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                // Severity Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Severity:", color = textColor, fontSize = (13 * textScale).sp)
                    Severity.values().forEach { sev ->
                        val isSelected = sev == incidentSeverity
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(sev.color).copy(alpha = 0.3f) else optionUnselectedBg)
                                .clickable { incidentSeverity = sev }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sev.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSelected) Color(sev.color) else textColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = (11 * textScale).sp
                                )
                            )
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = { submitIncident() },
                    modifier = Modifier.fillMaxWidth().testTag("submit_incident_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Submit", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Immediate Alert", fontWeight = FontWeight.Bold)
                }

                if (reportFeedback.isNotEmpty()) {
                    Text(
                        text = reportFeedback,
                        color = AccentEmerald,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (12 * textScale).sp
                        )
                    )
                }
            }
        }

        // --- First Aid Medical Assistant ---
        Text(
            text = "AI FIRST AID PROTOCOLS GUIDE",
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
                    text = "Request AI CPR / First Aid Instructions:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = subTextColor,
                        fontSize = (12 * textScale).sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cardiac Arrest (CPR)", "Severe Heat Stroke", "Allergic Shock / EpiPen").forEach { condition ->
                        val isSel = selectedCondition == condition
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) EmergencyRed.copy(alpha = 0.25f) else optionUnselectedBg)
                                .clickable { requestMedicalInstructions(condition) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = condition.substringBefore(" "),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSel) EmergencyRed else textColor,
                                    fontSize = (11 * textScale).sp
                                )
                            )
                        }
                    }
                }

                if (isGeneratingInstructions) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally), color = EmergencyRed)
                } else if (medicalInstructions.isNotEmpty()) {
                    Divider(color = dividerColor, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = "Medical Instructions", tint = EmergencyRed)
                            Text(
                                text = "AI RESPONSE PROTOCOL:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmergencyRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (10 * textScale).sp
                                )
                            )
                        }
                        
                        val isThisMedicalSpeaking = isTtsSpeaking && currentSpokenText == medicalInstructions
                        IconButton(
                            onClick = {
                                if (isThisMedicalSpeaking) {
                                    TtsManager.stop()
                                } else {
                                    TtsManager.speak(medicalInstructions, currentLanguage)
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("narrate_emergency_btn")
                        ) {
                            Icon(
                                imageVector = if (isThisMedicalSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isThisMedicalSpeaking) "Stop Narration" else "Narrate First Aid instructions",
                                tint = EmergencyRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = medicalInstructions,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = textColor,
                            lineHeight = 18.sp,
                            fontSize = (13 * textScale).sp
                        )
                    )
                }
            }
        }
    }
}
