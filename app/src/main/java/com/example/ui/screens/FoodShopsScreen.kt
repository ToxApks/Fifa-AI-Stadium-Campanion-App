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
import com.example.ui.components.GlassCard
import com.example.ui.components.AnimatedStaggeredEntry
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodShopsScreen() {
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()
    
    val rewardPoints by AppState.ecoRewardPoints.collectAsState()
    val carbonSaved by AppState.carbonSavedKg.collectAsState()

    var showRecycleSuccess by remember { mutableStateOf(false) }

    val washrooms = listOf(
        Pair("Concourse Sec 112 Washrooms", "2 min wait • Accessible"),
        Pair("Concourse Sec 122 Washrooms", "7 min wait • Baby changing"),
        Pair("Upper Tier Sec 218 Washrooms", "1 min wait • Accessible")
    )

    fun handleRecycleReturn() {
        AppState.ecoRewardPoints.value = rewardPoints + 50
        AppState.carbonSavedKg.value = carbonSaved + 0.3
        showRecycleSuccess = true
    }

    var animIndex = 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("food_shops_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        AnimatedStaggeredEntry(index = animIndex++) {
            Column {
                Text(
                    text = "GREEN CONCESSIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AccentEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = (10 * textScale).sp
                    )
                )
                Text(
                    text = "Food, Shops & Zero-Waste",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = (22 * textScale).sp,
                        color = Color.White
                    )
                )
            }
        }

        // --- Sustainability Points Card ---
        AnimatedStaggeredEntry(index = animIndex++) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your FIFA Green Wallet",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (15 * textScale).sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentEmerald.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Level 2 Champion",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (10 * textScale).sp
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ECO REWARD POINTS", fontSize = (10 * textScale).sp, color = Color.Gray)
                            Text("$rewardPoints pts", fontSize = (22 * textScale).sp, fontWeight = FontWeight.Black, color = AccentEmerald)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("CARBON SAVINGS", fontSize = (10 * textScale).sp, color = Color.Gray)
                            Text("${String.format("%.1f", carbonSaved)} kg CO2", fontSize = (22 * textScale).sp, fontWeight = FontWeight.Black, color = AccentCyan)
                        }
                    }

                    Divider(color = Color(0x22FFFFFF))

                    // Interactive recycling simulation button!
                    Button(
                        onClick = { handleRecycleReturn() },
                        modifier = Modifier.fillMaxWidth().testTag("return_reusable_cup_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (highContrast) Color.White else AccentEmerald,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Recycling, contentDescription = "Recycling", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Return Reusable Cup (+50 pts)", fontWeight = FontWeight.Bold)
                    }

                    if (showRecycleSuccess) {
                        Text(
                            text = "♻️ Thank you for keeping MetLife green! 50 points added and carbon savings incremented.",
                            color = AccentCyan,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = (11 * textScale).sp
                            )
                        )
                    }
                }
            }
        }

        // --- Zero-Waste Food Stalls ---
        AnimatedStaggeredEntry(index = animIndex++) {
            Text(
                text = "ZERO-WASTE FOOD PARTNERS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutedColor(highContrast),
                    letterSpacing = 1.sp,
                    fontSize = (11 * textScale).sp
                )
            )
        }

        AppState.foodStalls.forEach { stall ->
            AnimatedStaggeredEntry(index = animIndex++) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        .background(AccentCyan.copy(alpha = 0.15f))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestaurantMenu,
                                        contentDescription = stall.name,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stall.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = (14 * textScale).sp
                                    )
                                    Text(
                                        text = "${stall.category} • ${stall.location}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MutedColor(highContrast),
                                            fontSize = (12 * textScale).sp
                                        )
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (stall.waitTimeMinutes <= 5) AccentEmerald.copy(alpha = 0.2f) else WarningOrange.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${stall.waitTimeMinutes}m wait",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (stall.waitTimeMinutes <= 5) AccentEmerald else WarningOrange,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (10 * textScale).sp
                                        )
                                    )
                                }
                                if (stall.hasRecyclingBonus) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Eco Bonus Active",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AccentEmerald,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (10 * textScale).sp
                                        )
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0x11FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Meal Option: $14.50",
                                color = Color.LightGray,
                                fontSize = (12 * textScale).sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Button(
                                onClick = {
                                    com.example.data.DatabaseService.addFoodOrder(
                                        stallName = stall.name,
                                        itemName = "Eco Signature Combo",
                                        quantity = 1,
                                        totalPrice = 14.50,
                                        carbonSavings = stall.carbonSavingsKg
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (stall.hasRecyclingBonus) AccentEmerald else AccentCyan,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("order_${stall.name.replace(" ", "_")}")
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = "Order", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Quick Order", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Washroom Queue Finder ---
        AnimatedStaggeredEntry(index = animIndex++) {
            Text(
                text = "NEAREST HYGIENE & WASHROOMS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutedColor(highContrast),
                    letterSpacing = 1.sp,
                    fontSize = (11 * textScale).sp
                )
            )
        }

        washrooms.forEach { washroom ->
            AnimatedStaggeredEntry(index = animIndex++) {
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
                                    imageVector = Icons.Default.Wc,
                                    contentDescription = washroom.first,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = washroom.first,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = (13 * textScale).sp
                            )
                        }
                        Text(
                            text = washroom.second,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AccentCyan,
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
