package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.AppState
import com.example.data.Stadium
import com.example.data.StadiumDatabase
import com.example.ui.components.GlassCard
import com.example.ui.components.TravelRouteSkeleton
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.EmergencyRed
import org.json.JSONArray
import org.json.JSONObject

data class GroundedAmenity(
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val distanceText: String,
    val details: String,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen() {
    val context = LocalContext.current
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()

    val activeStadium by StadiumDatabase.activeStadium.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isAccessibleOnly by remember { mutableStateOf(false) }
    var selectedTarget by remember { mutableStateOf("My Seat (Sec 114)") }
    var mapSource by remember { mutableStateOf("google_road") }
    var virtualDemoMode by remember { mutableStateOf(true) }

    var customTargetLat by remember { mutableStateOf<Double?>(null) }
    var customTargetLng by remember { mutableStateOf<Double?>(null) }

    var amenitySearchQuery by remember { mutableStateOf("") }
    var isAmenitySearching by remember { mutableStateOf(false) }
    var amenitySearchResults by remember { mutableStateOf<List<GroundedAmenity>>(emptyList()) }
    var amenitySearchError by remember { mutableStateOf<String?>(null) }

    // Official info accordion expanded states
    var expandedHistory by remember { mutableStateOf(false) }
    var expandedTournaments by remember { mutableStateOf(false) }
    var expandedTransit by remember { mutableStateOf(false) }
    var expandedHotels by remember { mutableStateOf(false) }
    var expandedContacts by remember { mutableStateOf(false) }

    // Dropdown for stadiums
    var showStadiumDropdown by remember { mutableStateOf(false) }

    // Interactive Map Overlays
    var showSeatingOverlay by remember { mutableStateOf(true) }
    var showConcessionsOverlay by remember { mutableStateOf(true) }
    var showFacilitiesOverlay by remember { mutableStateOf(true) }

    // Location States
    var gpsLocation by remember { mutableStateOf<Location?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            Toast.makeText(context, "GPS Location Access Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Location permission denied. Utilizing simulated mode.", Toast.LENGTH_LONG).show()
        }
    }

    // Register real-world GPS tracking
    DisposableEffect(hasLocationPermission, context) {
        if (hasLocationPermission) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    gpsLocation = location
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            try {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L,
                    2f,
                    listener
                )
                // Also request network fallback location for indoor accuracy
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    2f,
                    listener
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            onDispose {
                locationManager?.removeUpdates(listener)
            }
        } else {
            onDispose {}
        }
    }

    // Determine current user latitude & longitude based on simulated or real GPS location
    val userLat = if (virtualDemoMode) {
        // Center of parking lot offset relative to stadium
        activeStadium.latitude + 0.0020
    } else {
        gpsLocation?.latitude ?: (activeStadium.latitude + 0.0020)
    }

    val userLng = if (virtualDemoMode) {
        activeStadium.longitude - 0.0020
    } else {
        gpsLocation?.longitude ?: (activeStadium.longitude - 0.0020)
    }

    // Available target destinations
    val targets = listOf(
        "My Seat (Sec 114)",
        "Entrance Gate (Gate C)",
        "Nearest Parking (Lot A)",
        "First Aid Medical Center",
        "Eco Food Pavilion",
        "Nearest Restroom",
        "Merchandise Store",
        "Lost & Found Booth",
        "Main Exit Way",
        "Volunteer Help Desk"
    )

    // Calculate details for active target
    val routeDetails = remember(activeStadium, selectedTarget, isAccessibleOnly, userLat, userLng, customTargetLat, customTargetLng) {
        calculateRouteDetails(activeStadium, selectedTarget, isAccessibleOnly, userLat, userLng, customTargetLat, customTargetLng)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("navigation_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Title Block ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REAL-WORLD WAYFINDING & MAPS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFBBF24), // Gold
                        fontWeight = FontWeight.Bold,
                        fontSize = (10 * textScale).sp,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = "Stadium Match Day Navigator",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = (22 * textScale).sp,
                        color = Color.White
                    )
                )
            }
            // Live Status indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (virtualDemoMode) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f))
                    .border(1.dp, if (virtualDemoMode) Color(0xFF3B82F6) else Color(0xFF10B981), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (virtualDemoMode) "DEMO ON-SITE" else "LIVE GPS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (virtualDemoMode) Color(0xFF60A5FA) else Color(0xFF34D399)
                )
            }
        }

        // --- Active Stadium Selection Panel ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ACTIVE STADIUM:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutedColor(highContrast),
                        fontWeight = FontWeight.Bold
                    )
                )
                Box {
                    Button(
                        onClick = { showStadiumDropdown = true },
                        modifier = Modifier.fillMaxWidth().testTag("active_stadium_dropdown_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x15FFFFFF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Place, contentDescription = "Stadium", tint = AccentCyan)
                                Text("${activeStadium.name} (${activeStadium.city}, ${activeStadium.country})", fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                    DropdownMenu(
                        expanded = showStadiumDropdown,
                        onDismissRequest = { showStadiumDropdown = false },
                        modifier = Modifier.background(Color(0xFF16161B)).border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                    ) {
                        StadiumDatabase.stadiums.forEach { stadium ->
                            DropdownMenuItem(
                                text = { Text(stadium.name, color = Color.White, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    StadiumDatabase.updateActiveStadium(stadium.name)
                                    showStadiumDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Mode Selector Switches ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Wheelchair Route Toggle
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
                            imageVector = Icons.Default.Accessible,
                            contentDescription = "Accessibility",
                            tint = AccentEmerald
                        )
                        Column {
                            Text(
                                text = "Wheelchair & Ramp Routes Only",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = (13 * textScale).sp
                                )
                            )
                            Text(
                                text = "Avoids stairs, utilizes elevators and ramp inclines",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = isAccessibleOnly,
                        onCheckedChange = { isAccessibleOnly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentEmerald,
                            checkedTrackColor = AccentEmerald.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("accessible_routes_switch")
                    )
                }

                Divider(color = Color(0x11FFFFFF))

                // GPS Location Toggle / Request Box
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
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS Location",
                            tint = AccentCyan
                        )
                        Column {
                            Text(
                                text = "Use Simulated On-Site Location",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = (13 * textScale).sp
                                )
                            )
                            Text(
                                text = if (virtualDemoMode) "Places simulated coordinate in Lot A Parking" else "Using physical phone GPS coordinates",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!hasLocationPermission) {
                            Button(
                                onClick = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("ACTIVATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        Switch(
                            checked = virtualDemoMode,
                            onCheckedChange = { virtualDemoMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF3B82F6),
                                checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("demo_location_switch")
                        )
                    }
                }
            }
        }

        // --- Select Target Destination ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "SELECT MATCH-DAY DESTINATION:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MutedColor(highContrast),
                    fontSize = (10 * textScale).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(targets) { target ->
                    val isSel = selectedTarget == target
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) AccentEmerald.copy(alpha = 0.25f) else Color(0x0AFFFFFF))
                            .border(1.dp, if (isSel) AccentEmerald.copy(alpha = 0.4f) else Color(0x11FFFFFF), RoundedCornerShape(10.dp))
                            .clickable { 
                                selectedTarget = target 
                                customTargetLat = null
                                customTargetLng = null
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = target,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSel) AccentEmerald else Color.White,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = (11 * textScale).sp
                            )
                        )
                    }
                }
            }
        }

        // --- AI-Powered Amenity Finder (Grounded with Google Maps) ---
        GlassCard(
            modifier = Modifier.fillMaxWidth().testTag("amenity_finder_panel"),
            isAiTheme = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "AI Grounding Search",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "GROUNDED AMENITY WAYFINDER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = (11 * textScale).sp,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = "Find stadium food stalls, restrooms, or emergency facilities grounded with Google Maps. Points are plotted directly onto the interactive map.",
                    fontSize = (10 * textScale).sp,
                    color = Color.LightGray
                )

                // Search Bar Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amenitySearchQuery,
                        onValueChange = { amenitySearchQuery = it },
                        placeholder = { Text("Search (e.g. burgers, restroom, exit)", fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier.weight(1f).height(50.dp).testTag("amenity_search_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 12.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x05FFFFFF)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (amenitySearchQuery.isNotBlank()) {
                                isAmenitySearching = true
                                amenitySearchError = null
                                coroutineScope.launch {
                                    try {
                                        val rawJson = com.example.data.GeminiRepository.searchAmenitiesGrounded(
                                            query = amenitySearchQuery,
                                            stadiumName = activeStadium.name,
                                            userLat = userLat,
                                            userLng = userLng
                                        )
                                        // Parse array of GroundedAmenity objects
                                        val jsonArray = JSONArray(rawJson)
                                        val list = mutableListOf<GroundedAmenity>()
                                        for (i in 0 until jsonArray.length()) {
                                            val obj = jsonArray.getJSONObject(i)
                                            list.add(
                                                GroundedAmenity(
                                                    name = obj.getString("name"),
                                                    type = obj.getString("type"),
                                                    latitude = obj.getDouble("latitude"),
                                                    longitude = obj.getDouble("longitude"),
                                                    distanceText = obj.getString("distanceText"),
                                                    details = obj.getString("details"),
                                                    icon = obj.optString("icon", "📍")
                                                )
                                            )
                                        }
                                        amenitySearchResults = list
                                    } catch (e: Exception) {
                                        android.util.Log.e("NavigationScreen", "Amenity search failed", e)
                                        amenitySearchError = "Failed to parse amenity data. Please try again."
                                    } finally {
                                        isAmenitySearching = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(48.dp).testTag("amenity_search_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("FIND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                // Quick Query Chips
                val quickChips = listOf(
                    Pair("🍔 Food Hubs", "burger stall, tacos, beers, concessions"),
                    Pair("🚻 Restrooms", "washrooms, accessible toilets, family restrooms"),
                    Pair("🚨 Safety Exit", "emergency gates, exit corridors, medical center"),
                    Pair("🏥 First Aid", "stadium emergency medical center first-aid")
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickChips) { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x0EFFFFFF))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                .clickable {
                                    amenitySearchQuery = chip.first.substring(2).trim()
                                    isAmenitySearching = true
                                    amenitySearchError = null
                                    coroutineScope.launch {
                                        try {
                                            val rawJson = com.example.data.GeminiRepository.searchAmenitiesGrounded(
                                                query = chip.second,
                                                stadiumName = activeStadium.name,
                                                userLat = userLat,
                                                userLng = userLng
                                            )
                                            val jsonArray = JSONArray(rawJson)
                                            val list = mutableListOf<GroundedAmenity>()
                                            for (i in 0 until jsonArray.length()) {
                                                val obj = jsonArray.getJSONObject(i)
                                                list.add(
                                                    GroundedAmenity(
                                                        name = obj.getString("name"),
                                                        type = obj.getString("type"),
                                                        latitude = obj.getDouble("latitude"),
                                                        longitude = obj.getDouble("longitude"),
                                                        distanceText = obj.getString("distanceText"),
                                                        details = obj.getString("details"),
                                                        icon = obj.optString("icon", "📍")
                                                    )
                                                )
                                            }
                                            amenitySearchResults = list
                                        } catch (e: Exception) {
                                            android.util.Log.e("NavigationScreen", "Quick search failed", e)
                                            amenitySearchError = "Search error. Try again."
                                        } finally {
                                            isAmenitySearching = false
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chip.first,
                                fontSize = (10 * textScale).sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Search Status / Error State
                if (isAmenitySearching) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentCyan)
                            Text("Grounding results with Gemini Maps AI...", fontSize = 11.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        repeat(2) {
                            TravelRouteSkeleton()
                        }
                    }
                }

                amenitySearchError?.let { err ->
                    Text(err, color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }

                // Results list
                if (amenitySearchResults.isNotEmpty() && !isAmenitySearching) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GROUNDED MATCHES (${amenitySearchResults.size}):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Clear All",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                modifier = Modifier.clickable {
                                    amenitySearchResults = emptyList()
                                    customTargetLat = null
                                    customTargetLng = null
                                }
                            )
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(amenitySearchResults) { amenity ->
                                val isChosen = customTargetLat == amenity.latitude && customTargetLng == amenity.longitude
                                Box(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isChosen) AccentCyan.copy(alpha = 0.15f) else Color(0x06FFFFFF))
                                        .border(1.dp, if (isChosen) AccentCyan else Color(0x11FFFFFF), RoundedCornerShape(12.dp))
                                        .clickable {
                                            customTargetLat = amenity.latitude
                                            customTargetLng = amenity.longitude
                                            selectedTarget = amenity.name
                                            Toast.makeText(context, "Plotted route to ${amenity.name}!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(amenity.icon, fontSize = 14.sp)
                                                Text(
                                                    text = amenity.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = (11 * textScale).sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Distance: ${amenity.distanceText}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCyan
                                        )
                                        Text(
                                            text = amenity.details,
                                            fontSize = 9.sp,
                                            color = Color.LightGray,
                                            maxLines = 2
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isChosen) AccentCyan else Color(0x1AFFFFFF))
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isChosen) "ROUTE DOCKED" else "TAP TO NAVIGATE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isChosen) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Interactive Map Overlays HUD controls ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "INTERACTIVE STADIUM MAP OVERLAYS:",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MutedColor(highContrast),
                    fontSize = (10 * textScale).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seating Toggle Chip
                FilterChip(
                    selected = showSeatingOverlay,
                    onClick = { showSeatingOverlay = !showSeatingOverlay },
                    label = { Text("Seating Zones", fontSize = (11 * textScale).sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Seating",
                            modifier = Modifier.size(16.dp),
                            tint = if (showSeatingOverlay) Color.White else Color.Gray
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFF60A5FA),
                        containerColor = Color(0x0AFFFFFF),
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("seating_overlay_chip")
                )

                // Concessions Toggle Chip
                FilterChip(
                    selected = showConcessionsOverlay,
                    onClick = { showConcessionsOverlay = !showConcessionsOverlay },
                    label = { Text("Concessions", fontSize = (11 * textScale).sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Fastfood,
                            contentDescription = "Concessions",
                            modifier = Modifier.size(16.dp),
                            tint = if (showConcessionsOverlay) Color.White else Color.Gray
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFFFBBF24),
                        containerColor = Color(0x0AFFFFFF),
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("concessions_overlay_chip")
                )

                // Facilities Toggle Chip
                FilterChip(
                    selected = showFacilitiesOverlay,
                    onClick = { showFacilitiesOverlay = !showFacilitiesOverlay },
                    label = { Text("Facilities", fontSize = (11 * textScale).sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = "Facilities",
                            modifier = Modifier.size(16.dp),
                            tint = if (showFacilitiesOverlay) Color.White else Color.Gray
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFF34D399),
                        containerColor = Color(0x0AFFFFFF),
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("facilities_overlay_chip")
                )
            }
        }

        // --- Satellite / Street View Map Frame ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
        ) {
            val tileUrl = when (mapSource) {
                "google_road" -> "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
                "google_satellite" -> "https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}"
                "google_hybrid" -> "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}"
                "google_terrain" -> "https://mt1.google.com/vt/lyrs=p&x={x}&y={y}&z={z}"
                "osm" -> "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                "esri" -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
                else -> "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
            }

            val routePoints = getRoutePointsJson(activeStadium, selectedTarget, userLat, userLng, customTargetLat, customTargetLng)
            val pathColorHex = if (isAccessibleOnly) "#10B981" else "#FBBF24"

            val amenitiesJsonStr = JSONArray().apply {
                amenitySearchResults.forEach { amenity ->
                    put(JSONObject().apply {
                        put("name", amenity.name)
                        put("type", amenity.type)
                        put("latitude", amenity.latitude)
                        put("longitude", amenity.longitude)
                        put("distanceText", amenity.distanceText)
                        put("details", amenity.details)
                        put("icon", amenity.icon)
                    })
                }
            }.toString()

            // Loading Interactive Leaflet WebView Map
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                    }
                },
                update = { webView ->
                    val htmlContent = getMapHtml(
                        stadium = activeStadium,
                        userLat = userLat,
                        userLng = userLng,
                        targetLat = routeDetails.targetLat,
                        targetLng = routeDetails.targetLng,
                        tileUrl = tileUrl,
                        routePointsJson = routePoints,
                        pathColor = pathColorHex,
                        showSeating = showSeatingOverlay,
                        showConcessions = showConcessionsOverlay,
                        showFacilities = showFacilitiesOverlay,
                        amenitiesJson = amenitiesJsonStr
                    )
                    webView.loadDataWithBaseURL("https://appassets.androidassets.net", htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Compass overlay (floating HUD effect)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                GlassCard(
                    cornerRadius = 8.dp,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Compass", tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                        Text("COMPASS N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }

            // Satellite Toggle Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0xD00B0F19))
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MAP ENGINE / OVERLAY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentCyan,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf(
                            Pair("google_road", "Google Road"),
                            Pair("google_satellite", "Google Satellite"),
                            Pair("google_hybrid", "Google Hybrid"),
                            Pair("google_terrain", "Google Terrain"),
                            Pair("osm", "OSM Standard"),
                            Pair("esri", "Esri Satellite")
                        )) { (src, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (mapSource == src) Color(0xFF008E47) else Color(0xFF1C1C1E))
                                    .clickable { mapSource = src }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Wayfinding Directions Slide-up Panel ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            isAiTheme = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isAccessibleOnly) AccentEmerald.copy(alpha = 0.2f) else Color(0xFFFBBF24).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAccessibleOnly) Icons.Default.Accessible else Icons.Default.DirectionsWalk,
                            contentDescription = "Routings",
                            tint = if (isAccessibleOnly) AccentEmerald else Color(0xFFFBBF24),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "REAL-TIME PATHWAY NAVIGATION • ${routeDetails.distanceText}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFBBF24), // Gold
                                fontWeight = FontWeight.Bold,
                                fontSize = (9 * textScale).sp,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Estimated Time: ${routeDetails.durationText}",
                            fontWeight = FontWeight.Black,
                            fontSize = (15 * textScale).sp,
                            color = Color.White
                        )
                    }
                }

                Divider(color = Color(0x11FFFFFF))

                // Turn-by-Turn Directions list
                Text(
                    text = "TURN-BY-TURN DIRECTIONS:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutedColor(highContrast),
                        fontWeight = FontWeight.Bold
                    )
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    routeDetails.steps.forEachIndexed { index, step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan,
                                fontSize = (12 * textScale).sp
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontSize = (12 * textScale).sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val intentUri = android.net.Uri.parse("geo:${activeStadium.latitude},${activeStadium.longitude}?q=${android.net.Uri.encode(activeStadium.name + ", " + activeStadium.address)}")
                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, intentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val genericIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, intentUri)
                            context.startActivity(genericIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    modifier = Modifier.fillMaxWidth().height(40.dp).testTag("open_google_maps_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Open in Google Maps App",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Route in Google Maps App",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                if (isAccessibleOnly) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentEmerald.copy(alpha = 0.1f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Check", tint = AccentEmerald, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Elevator Core B certified fully accessible. Avoids stairs entirely.",
                            fontSize = 11.sp,
                            color = AccentEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- Official Stadium Information Panel ---
        Text(
            text = "OFFICIAL STADIUM INFORMATION",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                letterSpacing = 1.sp,
                fontSize = (11 * textScale).sp
            )
        )

        // Accordion 1: History & Architecture
        StadiumInfoAccordion(
            title = "Stadium History & Architecture",
            isExpanded = expandedHistory,
            onToggle = { expandedHistory = !expandedHistory }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("History", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                Text(activeStadium.history, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Architecture & Design", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                Text(activeStadium.architecture, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Year Opened: ${activeStadium.yearOpened}", fontSize = 11.sp, color = Color.Gray)
                    Text("Total Capacity: ${activeStadium.capacity}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Accordion 2: Tournaments & Matches
        StadiumInfoAccordion(
            title = "Tournaments & Upcoming Matches",
            isExpanded = expandedTournaments,
            onToggle = { expandedTournaments = !expandedTournaments }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Major Tournaments Hosted", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                activeStadium.tournamentsHosted.forEach { tourney ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(4.dp).background(Color.White).clip(CircleShape))
                        Text(tourney, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Upcoming Matches", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                activeStadium.upcomingMatches.forEach { match ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsSoccer, contentDescription = "Match", tint = AccentEmerald, modifier = Modifier.size(12.dp))
                        Text(match, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Accordion 3: Transport Options
        StadiumInfoAccordion(
            title = "Transport & Transit Connections",
            isExpanded = expandedTransit,
            onToggle = { expandedTransit = !expandedTransit }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Metro / Train Stations", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                activeStadium.trainStations.forEach { train ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Train, contentDescription = "Train", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        Text(train, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Bus Stops", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                activeStadium.busStops.forEach { bus ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsBus, contentDescription = "Bus", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        Text(bus, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("General Transit Info", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                activeStadium.transportOptions.forEach { opt ->
                    Text(opt, color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        // Accordion 4: Local Guide (Hotels, Restaurants, Attractions)
        StadiumInfoAccordion(
            title = "Local Guide & Attractions",
            isExpanded = expandedHotels,
            onToggle = { expandedHotels = !expandedHotels }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nearby Hotels", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                activeStadium.nearbyHotels.forEach { hotel ->
                    Text(hotel, color = Color.LightGray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Nearby Restaurants", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                activeStadium.nearbyRestaurants.forEach { restaurant ->
                    Text(restaurant, color = Color.LightGray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Nearby Attractions", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                activeStadium.nearbyAttractions.forEach { attraction ->
                    Text(attraction, color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        // Accordion 5: Emergency Contacts & Links
        StadiumInfoAccordion(
            title = "Official Contacts & Support Links",
            isExpanded = expandedContacts,
            onToggle = { expandedContacts = !expandedContacts }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Helplines", fontWeight = FontWeight.Bold, color = EmergencyRed, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = EmergencyRed, modifier = Modifier.size(12.dp))
                    Text("Official Helpline: ${activeStadium.helpline}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                activeStadium.emergencyContacts.forEach { contact ->
                    Text(contact, color = Color.LightGray, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Official Websites", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = "Web", tint = AccentCyan, modifier = Modifier.size(12.dp))
                    Text(activeStadium.website, color = AccentCyan, fontSize = 12.sp, modifier = Modifier.clickable { /* Link click helper */ })
                }
                activeStadium.officialLinks.forEach { link ->
                    Text(link, color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Official Address", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 12.sp)
                Text(activeStadium.address, color = Color.LightGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StadiumInfoAccordion(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = Color.White
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// Helpers for calculating real routing and displaying turn-by-turn directions
data class RouteResult(
    val targetLat: Double,
    val targetLng: Double,
    val distanceText: String,
    val durationText: String,
    val steps: List<String>
)

fun calculateRouteDetails(
    stadium: Stadium,
    target: String,
    isAccessible: Boolean,
    userLat: Double,
    userLng: Double,
    customTargetLat: Double? = null,
    customTargetLng: Double? = null
): RouteResult {
    // Exact landmark offsets from the stadium center
    val (targetLat, targetLng) = if (customTargetLat != null && customTargetLng != null) {
        Pair(customTargetLat, customTargetLng)
    } else {
        when {
            target.contains("Gate", ignoreCase = true) -> Pair(stadium.latitude + 0.0010, stadium.longitude + 0.0010)
            target.contains("Seat", ignoreCase = true) -> Pair(stadium.latitude, stadium.longitude)
            target.contains("Parking", ignoreCase = true) -> Pair(stadium.latitude + 0.0020, stadium.longitude - 0.0020)
            target.contains("Medical", ignoreCase = true) -> Pair(stadium.latitude + 0.0008, stadium.longitude + 0.0008)
            target.contains("Food", ignoreCase = true) -> Pair(stadium.latitude - 0.0008, stadium.longitude - 0.0008)
            target.contains("Restroom", ignoreCase = true) -> Pair(stadium.latitude - 0.0005, stadium.longitude + 0.0005)
            target.contains("Merchandise", ignoreCase = true) -> Pair(stadium.latitude - 0.0010, stadium.longitude)
            target.contains("Lost", ignoreCase = true) -> Pair(stadium.latitude, stadium.longitude - 0.0010)
            target.contains("Exit", ignoreCase = true) -> Pair(stadium.latitude + 0.0015, stadium.longitude)
            target.contains("Volunteer", ignoreCase = true) -> Pair(stadium.latitude, stadium.longitude + 0.0008)
            else -> Pair(stadium.latitude, stadium.longitude)
        }
    }

    // Measure exact distance in meters using Haversine formula
    val distanceMeters = haversineDistance(userLat, userLng, targetLat, targetLng)
    val distanceText = if (distanceMeters < 1000) "${distanceMeters.toInt()} meters" else String.format("%.2f km", distanceMeters / 1000.0)
    
    // Average walking speed 1.4 m/s (slower for accessible routes)
    val speed = if (isAccessible) 1.0 else 1.3
    val durationSeconds = distanceMeters / speed
    val durationText = "${(durationSeconds / 60.0).toInt().coerceAtLeast(1)} minutes"

    // Custom steps
    val steps = when {
        target.contains("Gate", ignoreCase = true) -> listOf(
            "Depart from current location and follow the pedestrian walkway North-East.",
            if (isAccessible) "Follow the illuminated accessible ramp towards Gate C (gradient 1:12)." else "Proceed up the stairs to the Gate C security plaza.",
            "Pass through the metal detector at Security Arch 4.",
            "Verify digital match ticket at the Turnstile scanner."
        )
        target.contains("Seat", ignoreCase = true) -> listOf(
            "Enter the stadium concourse level through Gate C.",
            "Walk down the East Corridor for 80 meters towards Elevator Core B.",
            if (isAccessible) "Take Elevator B to Level 2. Proceed straight towards the designated ADA companion seating in Sec 114."
            else "Take Section 114 stairs directly to the lower seating bowl.",
            "Navigate to Row M, Seat 8."
        )
        target.contains("Parking", ignoreCase = true) -> listOf(
            "Head out of the active seating section towards the main exit gates.",
            "Exit through Gate C and proceed towards the North-West Outer Concourse.",
            if (isAccessible) "Utilize the ADA-marked curb cutouts and walk along the barrier-free Lot A walkway." else "Follow the direct path down Section C pedestrian stairs.",
            "Arrive safely at Lot A Parking Area."
        )
        target.contains("Medical", ignoreCase = true) || target.contains("Hospital", ignoreCase = true) || target.contains("First-Aid", ignoreCase = true) || target.contains("🏥", ignoreCase = true) -> listOf(
            "Walk straight down the corridor towards Section 109 first-aid signage.",
            if (isAccessible) "Take the flat, non-slip pathway bypassing the stairs." else "Proceed down the stairs towards the ground trauma plaza.",
            "Enter the stadium Medical Center next to Guest Services."
        )
        target.contains("Food", ignoreCase = true) || target.contains("Concession", ignoreCase = true) || target.contains("🍔", ignoreCase = true) || target.contains("🌮", ignoreCase = true) || target.contains("🍺", ignoreCase = true) -> listOf(
            "Depart section and turn right onto the main concession hallway.",
            "Head towards the green-certified Eco Food Pavilion.",
            "All payment counters are at accessible heights with lowered service tables."
        )
        target.contains("Restroom", ignoreCase = true) || target.contains("🚻", ignoreCase = true) || target.contains("Toilet", ignoreCase = true) -> listOf(
            "Head down the central hallway towards Section 112.",
            "Turn left into the well-lit, fully sanitised facility corridor.",
            if (isAccessible) "ADA compartment is on the right with dual grab bars and easy-turn locks." else "Proceed into the main washroom area."
        )
        target.contains("Exit", ignoreCase = true) || target.contains("🚨", ignoreCase = true) -> listOf(
            "Observe the closest illuminated neon Green exit signs.",
            "Proceed in an orderly fashion down the wide, step-free egress corridor.",
            "Exit through the double fire doors directly onto the external concourse."
        )
        else -> listOf(
            "Head out towards the nearest pedestrian path.",
            "Follow the high-contrast physical and virtual floor arrows.",
            if (isAccessible) "Path verified completely barrier-free. Avoids stairs, ledges, and steps." else "Proceed straight down standard pedestrian corridor."
        )
    }

    return RouteResult(
        targetLat = targetLat,
        targetLng = targetLng,
        distanceText = distanceText,
        durationText = durationText,
        steps = steps
    )
}

// Haversine formula
fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

// Generate JSON string of coordinates to display realistic curved/corner pathways on the map
fun getRoutePointsJson(
    stadium: Stadium,
    target: String,
    userLat: Double,
    userLng: Double,
    customTargetLat: Double? = null,
    customTargetLng: Double? = null
): String {
    val (targetLat, targetLng) = if (customTargetLat != null && customTargetLng != null) {
        Pair(customTargetLat, customTargetLng)
    } else {
        when {
            target.contains("Gate", ignoreCase = true) -> Pair(stadium.latitude + 0.0010, stadium.longitude + 0.0010)
            target.contains("Seat", ignoreCase = true) -> Pair(stadium.latitude, stadium.longitude)
            target.contains("Parking", ignoreCase = true) -> Pair(stadium.latitude + 0.0020, stadium.longitude - 0.0020)
            target.contains("Medical", ignoreCase = true) -> Pair(stadium.latitude + 0.0008, stadium.longitude + 0.0008)
            target.contains("Food", ignoreCase = true) -> Pair(stadium.latitude - 0.0008, stadium.longitude - 0.0008)
            target.contains("Restroom", ignoreCase = true) -> Pair(stadium.latitude - 0.0005, stadium.longitude + 0.0005)
            target.contains("Merchandise", ignoreCase = true) -> Pair(stadium.latitude - 0.0010, stadium.longitude)
            target.contains("Lost", ignoreCase = true) -> Pair(stadium.latitude, stadium.longitude - 0.0010)
            target.contains("Exit", ignoreCase = true) -> Pair(stadium.latitude + 0.0015, stadium.longitude)
            target.contains("Volunteer", ignoreCase = true) -> Pair(stadium.latitude, stadium.longitude + 0.0008)
            else -> Pair(stadium.latitude, stadium.longitude)
        }
    }

    // Construct 3-4 points to create a realistic curved/corner walkway rather than a straight line
    val midLat = (userLat + targetLat) / 2.0
    val midLng = (userLng + targetLng) / 2.0

    // Add a bend to avoid restricted stadium play fields
    val points = listOf(
        JSONArray().put(userLat).put(userLng),
        JSONArray().put(midLat + 0.0005).put(midLng - 0.0005),
        JSONArray().put(midLat - 0.0002).put(midLng + 0.0002),
        JSONArray().put(targetLat).put(targetLng)
    )

    return JSONArray(points).toString()
}

// Map HTML content generator with high contrast dark UI
fun getMapHtml(
    stadium: Stadium,
    userLat: Double,
    userLng: Double,
    targetLat: Double,
    targetLng: Double,
    tileUrl: String,
    routePointsJson: String,
    pathColor: String,
    showSeating: Boolean,
    showConcessions: Boolean,
    showFacilities: Boolean,
    amenitiesJson: String = "[]"
): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #0b0f19; }
                .leaflet-container { background: #0b0f19 !important; }
                
                /* Custom markers */
                .user-dot {
                    background-color: #3b82f6;
                    width: 12px;
                    height: 12px;
                    border-radius: 50%;
                    border: 2px solid white;
                    box-shadow: 0 0 10px #3b82f6;
                }
                .target-dot {
                    background-color: #ef4444;
                    width: 14px;
                    height: 14px;
                    border-radius: 50%;
                    border: 2px solid white;
                    box-shadow: 0 0 10px #ef4444;
                }
                .stadium-bounds {
                    background-color: rgba(16, 185, 129, 0.1);
                    border: 2px dashed #10b981;
                    border-radius: 50%;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([${targetLat}, ${targetLng}], 17);
                
                L.tileLayer('$tileUrl', {
                    maxZoom: 19,
                    attribution: 'OpenStreetMap | Esri Satellite Map'
                }).addTo(map);
                
                // Add Stadium Perimeter ring
                L.circle([${stadium.latitude}, ${stadium.longitude}], {
                    color: '#10b981',
                    fillColor: '#10b981',
                    fillOpacity: 0.05,
                    radius: 200
                }).addTo(map);

                // Add Seating Sections Overlay
                if ($showSeating) {
                    // Lower Bowl
                    L.circle([${stadium.latitude}, ${stadium.longitude}], {
                        color: '#3B82F6',
                        fillColor: '#3B82F6',
                        fillOpacity: 0.15,
                        radius: 70
                    }).addTo(map).bindTooltip("<b>Lower Seating Bowl</b><br>Sections 101-149", { permanent: false, direction: "center" });

                    // Club / Suites Tier
                    L.circle([${stadium.latitude}, ${stadium.longitude}], {
                        color: '#F59E0B',
                        fillColor: '#F59E0B',
                        fillOpacity: 0.1,
                        radius: 120
                    }).addTo(map).bindTooltip("<b>Club & Suites Tier</b><br>Sections 201-249", { permanent: false, direction: "center" });

                    // Upper Deck
                    L.circle([${stadium.latitude}, ${stadium.longitude}], {
                        color: '#EF4444',
                        fillColor: '#EF4444',
                        fillOpacity: 0.05,
                        radius: 180
                    }).addTo(map).bindTooltip("<b>Upper Seating Deck</b><br>Sections 301-349", { permanent: false, direction: "center" });
                }

                // Add Concessions Overlay
                if ($showConcessions) {
                    var concessionIcon1 = L.divIcon({
                        className: 'concession-marker-1',
                        html: '<div style="background-color: #F59E0B; width: 26px; height: 26px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 8px #F59E0B;">🍔</div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    L.marker([${stadium.latitude + 0.0007}, ${stadium.longitude}], { icon: concessionIcon1 }).addTo(map)
                        .bindPopup("<b>🍔 World Cup Eats</b><br><i>Concession Stand (North Row)</i><br>• Premium Angus Burgers<br>• Golden Fries & Drinks");

                    var concessionIcon2 = L.divIcon({
                        className: 'concession-marker-2',
                        html: '<div style="background-color: #F59E0B; width: 26px; height: 26px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 8px #F59E0B;">🍺</div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    L.marker([${stadium.latitude}, ${stadium.longitude + 0.0007}], { icon: concessionIcon2 }).addTo(map)
                        .bindPopup("<b>🍺 Budweiser Beer Zone</b><br><i>Beverage Hub (East Gate)</i><br>• Draft & Craft Beers<br>• Warm Salted Pretzels");

                    var concessionIcon3 = L.divIcon({
                        className: 'concession-marker-3',
                        html: '<div style="background-color: #F59E0B; width: 26px; height: 26px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 8px #F59E0B;">🌮</div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    L.marker([${stadium.latitude - 0.0007}, ${stadium.longitude}], { icon: concessionIcon3 }).addTo(map)
                        .bindPopup("<b>🌮 El Taco Loco</b><br><i>Taco Hub (South Section)</i><br>• Sizzling Tacos & Nachos<br>• Chilled Sodas");

                    var concessionIcon4 = L.divIcon({
                        className: 'concession-marker-4',
                        html: '<div style="background-color: #F59E0B; width: 26px; height: 26px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 8px #F59E0B;">🛍️</div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    L.marker([${stadium.latitude}, ${stadium.longitude - 0.0007}], { icon: concessionIcon4 }).addTo(map)
                        .bindPopup("<b>🛍️ Official Merchandise Megastore</b><br><i>Main retail booth (West Hall)</i><br>• World Cup Jerseys<br>• Scarves, Flags & Caps");
                }

                // Add Facilities Overlay
                if ($showFacilities) {
                    var facilityIcon1 = L.divIcon({
                        className: 'facility-marker-1',
                        html: '<div style="background-color: #10B981; width: 26px; height: 26px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 8px #10B981;">🏥</div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    L.marker([${stadium.latitude + 0.0004}, ${stadium.longitude + 0.0004}], { icon: facilityIcon1 }).addTo(map)
                        .bindPopup("<b>🏥 Emergency Medical Station</b><br><i>First-Aid Center (North-East Plaza)</i><br>• Certified EMS On-site<br>• Free sensory bags, ice-packs");

                    var facilityIcon2 = L.divIcon({
                        className: 'facility-marker-2',
                        html: '<div style="background-color: #10B981; width: 26px; height: 26px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 8px #10B981;">🛗</div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    L.marker([${stadium.latitude - 0.0004}, ${stadium.longitude - 0.0004}], { icon: facilityIcon2 }).addTo(map)
                        .bindPopup("<b>🛗 ADA Elevator Core B</b><br><i>Barrier-free lifts (South-West Core)</i><br>• Stair-free Level 1 & 2 access<br>• Priority companion elevator");

                    var facilityIcon3 = L.divIcon({
                        className: 'facility-marker-3',
                        html: '<div style="background-color: #10B981; width: 26px; height: 26px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 8px #10B981;">🚻</div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    L.marker([${stadium.latitude - 0.0004}, ${stadium.longitude + 0.0004}], { icon: facilityIcon3 }).addTo(map)
                        .bindPopup("<b>🚻 Family Restrooms & Baby Care</b><br><i>Accessible facilities (South-East Plaza)</i><br>• Private changing tables<br>• Fully ADA compliant");

                    var facilityIcon4 = L.divIcon({
                        className: 'facility-marker-4',
                        html: '<div style="background-color: #10B981; width: 26px; height: 26px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 8px #10B981;">🤫</div>',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                    L.marker([${stadium.latitude + 0.0004}, ${stadium.longitude - 0.0004}], { icon: facilityIcon4 }).addTo(map)
                        .bindPopup("<b>🤫 KultureCity Sensory Room</b><br><i>Quiet Zone (North-West Concourse)</i><br>• Weighted lap pads & ear defenders<br>• Noise-isolated serene environment");
                }

                // Target Destination Marker
                var targetIcon = L.divIcon({
                    className: 'target-marker',
                    html: '<div class="target-dot"></div>',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                });
                L.marker([$targetLat, $targetLng], { icon: targetIcon }).addTo(map)
                    .bindPopup("<b>Target Destination</b>")
                    .openPopup();
                
                // User Current GPS Location Marker
                var userIcon = L.divIcon({
                    className: 'user-marker',
                    html: '<div class="user-dot"></div>',
                    iconSize: [12, 12],
                    iconAnchor: [6, 6]
                });
                var userMarker = L.marker([$userLat, $userLng], { icon: userIcon }).addTo(map)
                    .bindPopup("<b>My Current Location</b>");

                // Custom Grounded search result pins
                var customAmenities = $amenitiesJson;
                if (customAmenities && customAmenities.length > 0) {
                    customAmenities.forEach(function(item) {
                        var markerColor = '#8b5cf6'; // Purple / Violet
                        if (item.type === 'FOOD') markerColor = '#f59e0b'; // Amber
                        else if (item.type === 'RESTROOM') markerColor = '#10b981'; // Emerald
                        else if (item.type === 'EMERGENCY_EXIT') markerColor = '#ef4444'; // Red

                        var customAmenityIcon = L.divIcon({
                            className: 'custom-amenity-' + item.name.replace(/\s+/g, '-'),
                            html: '<div style="background-color: ' + markerColor + '; width: 28px; height: 28px; border-radius: 50%; border: 2px solid white; display: flex; align-items: center; justify-content: center; font-size: 14px; box-shadow: 0 0 10px ' + markerColor + ';">' + (item.icon || '📍') + '</div>',
                            iconSize: [28, 28],
                            iconAnchor: [14, 14]
                        });

                        L.marker([item.latitude, item.longitude], { icon: customAmenityIcon }).addTo(map)
                            .bindPopup("<b>" + (item.icon || '📍') + " " + item.name + "</b><br><i>" + item.distanceText + "</i><br>• " + item.details);
                    });
                }

                // Route Pathway Points
                var points = $routePointsJson;
                if (points && points.length > 0) {
                    var polyline = L.polyline(points, {
                        color: '$pathColor',
                        weight: 6,
                        opacity: 0.9,
                        lineJoin: 'round',
                        lineCap: 'round'
                    }).addTo(map);
                    
                    // Automatically adjust map view to contain entire route path nicely
                    var group = new L.featureGroup([userMarker, L.marker([$targetLat, $targetLng])]);
                    map.fitBounds(group.getBounds().pad(0.15));
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
