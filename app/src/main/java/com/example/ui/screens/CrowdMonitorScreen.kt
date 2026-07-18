package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppState
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.WarningOrange
import kotlin.math.cos
import kotlin.math.sin

data class CrowdSection(
    val id: String,
    val sectorName: String,
    val densityPercentage: Int,
    val color: Color,
    val recommendedExit: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrowdMonitorScreen() {
    val isDarkMode by AppState.isDarkMode.collectAsState()
    val textColor = if (isDarkMode) Color.White else Color(0xFF131315)
    val subTextColor = if (isDarkMode) Color.LightGray else Color(0xFF4B5563)
    val dividerColor = if (isDarkMode) Color(0x22FFFFFF) else Color(0x22000000)
    val optionUnselectedBg = if (isDarkMode) Color(0x11FFFFFF) else Color(0x11000000)

    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()

    var activeSectorFilter by remember { mutableStateOf("All Sectors") }
    
    val sections = listOf(
        CrowdSection("1", "Sec 100-110 (North Entrance)", 88, EmergencyRed, "Gate A"),
        CrowdSection("2", "Sec 111-120 (East Concourse)", 42, AccentEmerald, "Gate C"),
        CrowdSection("3", "Sec 121-130 (South Plaza)", 71, WarningOrange, "Gate B"),
        CrowdSection("4", "Sec 200-220 (Upper Tier East)", 24, AccentEmerald, "Gate C Elevator"),
        CrowdSection("5", "Sec 221-240 (Upper Tier West)", 63, WarningOrange, "Gate A Ramp")
    )
    
    var selectedSection by remember { mutableStateOf(sections[0]) }

    // Match volunteer assignments to wait times
    val waitA = AppState.getGateWaitTime("Gate A (North)")
    val waitB = AppState.getGateWaitTime("Gate B (South)")
    val waitC = AppState.getGateWaitTime("Gate C (East)")

    val infiniteTransition = rememberInfiniteTransition(label = "crowd_anims")
    
    // Pulse animation for heatmaps
    val heatPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heatPulse"
    )

    // Decibel indicator pulsing oscillation (representing live sound level)
    val dbValueOscillation by infiniteTransition.animateFloat(
        initialValue = 92.4f,
        targetValue = 96.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dbValue"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("crowd_monitor_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Block ---
        Column {
            Text(
                text = "REAL-TIME CROWD INTELLIGENCE COMMAND",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * textScale).sp,
                    letterSpacing = 1.5.sp
                )
            )
            Text(
                text = "Density Metrics & Queue Solvers",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textScale).sp,
                    color = textColor
                )
            )
        }

        // --- Live High-Fidelity Decibel Spectral Sensor Card ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Custom Decibel Indicator Spectrum Graph
                    Canvas(modifier = Modifier.size(50.dp)) {
                        val midX = size.width / 2f
                        val midY = size.height / 2f
                        // Outer arc circle
                        drawCircle(
                            color = dividerColor,
                            radius = size.width / 2f,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        // Colored active decibel reading arc
                        drawArc(
                            color = WarningOrange,
                            startAngle = 135f,
                            sweepAngle = 270f * (dbValueOscillation / 120f),
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    Column {
                        Text(
                            text = "LIVE NOISE DECIBEL SENSOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WarningOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = (10 * textScale).sp,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "${String.format("%.1f", dbValueOscillation)} dB - HIGH VOL",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                fontSize = (15 * textScale).sp
                            )
                        )
                        Text(
                            text = "Crowd energy peak level. Sensory warning active.",
                            fontSize = 10.sp,
                            color = subTextColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningOrange.copy(alpha = 0.15f))
                        .border(1.dp, WarningOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Sensory Room 101: Open",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarningOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10 * textScale).sp
                        )
                    )
                }
            }
        }

        // --- Interactive Heatmap Seating Ring Block Grid ---
        Text(
            text = "STADIUM HEATMAP SECTOR ANALYSIS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        // Segment Filter Toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All Sectors", "Lower Tier", "Upper Tier").forEach { opt ->
                val isSel = activeSectorFilter == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) AccentCyan.copy(alpha = 0.25f) else optionUnselectedBg)
                        .border(1.dp, if (isSel) AccentCyan.copy(alpha = 0.4f) else optionUnselectedBg, RoundedCornerShape(20.dp))
                        .clickable { activeSectorFilter = opt }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSel) AccentCyan else textColor,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = (11 * textScale).sp
                        )
                    )
                }
            }
        }

        // Vector Radial Seating Sectors Illustration (Interactive Canvas Selections!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDarkMode) Color(0xFF020617) else Color(0xFFF1F5F9))
                .border(1.dp, if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1A000000), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerRadius = 240f
                val innerRadius = 160f

                // Draw field background
                drawRect(
                    color = Color(0x1210B981),
                    topLeft = Offset(cx - 90f, cy - 60f),
                    size = Size(180f, 120f)
                )
                drawRect(
                    color = AccentEmerald.copy(alpha = 0.3f),
                    topLeft = Offset(cx - 90f, cy - 60f),
                    size = Size(180f, 120f),
                    style = Stroke(width = 3f)
                )

                // Draw concentric seating arcs with colored densities
                // Sector 1: Red heat (North)
                drawArc(
                    color = EmergencyRed.copy(alpha = if (selectedSection.id == "1") heatPulseAlpha else 0.4f),
                    startAngle = 210f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(cx - outerRadius, cy - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 40f)
                )

                // Sector 2: Emerald heat (East)
                drawArc(
                    color = AccentEmerald.copy(alpha = if (selectedSection.id == "2") heatPulseAlpha else 0.4f),
                    startAngle = 300f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(cx - outerRadius, cy - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 40f)
                )

                // Sector 3: Amber heat (South)
                drawArc(
                    color = WarningOrange.copy(alpha = if (selectedSection.id == "3") heatPulseAlpha else 0.4f),
                    startAngle = 50f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(cx - outerRadius, cy - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 40f)
                )

                // Inner ring seating
                drawCircle(
                    color = AccentCyan.copy(alpha = 0.25f),
                    center = Offset(cx, cy),
                    radius = innerRadius,
                    style = Stroke(width = 24f)
                )
            }

            // Interactive helper overlay text
            Text(
                text = "SECTOR HEAT MAP VIEW",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp)
            )
        }

        // Horizontal Row of click selector cards for individual sectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filteredList = when (activeSectorFilter) {
                "Lower Tier" -> sections.filter { !it.sectorName.contains("Upper") }
                "Upper Tier" -> sections.filter { it.sectorName.contains("Upper") }
                else -> sections
            }

            filteredList.take(3).forEach { sec ->
                val isSel = selectedSection.id == sec.id
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .border(1.dp, if (isSel) AccentCyan.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp)),
                    onClick = { selectedSection = sec }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SEC ${sec.id}00",
                            fontSize = (11 * textScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .fillMaxWidth()
                                .background(sec.color)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = "${sec.densityPercentage}% FULL",
                            fontSize = (10 * textScale).sp,
                            color = sec.color,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Selected section detail explanation card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            isAiTheme = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(selectedSection.color))
                    Text(
                        text = "Sector Focus: ${selectedSection.sectorName}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (14 * textScale).sp
                    )
                }
                Text(
                    text = "Density level is currently ${selectedSection.densityPercentage}%. Recommended egress strategy: Direct fans via ${selectedSection.recommendedExit} immediately following full time whistle to safely distribute exit vectors and bypass main concourse bottlenecks.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.LightGray,
                        fontSize = (13 * textScale).sp,
                        lineHeight = 17.sp
                    )
                )
            }
        }

        // --- Live Gate Queues (Linked with Organizer Volunteer sliders!) ---
        Text(
            text = "LIVE ENTRANCE GATE QUEUES",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        val gates = listOf(
            Triple("Gate A (North Entrance)", waitA, AppState.gateAVolunteers.collectAsState().value),
            Triple("Gate B (South Plaza)", waitB, AppState.gateBVolunteers.collectAsState().value),
            Triple("Gate C (East Concourse)", waitC, AppState.gateCVolunteers.collectAsState().value)
        )

        gates.forEach { gateTuple ->
            val (name, wait, volCount) = gateTuple
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (14 * textScale).sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentCyan))
                            Text(
                                text = "Optimized by $volCount volunteers",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = AccentCyan,
                                    fontSize = (12 * textScale).sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (wait <= 5) AccentEmerald.copy(alpha = 0.2f) else WarningOrange.copy(alpha = 0.2f))
                                .border(1.dp, if (wait <= 5) AccentEmerald.copy(alpha = 0.4f) else WarningOrange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$wait MIN WAIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (wait <= 5) AccentEmerald else WarningOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (11 * textScale).sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
