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
import com.example.data.IncidentStatus
import com.example.data.TtsManager
import com.example.data.Language
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerScreen() {
    val coroutineScope = rememberCoroutineScope()
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()
    val currentLanguage by AppState.currentLanguage.collectAsState()
    val isTtsSpeaking by TtsManager.isSpeaking.collectAsState()
    val currentSpokenText by TtsManager.currentSpokenText.collectAsState()

    val tasks by com.example.data.DatabaseService.volunteerTasks.collectAsState()
    val incidents by com.example.data.DatabaseService.incidents.collectAsState()

    // AI Announcement Generator
    var newsSubject by remember { mutableStateOf("Lot E Rideshare Congestion") }
    var generatedAnnouncement by remember { mutableStateOf("") }
    var isGeneratingAnnouncement by remember { mutableStateOf(false) }

    fun generateBroadcastAnnouncement() {
        isGeneratingAnnouncement = true
        coroutineScope.launch {
            val prompt = "Generate a highly professional, polite stadium PA broadcast announcement based on: '$newsSubject'. Keep it clear, concise, and structured for crowd safety."
            val result = GeminiRepository.generateResponse(prompt)
            generatedAnnouncement = result
            isGeneratingAnnouncement = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("volunteer_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Column {
            Text(
                text = "VOLUNTEER FORCE COMMAND",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * textScale).sp
                )
            )
            Text(
                text = "Volunteer Control Center",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textScale).sp,
                    color = Color.White
                )
            )
        }

        // --- Volunteer Task Checklist ---
        Text(
            text = "ASSIGNED OPERATIONS TASKS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        tasks.forEach { task ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            color = if (task.isCompleted) Color.Gray else Color.White,
                            fontSize = (14 * textScale).sp
                        )
                        Text(
                            text = "Location: ${task.location} • ${task.description}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (task.isCompleted) Color.DarkGray else Color.LightGray,
                                fontSize = (11 * textScale).sp
                            )
                        )
                    }

                    if (task.isCompleted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentEmerald.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Completed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (10 * textScale).sp
                                )
                            )
                        }
                    } else {
                        Button(
                            onClick = { com.example.data.DatabaseService.completeVolunteerTask(task.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("complete_task_${task.id}")
                        ) {
                            Text("Resolve", fontSize = (10 * textScale).sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Incident Management Triage ---
        Text(
            text = "LIVE ACTIVE EMERGENCIES TRIAGE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        incidents.forEach { incident ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = incident.title,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (14 * textScale).sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(incident.severity.color).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = incident.severity.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(incident.severity.color),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (10 * textScale).sp
                                )
                            )
                        }
                    }

                    Text(
                        text = "Location: ${incident.location} • ${incident.description}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.LightGray,
                            fontSize = (12 * textScale).sp
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${incident.status}",
                            fontSize = (11 * textScale).sp,
                            color = WarningOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (incident.status != IncidentStatus.RESOLVED) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x22FFFFFF))
                                    .clickable { com.example.data.DatabaseService.updateIncidentStatus(incident.id, IncidentStatus.RESOLVED) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Mark Resolved", fontSize = (10 * textScale).sp, color = AccentEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- AI Broadcast Announcement Generator ---
        Text(
            text = "AI BROADCAST GENERATOR",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newsSubject,
                    onValueChange = { newsSubject = it },
                    label = { Text("Topic for announcement") },
                    modifier = Modifier.fillMaxWidth().testTag("news_subject_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Button(
                    onClick = { generateBroadcastAnnouncement() },
                    modifier = Modifier.fillMaxWidth().testTag("generate_announcement_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highContrast) Color.White else AccentCyan,
                        contentColor = Color.Black
                    )
                ) {
                    if (isGeneratingAnnouncement) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                    } else {
                        Icon(Icons.Default.VolumeUp, contentDescription = "PA", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draft AI PA Broadcast Announcement", fontWeight = FontWeight.Bold)
                    }
                }

                if (generatedAnnouncement.isNotEmpty()) {
                    Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "APPROVED BROADCAST SCRIPT:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = (10 * textScale).sp
                            )
                        )
                        
                        val isThisScriptSpeaking = isTtsSpeaking && currentSpokenText == generatedAnnouncement
                        IconButton(
                            onClick = {
                                if (isThisScriptSpeaking) {
                                    TtsManager.stop()
                                } else {
                                    TtsManager.speak(generatedAnnouncement, currentLanguage)
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("narrate_announcement_btn")
                        ) {
                            Icon(
                                imageVector = if (isThisScriptSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isThisScriptSpeaking) "Stop Narration" else "Narrate Script",
                                tint = AccentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = generatedAnnouncement,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            lineHeight = 16.sp,
                            fontSize = (12 * textScale).sp
                        )
                    )
                }
            }
        }
    }
}
