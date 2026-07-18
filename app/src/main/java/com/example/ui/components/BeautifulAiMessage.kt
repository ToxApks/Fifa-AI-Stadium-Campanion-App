package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import org.json.JSONArray
import org.json.JSONObject

// Structured data class representing parsed AI replies
data class StructuredAiResponse(
    val title: String,
    val summary: String,
    val explanation: String,
    val recommendedActions: List<String>,
    val confidenceScore: String,
    val sources: List<String>
) {
    companion object {
        fun fromJson(text: String): StructuredAiResponse? {
            val clean = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            if (!clean.startsWith("{") || !clean.endsWith("}")) {
                return null
            }
            return try {
                val obj = JSONObject(clean)
                val title = obj.optString("title", "AI DECISION SUPPORT REPORT")
                val summary = obj.optString("summary", "")
                val explanation = obj.optString("explanation", "")
                
                val recActions = mutableListOf<String>()
                val actionsArr = obj.optJSONArray("recommendedActions")
                if (actionsArr != null) {
                    for (i in 0 until actionsArr.length()) {
                        recActions.add(actionsArr.getString(i))
                    }
                }
                
                val confidenceScore = obj.optString("confidenceScore", "95% Grounded AI")
                
                val sourcesList = mutableListOf<String>()
                val sourcesArr = obj.optJSONArray("sources")
                if (sourcesArr != null) {
                    for (i in 0 until sourcesArr.length()) {
                        sourcesList.add(sourcesArr.getString(i))
                    }
                }
                
                StructuredAiResponse(
                    title = title,
                    summary = summary,
                    explanation = explanation,
                    recommendedActions = recActions,
                    confidenceScore = confidenceScore,
                    sources = sourcesList
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Composable
fun BeautifulAiMessage(text: String, textScale: Float) {
    val structuredResponse = remember(text) {
        StructuredAiResponse.fromJson(text)
    }

    if (structuredResponse != null) {
        StructuredResponseView(response = structuredResponse, textScale = textScale)
    } else {
        LegacyBeautifulAiMessage(text = text, textScale = textScale)
    }
}

@Composable
fun StructuredResponseView(response: StructuredAiResponse, textScale: Float) {
    val isDarkMode = com.example.data.AppState.isDarkMode.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Gold Title Header with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Intel Report",
                tint = Color(0xFFC5A059),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = response.title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFFC5A059), // Elegant Gold
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    fontSize = (11 * textScale).sp
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // 2. Highlighted Summary Banner Card
        if (response.summary.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFF3F4F6))
                    .border(1.dp, if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Summary",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = response.summary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDarkMode) Color.White else Color(0xFF131315),
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp,
                            fontSize = (13 * textScale).sp
                        )
                    )
                }
            }
        }

        // 3. Explanation Section
        if (response.explanation.isNotEmpty()) {
            Text(
                text = response.explanation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDarkMode) Color.LightGray else Color(0xFF4B5563),
                    lineHeight = 20.sp,
                    fontSize = (13.5 * textScale).sp
                )
            )
        }

        // 4. Recommended Actions (Interactive Checklist Cards with 48dp target)
        if (response.recommendedActions.isNotEmpty()) {
            Text(
                text = "RECOMMENDED ACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = (10 * textScale).sp
                )
            )

            response.recommendedActions.forEachIndexed { index, action ->
                var checked by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (checked) Color(0x1100C853) else if (isDarkMode) Color(0xFF161618) else Color(0xFFFFFFFF))
                        .border(
                            1.dp, 
                            if (checked) Color(0x4400C853) else if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), 
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { checked = !checked }
                        .padding(horizontal = 12.dp, vertical = 12.dp) // Generous padding to ensure 48dp+ height
                        .defaultMinSize(minHeight = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentEmerald,
                            checkmarkColor = Color.Black
                        )
                    )
                    Text(
                        text = action,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (checked) Color.Gray else if (isDarkMode) Color.White else Color(0xFF131315),
                            fontWeight = if (checked) FontWeight.Normal else FontWeight.Medium,
                            fontSize = (13 * textScale).sp,
                            lineHeight = 17.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Decision Confidence Meter Progress Bar
        val percentage = remember(response.confidenceScore) {
            response.confidenceScore.filter { it.isDigit() }.toIntOrNull() ?: 95
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDarkMode) Color(0xFF161618) else Color(0xFFFFFFFF))
                .border(1.dp, if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, 
                    contentDescription = "Confidence", 
                    tint = AccentEmerald, 
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AI SYSTEM CONFIDENCE", 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF8E8E93), 
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = response.confidenceScore, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Black, 
                            color = AccentEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = percentage.toFloat() / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = AccentEmerald,
                        trackColor = if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0)
                    )
                }
            }
        }

        // 6. Information Sources Chips List
        if (response.sources.isNotEmpty()) {
            Text(
                text = "INFORMATION SOURCING",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = (10 * textScale).sp
                )
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(response.sources) { src ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFF3F4F6))
                            .border(1.dp, if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Source",
                                tint = AccentCyan,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = src.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color(0xFF131315)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegacyBeautifulAiMessage(text: String, textScale: Float) {
    val lines = text.split("\n")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        lines.forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach

            // 1. Detect Confidence Meter
            if (trimmedLine.contains("Confidence", ignoreCase = true) && trimmedLine.contains("%")) {
                val percentage = trimmedLine.filter { it.isDigit() }.toIntOrNull() ?: 95
                ConfidenceMeterCard(percentage = percentage, textScale = textScale)
            }
            // 2. Detect Titles / Headers
            else if (trimmedLine.startsWith("**") && trimmedLine.endsWith("**") && trimmedLine.length < 60) {
                val cleanTitle = trimmedLine.replace("**", "")
                HeaderSection(title = cleanTitle, textScale = textScale)
            }
            else if (trimmedLine.startsWith("###") || trimmedLine.startsWith("##") || trimmedLine.startsWith("#")) {
                val cleanTitle = trimmedLine.replace("#", "").trim()
                HeaderSection(title = cleanTitle, textScale = textScale)
            }
            // 3. Detect Bullet points
            else if (trimmedLine.startsWith("•") || trimmedLine.startsWith("-") || trimmedLine.startsWith("*")) {
                val cleanBullet = trimmedLine.substring(1).replace("**", "").trim()
                BulletPointRow(text = cleanBullet, textScale = textScale)
            }
            // 4. Recommendation Cards
            else if (trimmedLine.contains("Recommendation:", ignoreCase = true) || trimmedLine.contains("Action:", ignoreCase = true) || trimmedLine.contains("Suggested Action:", ignoreCase = true)) {
                val cleanRec = trimmedLine.replace("**", "")
                RecommendationCard(text = cleanRec, textScale = textScale)
            }
            // 5. Standard line
            else {
                StandardParsedText(rawText = trimmedLine, textScale = textScale)
            }
        }
    }
}

