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
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostFoundScreen() {
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()

    val items by com.example.data.DatabaseService.lostItems.collectAsState()

    var itemName by remember { mutableStateOf("") }
    var itemCategory by remember { mutableStateOf("Electronics") }
    var itemLocation by remember { mutableStateOf("") }
    var itemDesc by remember { mutableStateOf("") }

    var reportFeedback by remember { mutableStateOf("") }

    val categories = listOf("Electronics", "Personal Valuables", "Souvenirs & Apparel", "Documents & Tickets")

    fun submitLostReport() {
        if (itemName.isBlank() || itemLocation.isBlank()) return
        com.example.data.DatabaseService.addLostItem(itemName, itemCategory, itemLocation, itemDesc)
        itemName = ""
        itemLocation = ""
        itemDesc = ""
        reportFeedback = "🎒 Case registered successfully. FIFA Lost & Found team alerted!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("lost_found_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        Column {
            Text(
                text = "RETRIEVAL LOGISTICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = (10 * textScale).sp
                )
            )
            Text(
                text = "Lost & Found Tracker",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textScale).sp,
                    color = Color.White
                )
            )
        }

        // --- Report New Lost Item ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "File a Lost Item Report",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = (15 * textScale).sp
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name (e.g. Leather Wallet, Glasses)") },
                    modifier = Modifier.fillMaxWidth().testTag("lost_item_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category:", color = Color.White, fontSize = (12 * textScale).sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            val isSel = cat == itemCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) AccentCyan.copy(alpha = 0.25f) else Color(0x11FFFFFF))
                                    .clickable { itemCategory = cat }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.substringBefore(" "),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSel) AccentCyan else Color.White,
                                        fontSize = (11 * textScale).sp
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = itemLocation,
                    onValueChange = { itemLocation = it },
                    label = { Text("Last Seen Location (e.g. Sec 114, Row F)") },
                    modifier = Modifier.fillMaxWidth().testTag("lost_item_location_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                OutlinedTextField(
                    value = itemDesc,
                    onValueChange = { itemDesc = it },
                    label = { Text("Physical Description (Color, markings...)") },
                    modifier = Modifier.fillMaxWidth().testTag("lost_item_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                // Submit button
                Button(
                    onClick = { submitLostReport() },
                    modifier = Modifier.fillMaxWidth().testTag("submit_lost_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highContrast) Color.White else AccentCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Report", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register Lost Item Case", fontWeight = FontWeight.Bold)
                }

                if (reportFeedback.isNotEmpty()) {
                    Text(
                        text = reportFeedback,
                        color = AccentEmerald,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (11 * textScale).sp
                        )
                    )
                }
            }
        }

        // --- Active Lost Case Feed ---
        Text(
            text = "ACTIVE REPORTED LOST & FOUND CASES",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        items.forEach { case ->
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
                                .background(Color(0x11FFFFFF))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = case.itemName,
                                tint = AccentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = case.itemName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = (14 * textScale).sp
                            )
                            Text(
                                text = "Last seen: ${case.lastSeenLocation} • ${case.category}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MutedColor(highContrast),
                                    fontSize = (11 * textScale).sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(WarningOrange.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = case.status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WarningOrange,
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
