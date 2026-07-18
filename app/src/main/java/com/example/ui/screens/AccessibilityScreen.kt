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
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen() {
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()
    val isDarkMode by AppState.isDarkMode.collectAsState()
    val voiceEnabled by AppState.voiceOutputEnabled.collectAsState()

    val textColor = if (isDarkMode) Color.White else Color(0xFF131315)
    val dividerColor = if (isDarkMode) Color(0x22FFFFFF) else Color(0x22000000)

    var voiceSimulationCommand by remember { mutableStateOf("") }
    var voiceSimulationResult by remember { mutableStateOf("") }

    val refereeSigns = listOf(
        Pair("VAR Review (Rectangle Hand Box)", "A rectangle box drawn in the air. Indicates the referee is initiating a video review of an incident."),
        Pair("Direct Free Kick (Pointing arm)", "The referee points their arm directly in the direction of the team taking the free kick."),
        Pair("Corner Kick (Pointing corner)", "The referee points their arm upwards towards the corner of the soccer pitch.")
    )

    fun simulateVoiceCommand(cmd: String) {
        voiceSimulationCommand = cmd
        voiceSimulationResult = when (cmd) {
            "Nearest elevator" -> "Elevator Core B is located 40 meters east behind Section 114 concourse. It takes you directly to Level 1 and Level 3."
            "Call medical assistance" -> "ALERT RED INITIALIZED. Dispatching local medical squad Bravo directly to Section 114, Row M. Please stay where you are."
            "Show eco carbon points" -> "You currently have 320 Eco-Points. Return 1 more cup to earn 50 additional carbon credits!"
            else -> ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("accessibility_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Column {
            Text(
                text = "INCLUSIVE DESIGN CENTRE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * textScale).sp
                )
            )
            Text(
                text = "Accessibility Tools & Controls",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textScale).sp,
                    color = textColor
                )
            )
        }

        // --- Core Overrides Panel ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Aesthetic & Legibility Controls",
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = (14 * textScale).sp
                )

                // High Contrast Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Contrast, contentDescription = "Contrast", tint = AccentCyan)
                        Text("High Contrast Mode (OLED Black)", fontSize = (13 * textScale).sp, color = textColor)
                    }
                    Switch(
                        checked = highContrast,
                        onCheckedChange = { AppState.highContrastMode.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan),
                        modifier = Modifier.testTag("high_contrast_switch")
                    )
                }

                Divider(color = dividerColor)

                // Dark Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme Mode",
                            tint = AccentCyan
                        )
                        Text("Dark Theme (Night / Stadium)", fontSize = (13 * textScale).sp, color = textColor)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { AppState.isDarkMode.value = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan),
                        modifier = Modifier.testTag("dark_mode_toggle_switch")
                    )
                }

                Divider(color = dividerColor)

                // Text Scale Font Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Text Size", tint = AccentCyan)
                            Text("Dynamic Heading Text Scale", fontSize = (13 * textScale).sp, color = textColor)
                        }
                        Text(
                            text = "${(textScale * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            fontSize = (13 * textScale).sp
                        )
                    }
                    Slider(
                        value = textScale,
                        onValueChange = { AppState.textScale.value = it },
                        valueRange = 0.8f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = AccentCyan
                        ),
                        modifier = Modifier.testTag("text_scale_slider")
                    )
                }
            }
        }

        // --- Interactive Speech Command Simulator ---
        Text(
            text = "AI VOICE COMMANDS SIMULATOR",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tap a speech command to simulate spoken voice:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MutedColor(highContrast),
                        fontSize = (12 * textScale).sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Nearest elevator", "Call medical assistance", "Show eco carbon points").forEach { cmd ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (voiceSimulationCommand == cmd) AccentCyan.copy(alpha = 0.25f) else if (isDarkMode) Color(0x11FFFFFF) else Color(0x11000000))
                                .clickable { simulateVoiceCommand(cmd) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cmd.substringBefore(" "),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (voiceSimulationCommand == cmd) AccentCyan else textColor,
                                    fontSize = (11 * textScale).sp
                                )
                            )
                        }
                    }
                }

                if (voiceSimulationResult.isNotEmpty()) {
                    Divider(color = dividerColor, modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "AI SPEECH ENGINE INTERPRETATION:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10 * textScale).sp
                        )
                    )
                    Text(
                        text = "\"$voiceSimulationResult\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            lineHeight = 18.sp,
                            fontSize = (13 * textScale).sp
                        )
                    )
                }
            }
        }

        // --- Referee Sign Language Translation Guide ---
        Text(
            text = "REFEREES SIGN LANGUAGE GUIDE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        refereeSigns.forEach { sign ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = sign.first,
                        tint = AccentEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = sign.first,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = (14 * textScale).sp
                        )
                        Text(
                            text = sign.second,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                lineHeight = 16.sp,
                                fontSize = (12 * textScale).sp
                            )
                        )
                    }
                }
            }
        }
    }
}