@Composable
fun ConfidenceMeterCard(percentage: Int, textScale: Float) {
    val isDarkMode = com.example.data.AppState.isDarkMode.collectAsState().value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkMode) Color(0xFF161618) else Color(0xFFFFFFFF))
            .border(1.dp, if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "Confidence", tint = Color(0xFFC5A059), modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("AI DECISION CONFIDENCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5A059), letterSpacing = 0.5.sp)
                    Text("$percentage%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Color(0xFF131315))
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = percentage.toFloat() / 100f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = AccentCyan,
                    trackColor = if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun HeaderSection(title: String, textScale: Float) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color(0xFFC5A059), 
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                fontSize = (11 * textScale).sp
            )
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(AccentCyan)
        )
    }
}

@Composable
fun BulletPointRow(text: String, textScale: Float) {
    val isDarkMode = com.example.data.AppState.isDarkMode.collectAsState().value
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentCyan)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isDarkMode) Color.LightGray else Color(0xFF4B5563),
                lineHeight = 18.sp,
                fontSize = (13 * textScale).sp
            )
        )
    }
}

@Composable
fun RecommendationCard(text: String, textScale: Float) {
    val isDarkMode = com.example.data.AppState.isDarkMode.collectAsState().value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AccentEmerald.copy(alpha = if (isDarkMode) 0.08f else 0.12f))
            .border(1.dp, AccentEmerald.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Recommendation", tint = AccentEmerald, modifier = Modifier.size(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isDarkMode) Color.White else Color(0xFF131315),
                    lineHeight = 18.sp,
                    fontSize = (13 * textScale).sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun StandardParsedText(rawText: String, textScale: Float) {
    val isDarkMode = com.example.data.AppState.isDarkMode.collectAsState().value
    val cleanText = rawText.replace("**", "").replace("_", "")
    Text(
        text = cleanText,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = if (isDarkMode) Color.White else Color(0xFF131315),
            lineHeight = 19.sp,
            fontSize = (13.5 * textScale).sp
        )
    )
}
