package com.example.ui.screens

import android.widget.Toast
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.GeminiRepository
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.AppState
import com.example.data.FootballPhotos
import com.example.data.MatchService
import com.example.ui.components.GlassCard
import com.example.ui.components.NewsCardSkeleton
import com.example.ui.components.MatchCardSkeleton
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.Dispatchers
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.UUID

data class ParsedLiveMatch(
    val id: String,
    val competition: String,
    val homeTeam: String,
    val homeTeamCode: String,
    val homeScore: Int,
    val awayTeam: String,
    val awayTeamCode: String,
    val awayScore: Int,
    val status: String,
    val minute: String,
    val events: List<String> = emptyList(),
    val venue: String = ""
)

object MockSportsJson {
    val API_FOOTBALL_JSON = """
    {
      "response": [
        {
          "fixture": {
            "id": "apif_1",
            "venue": { "name": "MetLife Stadium" },
            "status": { "short": "2H", "elapsed": 68 }
          },
          "league": { "name": "FIFA World Cup" },
          "teams": {
            "home": { "name": "United States" },
            "away": { "name": "Germany" }
          },
          "goals": { "home": 2, "away": 1 },
          "events": [
            { "time": { "elapsed": 12 }, "team": { "name": "United States" }, "player": { "name": "C. Pulisic" }, "type": "Goal" },
            { "time": { "elapsed": 34 }, "team": { "name": "Germany" }, "player": { "name": "K. Havertz" }, "type": "Goal" },
            { "time": { "elapsed": 59 }, "team": { "name": "United States" }, "player": { "name": "T. Weah" }, "type": "Goal" }
          ]
        },
        {
          "fixture": {
            "id": "apif_2",
            "venue": { "name": "Azteca Stadium" },
            "status": { "short": "1H", "elapsed": 24 }
          },
          "league": { "name": "FIFA World Cup" },
          "teams": {
            "home": { "name": "Mexico" },
            "away": { "name": "Argentina" }
          },
          "goals": { "home": 0, "away": 2 },
          "events": [
            { "time": { "elapsed": 8 }, "team": { "name": "Argentina" }, "player": { "name": "L. Messi" }, "type": "Goal" },
            { "time": { "elapsed": 19 }, "team": { "name": "Argentina" }, "player": { "name": "J. Álvarez" }, "type": "Goal" }
          ]
        }
      ]
    }
    """.trimIndent()

    val SPORTRADAR_JSON = """
    {
      "summaries": [
        {
          "sport_event": {
            "id": "sr_1",
            "sport_event_context": { "competition": { "name": "Champions League" } },
            "competitors": [
              { "name": "Real Madrid", "abbreviation": "RMA", "qualifier": "home" },
              { "name": "Manchester City", "abbreviation": "MCI", "qualifier": "away" }
            ],
            "venue": { "name": "Santiago Bernabéu" }
          },
          "sport_event_status": {
            "home_score": 3,
            "away_score": 2,
            "match_status": "2nd_half",
            "clock": { "played": "81:12" }
          }
        },
        {
          "sport_event": {
            "id": "sr_2",
            "sport_event_context": { "competition": { "name": "Premier League" } },
            "competitors": [
              { "name": "Arsenal", "abbreviation": "ARS", "qualifier": "home" },
              { "name": "Chelsea", "abbreviation": "CHE", "qualifier": "away" }
            ],
            "venue": { "name": "Emirates Stadium" }
          },
          "sport_event_status": {
            "home_score": 1,
            "away_score": 0,
            "match_status": "1st_half",
            "clock": { "played": "18:45" }
          }
        }
      ]
    }
    """.trimIndent()

    val CRICKETDATA_JSON = """
    {
      "data": [
        {
          "id": "cric_1",
          "name": "India vs Australia T20 International",
          "status": "India won by 6 wickets",
          "venue": "Melbourne Cricket Ground",
          "teams": ["India", "Australia"],
          "score": [
            { "r": 182, "w": 4, "o": 19.2, "inning": "India Inning 1" },
            { "r": 180, "w": 8, "o": 20, "inning": "Australia Inning 1" }
          ]
        },
        {
          "id": "cric_2",
          "name": "England vs Pakistan Test Match",
          "status": "Pakistan trail by 124 runs",
          "venue": "Lord's Cricket Ground",
          "teams": ["England", "Pakistan"],
          "score": [
            { "r": 450, "w": 10, "o": 112.4, "inning": "England Inning 1" },
            { "r": 326, "w": 6, "o": 88, "inning": "Pakistan Inning 1" }
          ]
        }
      ],
      "status": "success"
    }
    """.trimIndent()
}

fun parseLiveScoresJson(jsonStr: String, source: String): List<ParsedLiveMatch> {
    val resultList = mutableListOf<ParsedLiveMatch>()
    try {
        val rootObj = JSONObject(jsonStr)
        if (source == "RAPIDAPI") {
            val responseArray = rootObj.optJSONArray("response")
            if (responseArray != null) {
                for (i in 0 until responseArray.length()) {
                    val item = responseArray.getJSONObject(i)
                    val fixture = item.getJSONObject("fixture")
                    val league = item.getJSONObject("league")
                    val teams = item.getJSONObject("teams")
                    val goals = item.getJSONObject("goals")
                    
                    val homeTeamObj = teams.getJSONObject("home")
                    val awayTeamObj = teams.getJSONObject("away")
                    
                    val eventsList = mutableListOf<String>()
                    val eventsArray = item.optJSONArray("events")
                    if (eventsArray != null) {
                        for (j in 0 until eventsArray.length()) {
                            val eventObj = eventsArray.getJSONObject(j)
                            val timeObj = eventObj.getJSONObject("time")
                            val elapsed = timeObj.getInt("elapsed")
                            val playerObj = eventObj.getJSONObject("player")
                            val playerName = playerObj.optString("name", "Player")
                            val eventType = eventObj.optString("type", "Event")
                            eventsList.add("$elapsed' - $eventType: $playerName")
                        }
                    }
                    
                    resultList.add(
                        ParsedLiveMatch(
                            id = fixture.optString("id", UUID.randomUUID().toString()),
                            competition = league.optString("name", "World Cup"),
                            homeTeam = homeTeamObj.optString("name", "Home"),
                            homeTeamCode = homeTeamObj.optString("name", "HOM").take(3).uppercase(),
                            homeScore = goals.optInt("home", 0),
                            awayTeam = awayTeamObj.optString("name", "Away"),
                            awayTeamCode = awayTeamObj.optString("name", "AWY").take(3).uppercase(),
                            awayScore = goals.optInt("away", 0),
                            status = fixture.getJSONObject("status").optString("short", "LIVE"),
                            minute = fixture.getJSONObject("status").optInt("elapsed", 0).toString() + "'",
                            events = eventsList,
                            venue = fixture.getJSONObject("venue").optString("name", "Stadium")
                        )
                    )
                }
            }
        } else if (source == "SPORTRADAR") {
            val summariesArray = rootObj.optJSONArray("summaries")
            if (summariesArray != null) {
                for (i in 0 until summariesArray.length()) {
                    val summary = summariesArray.getJSONObject(i)
                    val sportEvent = summary.getJSONObject("sport_event")
                    val context = sportEvent.getJSONObject("sport_event_context")
                    val competitionName = context.getJSONObject("competition").optString("name", "Soccer Match")
                    
                    val competitorsArray = sportEvent.getJSONArray("competitors")
                    var homeTeamName = "Home"
                    var homeTeamCode = "HOM"
                    var awayTeamName = "Away"
                    var awayTeamCode = "AWY"
                    
                    for (j in 0 until competitorsArray.length()) {
                        val comp = competitorsArray.getJSONObject(j)
                        val qualifier = comp.optString("qualifier", "home")
                        if (qualifier == "home") {
                            homeTeamName = comp.optString("name", "Home")
                            homeTeamCode = comp.optString("abbreviation", "HOM")
                        } else {
                            awayTeamName = comp.optString("name", "Away")
                            awayTeamCode = comp.optString("abbreviation", "AWY")
                        }
                    }
                    
                    val statusObj = summary.getJSONObject("sport_event_status")
                    val homeScore = statusObj.optInt("home_score", 0)
                    val awayScore = statusObj.optInt("away_score", 0)
                    val matchStatus = statusObj.optString("match_status", "live")
                    val clockPlayed = statusObj.optJSONObject("clock")?.optString("played", "00:00") ?: "Live"
                    
                    resultList.add(
                        ParsedLiveMatch(
                            id = sportEvent.optString("id", UUID.randomUUID().toString()),
                            competition = competitionName,
                            homeTeam = homeTeamName,
                            homeTeamCode = homeTeamCode,
                            homeScore = homeScore,
                            awayTeam = awayTeamName,
                            awayTeamCode = awayTeamCode,
                            awayScore = awayScore,
                            status = matchStatus.replace("_", " ").uppercase(),
                            minute = clockPlayed,
                            venue = sportEvent.optJSONObject("venue")?.optString("name", "Stadium") ?: "Stadium"
                        )
                    )
                }
            }
        } else if (source == "CRICKETDATA") {
            val dataArray = rootObj.optJSONArray("data")
            if (dataArray != null) {
                for (i in 0 until dataArray.length()) {
                    val matchObj = dataArray.getJSONObject(i)
                    val id = matchObj.optString("id", UUID.randomUUID().toString())
                    val name = matchObj.optString("name", "Cricket Match")
                    val status = matchObj.optString("status", "Live")
                    val venue = matchObj.optString("venue", "MCG")
                    
                    val scoreArray = matchObj.optJSONArray("score")
                    var homeInning = ""
                    var awayInning = ""
                    if (scoreArray != null && scoreArray.length() > 0) {
                        val score1 = scoreArray.getJSONObject(0)
                        homeInning = "${score1.optInt("r")}/${score1.optInt("w")} (${score1.optDouble("o")} ov)"
                        if (scoreArray.length() > 1) {
                            val score2 = scoreArray.getJSONObject(1)
                            awayInning = "${score2.optInt("r")}/${score2.optInt("w")} (${score2.optDouble("o")} ov)"
                        }
                    }
                    
                    val teamsArr = matchObj.optJSONArray("teams")
                    val homeT = teamsArr?.optString(0) ?: "Team A"
                    val awayT = teamsArr?.optString(1) ?: "Team B"
                    
                    resultList.add(
                        ParsedLiveMatch(
                            id = id,
                            competition = "International Cricket Series",
                            homeTeam = homeT,
                            homeTeamCode = homeT.take(3).uppercase(),
                            homeScore = 0,
                            awayTeam = awayT,
                            awayTeamCode = awayT.take(3).uppercase(),
                            awayScore = 0,
                            status = status,
                            minute = "LIVE",
                            events = listOf("Inning 1: $homeInning", "Inning 2: $awayInning").filter { it.length > 10 },
                            venue = venue
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        Log.e("LiveMatchFeedScreen", "Error parsing sports JSON", e)
    }
    return resultList
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMatchFeedScreen() {
    val context = LocalContext.current
    val isDarkMode by AppState.isDarkMode.collectAsState()
    val textScale by AppState.textScale.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()

    val liveMatch by MatchService.liveMatchState.collectAsState()
    val standings by MatchService.liveStandingsState.collectAsState()
    val competitionMatches by MatchService.competitionMatchesState.collectAsState()
    val activeCompetitionCode by MatchService.activeCompetitionCode.collectAsState()
    val apiKey by MatchService.activeApiKey.collectAsState()
    val isFetching by MatchService.isFetchingApi.collectAsState()
    val apiError by MatchService.apiError.collectAsState()
    val highlights by MatchService.highlightsState.collectAsState()

    var showApiKeyConfig by remember { mutableStateOf(false) }
    var inputToken by remember { mutableStateOf(apiKey) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Match Center, 1: Standings, 2: Highlights, 3: AI News Feed, 4: My Alerts, 5: Live API & Widgets

    // Global Sports API Live integration states
    var selectedApiSource by remember { mutableStateOf("RAPIDAPI") } // RAPIDAPI, SPORTRADAR, CRICKETDATA, CUSTOM_REST
    var customApiUrl by remember { mutableStateOf("https://api-football-v1.p.rapidapi.com/v3/fixtures?live=all") }
    var customApiKey by remember { mutableStateOf("") }
    var customApiHost by remember { mutableStateOf("api-football-v1.p.rapidapi.com") }
    var rawJsonResponse by remember { mutableStateOf("") }
    var parsedLiveMatches by remember { mutableStateOf<List<ParsedLiveMatch>>(emptyList()) }
    var isLiveApiLoading by remember { mutableStateOf(false) }
    var liveApiError by remember { mutableStateOf<String?>(null) }

    var newsQuery by remember { mutableStateOf("") }
    var newsItemsList by remember { mutableStateOf<List<WorldCupNewsItem>>(emptyList()) }
    var isNewsLoading by remember { mutableStateOf(false) }
    var newsError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3 && newsItemsList.isEmpty()) {
            isNewsLoading = true
            newsError = null
            try {
                val response = GeminiRepository.fetchWorldCupNewsGrounded("latest 2026 World Cup matches lineups injuries updates")
                newsItemsList = parseNewsJson(response)
            } catch (e: Exception) {
                newsError = e.localizedMessage ?: "Failed to fetch updates."
            } finally {
                isNewsLoading = false
            }
        }
    }

    val competitions = listOf(
        Pair("WC", "FIFA World Cup"),
        Pair("PL", "Premier League"),
        Pair("CL", "Champions League"),
        Pair("PD", "La Liga"),
        Pair("SA", "Serie A"),
        Pair("BL1", "Bundesliga")
    )

    // Goal flash animation state
    var goalFlashActive by remember { mutableStateOf(false) }
    var prevHomeScore by remember { mutableStateOf<Int?>(null) }
    var prevAwayScore by remember { mutableStateOf<Int?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(liveMatch) {
        val currentHome = liveMatch?.homeScore
        val currentAway = liveMatch?.awayScore
        if (currentHome != null && prevHomeScore != null && currentHome > prevHomeScore!!) {
            goalFlashActive = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(150)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (currentAway != null && prevAwayScore != null && currentAway > prevAwayScore!!) {
            goalFlashActive = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            kotlinx.coroutines.delay(150)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (currentHome != null) prevHomeScore = currentHome
        if (currentAway != null) prevAwayScore = currentAway
    }
    val flashAlpha by animateFloatAsState(
        targetValue = if (goalFlashActive) 1f else 0f,
        animationSpec = repeatable(
            iterations = 4,
            animation = tween(250, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        finishedListener = { goalFlashActive = false },
        label = "flashAlpha"
    )

    LaunchedEffect(apiKey) {
        inputToken = apiKey
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background High-Resolution Match Celebration / Action Photo
            AsyncImage(
                model = FootballPhotos.MATCH_ACTION_1,
                contentDescription = "Match Action Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.08f
            )
            // Flash banner overlay for Goals
            if (flashAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF008E47).copy(alpha = flashAlpha * 0.8f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = "Goal Icon",
                            tint = Color.White,
                            modifier = Modifier.size(100.dp)
                        )
                        Text(
                            text = "GOAL!!!",
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Scoreboard Updated in Real-Time!",
                            fontSize = 20.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                // 1. Header Section
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LIVE MATCH CENTER",
                                    fontWeight = FontWeight.Black,
                                    fontSize = (22 * textScale).sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Real-time scores & tournament standings",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                            IconButton(
                                onClick = { showApiKeyConfig = !showApiKeyConfig },
                                modifier = Modifier
                                    .background(
                                        if (apiKey.isNotBlank()) AccentEmerald.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                                        CircleShape
                                    )
                                    .testTag("api_key_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (apiKey.isNotBlank()) Icons.Default.Key else Icons.Default.Lock,
                                    contentDescription = "Configure API Key",
                                    tint = if (apiKey.isNotBlank()) AccentEmerald else Color.White
                                )
                            }
                        }
                    }
                }

                // 2. Expandable API Token Config Panel
                if (showApiKeyConfig) {
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF242427), RoundedCornerShape(12.dp))
                                .testTag("api_key_config_panel")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Key,
                                        contentDescription = "Key Icon",
                                        tint = Color(0xFFC5A059)
                                    )
                                    Text(
                                        text = "Football-Data.org API Token",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "To enable authentic live data feeds, enter your API token below. You can obtain a free key instantly from football-data.org.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                OutlinedTextField(
                                    value = inputToken,
                                    onValueChange = { inputToken = it },
                                    placeholder = { Text("Enter X-Auth-Token...", color = Color.Gray) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("api_key_input_field"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = AccentEmerald,
                                        unfocusedBorderColor = Color(0xFF3E3E42)
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (apiKey.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                MatchService.setApiKey("")
                                                inputToken = ""
                                                Toast.makeText(context, "API Key cleared. Switched to offline simulation mode.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                        ) {
                                            Text("Clear Key")
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            MatchService.setApiKey(inputToken)
                                            Toast.makeText(context, "API Token updated! Refreshing match data...", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                                    ) {
                                        Text("Save & Apply")
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Competition Dropdown/Horizontal Selector
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = Color(0xFFC5A059),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "TOURNAMENT ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFC5A059),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            competitions.take(3).forEach { (code, name) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (activeCompetitionCode == code) Color(0xFF2C2C2E) else Color.Transparent)
                                        .clickable { MatchService.setCompetitionCode(code) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (activeCompetitionCode == code) Color.White else Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            competitions.drop(3).forEach { (code, name) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (activeCompetitionCode == code) Color(0xFF2C2C2E) else Color.Transparent)
                                        .clickable { MatchService.setCompetitionCode(code) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (activeCompetitionCode == code) Color.White else Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Status info bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (apiKey.isNotBlank()) AccentEmerald else WarningOrange)
                            )
                            Text(
                                text = if (apiKey.isNotBlank()) "External API Live Stream Connected" else "Simulated Match Mode Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = AccentEmerald)
                        } else {
                            IconButton(
                                onClick = { MatchService.refreshAllData() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Feed", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Error State Display
                if (apiError != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1C1C)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red)
                                Column {
                                    Text("API Connection Warning", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text(apiError ?: "Unknown error", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Tab selection: Live feed vs Standings
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF008E47)
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Match Center", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("match_center_tab")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Group Standings", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("standings_tab")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Highlights", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("highlights_tab")
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("AI News Feed", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("ai_news_tab")
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("My Alerts", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("notifications_tab")
                        )
                        Tab(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            text = { Text("API & Widgets", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("api_widgets_tab")
                        )
                    }
                }

                if (selectedTab == 0) {
                    // TAB 0: Match Center & Live Scorecard Banner
                    item {
                        val activeCompName = competitions.firstOrNull { it.first == activeCompetitionCode }?.second ?: "FIFA World Cup"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                .testTag("ai_grounded_fetch_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "REAL-TIME GOOGLE SEARCH FETCH",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = AccentCyan
                                    )
                                }
                                Text(
                                    text = "No API key? Fetch real-world live match scores, upcoming schedules, and actual group tables directly from Google search index grounded with Gemini.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                                Button(
                                    onClick = { MatchService.fetchRealScoresGroundedViaAI(activeCompetitionCode, activeCompName) },
                                    enabled = !isFetching,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008E47)),
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    if (isFetching) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("AI Querying Live Web...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Fetch Real Live Matches & Scores", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    val match = liveMatch
                    if (match != null) {
                        item {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF242427), RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
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
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (match.matchStatus == "LIVE" || match.matchStatus == "IN_PLAY" || match.matchStatus == "PAUSED") EmergencyRed else Color.Gray)
                                            )
                                            Text(
                                                text = match.competition.uppercase(),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = Color(0xFFC5A059),
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.5.sp,
                                                    fontSize = (10 * textScale).sp
                                                )
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF242427))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${match.currentMinute} ${match.matchStatus}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = (11 * textScale).sp
                                                )
                                            )
                                        }
                                    }

                                    // Team 1 vs Team 2 scores
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Home
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF1D4ED8)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(match.homeTeamCode, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = match.homeTeam,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("Home", fontSize = 10.sp, color = Color.Gray)
                                        }

                                        // Large Score numbers
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(
                                                text = match.homeScore.toString(),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = (42 * textScale).sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = ":",
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 28.sp,
                                                color = Color(0xFF8E8E93)
                                            )
                                            Text(
                                                text = match.awayScore.toString(),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = (42 * textScale).sp,
                                                color = Color.White
                                            )
                                        }

                                        // Away
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF008E47)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(match.awayTeamCode, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = match.awayTeam,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("Visitor", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }

                                    // Interactive Dynamic Simulation Controls for user testing!
                                    if (apiKey.isBlank()) {
                                        Divider(color = Color(0xFF242427), thickness = 1.dp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("DEMO SIMULATOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5A059))
                                                Text("Test live update capabilities", fontSize = 11.sp, color = Color.LightGray)
                                            }
                                            Button(
                                                onClick = {
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    goalFlashActive = true
                                                    MatchService.simulateGoalScored()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008E47)),
                                                modifier = Modifier.testTag("simulate_goal_btn")
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Text("Simulate Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // Possession, Shots, and Stats Info
                                    Divider(color = if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), thickness = 1.dp)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Possession: ${match.homeTeamCode} ${match.possessionHome}%", fontSize = 11.sp, color = if (isDarkMode) Color.LightGray else Color(0xFF4B5563))
                                            Text("${match.awayTeamCode} ${match.possessionAway}%", fontSize = 11.sp, color = if (isDarkMode) Color.LightGray else Color(0xFF4B5563))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(match.possessionHome.toFloat() / 100f)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF008E47))
                                            )
                                        }
                                    }

                                    Divider(color = if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), thickness = 1.dp)

                                    // Event Logs
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "GOALSCORERS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC5A059)
                                        )
                                        if (match.goalscorers.isEmpty()) {
                                            Text("No goals scored yet.", fontSize = 11.sp, color = Color.Gray)
                                        } else {
                                            match.goalscorers.forEach { scoreLog ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFFC5A059), modifier = Modifier.size(14.dp))
                                                    Text(scoreLog, fontSize = 12.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section: Competition Match Feed
                    item {
                        Text(
                            text = "COMPETITION FIXTURES & SCORES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC5A059),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (competitionMatches.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No matches found for this competition code.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(competitionMatches) { itemMatch ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF141416) else Color(0xFFFFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = itemMatch.homeTeam,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = itemMatch.awayTeam,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Score numbers
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (itemMatch.status == "SCHEDULED") "—" else itemMatch.homeScore.toString(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (itemMatch.status == "SCHEDULED") "—" else itemMatch.awayScore.toString(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Status Badge / Clock Info
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (itemMatch.status) {
                                                    "IN_PLAY", "LIVE" -> EmergencyRed.copy(alpha = 0.2f)
                                                    "FINISHED" -> Color.White.copy(alpha = 0.08f)
                                                    else -> AccentCyan.copy(alpha = 0.15f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = when (itemMatch.status) {
                                                "IN_PLAY", "LIVE" -> "LIVE ${itemMatch.minute}"
                                                "FINISHED" -> "FT"
                                                else -> "UPCOMING"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when (itemMatch.status) {
                                                "IN_PLAY", "LIVE" -> EmergencyRed
                                                "FINISHED" -> Color.LightGray
                                                else -> AccentCyan
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // TAB 1: Group Standings Grid
                    if (standings.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No standings table available.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        // Group standings sorted or grouped by GroupName
                        val groupedStandings = standings.groupBy { it.groupName }

                        groupedStandings.forEach { (groupName, tableEntries) ->
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Text(
                                        text = groupName.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color(0xFFC5A059),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    // Standings Table Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF141416), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(20.dp))
                                        Text("TEAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                                        Text("PL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                        Text("W", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                        Text("D", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                        Text("L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                        Text("GD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                        Text("PTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                    }

                                    // Standings Table Rows
                                    tableEntries.sortedBy { it.position }.forEachIndexed { index, entry ->
                                        val isUserTeam = entry.teamName.contains("United States") || entry.teamCode == "USA"
                                        val isLastRow = index == tableEntries.size - 1

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isUserTeam) Color(0xFF008E47).copy(alpha = 0.12f)
                                                    else if (index % 2 == 0) Color(0xFF1C1C1E)
                                                    else Color(0xFF141416)
                                                )
                                                .clip(
                                                    if (isLastRow) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                                    else RoundedCornerShape(0.dp)
                                                )
                                                .border(
                                                    width = if (isUserTeam) 1.dp else 0.dp,
                                                    color = if (isUserTeam) Color(0xFF008E47).copy(alpha = 0.4f) else Color.Transparent,
                                                    shape = if (isLastRow) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = entry.position.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (entry.position <= 2) AccentEmerald else Color.LightGray,
                                                modifier = Modifier.width(20.dp)
                                            )
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(if (entry.position == 1) Color(0xFFC5A059) else Color(0xFF3A3A3C)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(entry.teamCode.take(2), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                                Text(
                                                    text = entry.teamName,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isUserTeam) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(text = entry.playedGames.toString(), fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                            Text(text = entry.won.toString(), fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                            Text(text = entry.draw.toString(), fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                            Text(text = entry.lost.toString(), fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                            Text(
                                                text = (if (entry.goalDifference > 0) "+" else "") + entry.goalDifference.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (entry.goalDifference > 0) AccentEmerald else if (entry.goalDifference < 0) Color.Red else Color.LightGray,
                                                modifier = Modifier.width(32.dp),
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = entry.points.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                modifier = Modifier.width(36.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 2) {
                    // TAB 2: Highlights
                    if (highlights.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No highlights available at this time.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "MATCH HIGHLIGHTS & VIDEO CORNER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC5A059),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(highlights) { highlight ->
                            var showPlays by remember { mutableStateOf(false) }
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF242427), RoundedCornerShape(12.dp))
                                    .clickable { showPlays = !showPlays }
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF008E47).copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = highlight.duration,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentEmerald
                                                )
                                            }
                                            Text(
                                                text = highlight.timestamp,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Icon(
                                            imageVector = if (showPlays) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle key plays",
                                            tint = Color.LightGray
                                        )
                                    }

                                    Text(
                                        text = highlight.matchTitle,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )

                                    Text(
                                        text = highlight.description,
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        lineHeight = 16.sp
                                    )

                                    // Button to play highlights
                                    var isPlayingVideo by remember { mutableStateOf(false) }
                                    if (isPlayingVideo) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = AccentEmerald, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Streaming Match Highlights...", fontSize = 11.sp, color = Color.White)
                                                TextButton(onClick = { isPlayingVideo = false }) {
                                                    Text("Close Player", color = Color.Red, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { isPlayingVideo = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008E47)),
                                            modifier = Modifier.fillMaxWidth().testTag("play_highlight_${highlight.id}")
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Text("Play Video Highlights", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    if (showPlays && highlight.plays.isNotEmpty()) {
                                        Divider(color = Color(0xFF242427))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "KEY PLAYS & EVENTS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC5A059)
                                            )
                                            highlight.plays.forEach { play ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFFC5A059), modifier = Modifier.size(12.dp))
                                                    Text(play, fontSize = 11.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 3: AI News Feed
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF242427), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsSoccer,
                                        contentDescription = "Soccer Ball",
                                        tint = Color(0xFFC5A059),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "WORLD CUP NEWS & LINEUPS AI FEED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFC5A059),
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                                Text(
                                    text = "Get real-time up-to-the-minute updates, player lineups, and injuries directly grounded from official sources using Google Search and Gemini.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )

                                // Search/Ask AI text field
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newsQuery,
                                        onValueChange = { newsQuery = it },
                                        placeholder = { Text("Search lineups, injuries, updates...", color = Color.Gray, fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f).testTag("news_search_field"),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF008E47),
                                            unfocusedBorderColor = Color(0xFF3E3E42)
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isNewsLoading = true
                                                newsError = null
                                                try {
                                                    val response = GeminiRepository.fetchWorldCupNewsGrounded(
                                                        if (newsQuery.isNotBlank()) newsQuery else "latest 2026 World Cup matches lineups injuries updates"
                                                    )
                                                    newsItemsList = parseNewsJson(response)
                                                } catch (e: Exception) {
                                                    newsError = e.localizedMessage ?: "Failed to fetch updates."
                                                } finally {
                                                    isNewsLoading = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008E47)),
                                        modifier = Modifier.height(56.dp).testTag("news_search_button")
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "Search news")
                                    }
                                }

                                // Quick tags row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val tags = listOf(
                                        "Latest Updates",
                                        "Injuries",
                                        "Lineups"
                                    )
                                    tags.forEach { tagLabel ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF1C1C1E))
                                                .border(1.dp, Color(0xFF3E3E42), RoundedCornerShape(16.dp))
                                                .clickable {
                                                    val queryText = when (tagLabel) {
                                                        "Injuries" -> "injury report player fitness and suspension updates for 2026 FIFA World Cup matches"
                                                        "Lineups" -> "starting team lineups squad formations and starting xi for recent ongoing 2026 World Cup matches"
                                                        else -> "latest ongoing 2026 FIFA World Cup matches results goals updates news"
                                                    }
                                                    newsQuery = tagLabel
                                                    scope.launch {
                                                        isNewsLoading = true
                                                        newsError = null
                                                        try {
                                                            val response = GeminiRepository.fetchWorldCupNewsGrounded(queryText)
                                                            newsItemsList = parseNewsJson(response)
                                                        } catch (e: Exception) {
                                                            newsError = e.localizedMessage ?: "Failed to fetch updates."
                                                        } finally {
                                                            isNewsLoading = false
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = tagLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isNewsLoading) {
                        items(3) {
                            NewsCardSkeleton()
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    } else if (newsError != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1C1C)),
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red)
                                    Column {
                                        Text("AI Grounding Warning", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                        Text(newsError ?: "Failed to reach search grounding service.", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else if (newsItemsList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No matching World Cup updates found. Try a different search query.", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(newsItemsList, key = { it.title }) { newsItem ->
                            val categoryColor = when (newsItem.category) {
                                "INJURY" -> WarningOrange
                                "LINEUP" -> AccentCyan
                                else -> AccentEmerald
                            }

                            val categoryBgColor = when (newsItem.category) {
                                "INJURY" -> WarningOrange.copy(alpha = 0.12f)
                                "LINEUP" -> AccentCyan.copy(alpha = 0.12f)
                                else -> AccentEmerald.copy(alpha = 0.12f)
                            }

                            val iconImage = when (newsItem.category) {
                                "INJURY" -> Icons.Default.Warning
                                "LINEUP" -> Icons.Default.Groups
                                else -> Icons.Default.SportsSoccer
                            }

                            var visible by remember(newsItem.title) { mutableStateOf(false) }
                            LaunchedEffect(newsItem.title) {
                                visible = true
                            }
                            val alpha by animateFloatAsState(
                                targetValue = if (visible) 1f else 0f,
                                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
                                label = "news_alpha"
                            )
                            val offsetY by animateDpAsState(
                                targetValue = if (visible) 0.dp else 12.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "news_offset"
                            )

                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(alpha)
                                    .offset(y = offsetY)
                                    .border(1.dp, if (isDarkMode) Color(0xFF242427) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
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
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(categoryBgColor)
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(iconImage, contentDescription = null, tint = categoryColor, modifier = Modifier.size(12.dp))
                                                    Text(
                                                        text = newsItem.category,
                                                        color = categoryColor,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            if (newsItem.impactLevel.isNotBlank()) {
                                                val impactColor = when (newsItem.impactLevel) {
                                                    "HIGH" -> EmergencyRed
                                                    "MEDIUM" -> WarningOrange
                                                    else -> Color.Gray
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .border(1.dp, impactColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${newsItem.impactLevel} IMPACT",
                                                        color = impactColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = newsItem.timestamp,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Text(
                                        text = newsItem.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    if (newsItem.teams.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .background(Color(0xFF141416), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SportsSoccer,
                                                contentDescription = null,
                                                tint = Color(0xFFC5A059),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = newsItem.teams,
                                                fontSize = 11.sp,
                                                color = Color.LightGray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = newsItem.summary,
                                        fontSize = 13.sp,
                                        color = Color.LightGray,
                                        lineHeight = 18.sp
                                    )

                                    Divider(color = Color(0xFF242427))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Verified via ${newsItem.source}",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTab == 4) {
                    // TAB 4: Dynamic Push Notifications & Favorite Teams Configuration
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Section Header
                            Text(
                                text = "FAVORITE TEAMS SELECTOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            
                            // Explanation
                            Text(
                                text = "Select your favorite teams to receive immediate real-time push notifications when goals are scored, cards are issued, or substitutions happen during their matches.",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )

                            val availableTeams = listOf(
                                Triple("USA", "United States", "🇺🇸"),
                                Triple("MEX", "Mexico", "🇲🇽"),
                                Triple("CAN", "Canada", "🇨🇦"),
                                Triple("ARG", "Argentina", "🇦🇷"),
                                Triple("FRA", "France", "🇫🇷"),
                                Triple("ENG", "England", "🇬🇧"),
                                Triple("GER", "Germany", "🇩🇪"),
                                Triple("BRA", "Brazil", "🇧🇷"),
                                Triple("URU", "Uruguay", "🇺🇾")
                            )

                            val favs by com.example.data.NotificationService.favouriteTeams.collectAsState()

                            // Grid of selectable teams
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableTeams.chunked(3).forEach { rowTeams ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowTeams.forEach { (code, name, flag) ->
                                            val isFav = favs.contains(code)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isFav) Color(0xFF005A31) else Color(0xFF1C1C1E))
                                                    .border(
                                                        1.dp, 
                                                        if (isFav) Color(0xFF008E47) else Color(0x33FFFFFF), 
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        if (isFav) {
                                                            com.example.data.NotificationService.removeFavouriteTeam(code)
                                                        } else {
                                                            com.example.data.NotificationService.addFavouriteTeam(code)
                                                        }
                                                    }
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(text = flag, fontSize = 28.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = code, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Text(text = name, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                if (isFav) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Favorited",
                                                        tint = Color.White,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .align(Alignment.TopEnd)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "NOTIFICATION CATEGORIES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )

                            val goalsOn by com.example.data.NotificationService.alertOnGoals.collectAsState()
                            val cardsOn by com.example.data.NotificationService.alertOnCards.collectAsState()
                            val subsOn by com.example.data.NotificationService.alertOnSubs.collectAsState()

                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⚽ Goals and Penalties", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Switch(
                                            checked = goalsOn,
                                            onCheckedChange = { com.example.data.NotificationService.alertOnGoals.value = it }
                                        )
                                    }
                                    Divider(color = Color(0x22FFFFFF))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🟨 Card Bookings (Yellow/Red)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Switch(
                                            checked = cardsOn,
                                            onCheckedChange = { com.example.data.NotificationService.alertOnCards.value = it }
                                        )
                                    }
                                    Divider(color = Color(0x22FFFFFF))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔄 Player Substitutions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Switch(
                                            checked = subsOn,
                                            onCheckedChange = { com.example.data.NotificationService.alertOnSubs.value = it }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "MATCH EVENT SIMULATOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            
                            Text(
                                text = "Simulate matches and instant game highlights. Push notifications will trigger dynamically ONLY for teams you favorited above!",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )

                            // Select which team to simulate for
                            var selectedSimTeam by remember { mutableStateOf("USA") }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("USA", "MEX", "ARG", "FRA", "ENG").forEach { team ->
                                    val isSel = selectedSimTeam == team
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF008E47) else Color.Transparent)
                                            .clickable { selectedSimTeam = team }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = team,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSel) Color.White else Color.Gray
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        MatchService.simulateGoalScoredForFav(selectedSimTeam, context)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008E47))
                                ) {
                                    Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sim Goal", fontSize = 11.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        MatchService.simulateCardForFav(selectedSimTeam, context)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00))
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sim Card", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        MatchService.simulateSubForFav(selectedSimTeam, context)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sim Sub", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ALERTS HISTORY LOG",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                val logList = com.example.data.NotificationService.notificationsLog.value
                                if (logList.isNotEmpty()) {
                                    Text(
                                        text = "Clear All",
                                        color = EmergencyRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { com.example.data.NotificationService.clearLog() }
                                    )
                                }
                            }

                            val alertsList by com.example.data.NotificationService.notificationsLog.collectAsState()

                            if (alertsList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No match alerts triggered yet.", color = Color.Gray, fontSize = 13.sp)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    alertsList.forEach { alert ->
                                        val alertColor = when (alert.type) {
                                            "GOAL" -> Color(0xFF008E47)
                                            "CARD" -> Color(0xFFFF8F00)
                                            "SUB" -> Color(0xFF0288D1)
                                            "EMERGENCY" -> Color(0xFFC62828)
                                            else -> Color.Gray
                                        }

                                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(alertColor)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = alert.title,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(
                                                            text = alert.timestamp,
                                                            color = Color.Gray,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = alert.message,
                                                        color = Color.LightGray,
                                                        fontSize = 12.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Match: ${alert.matchTitle}",
                                                        color = Color.Gray,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
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

                if (selectedTab == 5) {
                    // TAB 5: Sports API Live Tracker & Quick WebView Widget
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF242427), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "API Config",
                                        tint = Color(0xFFD4AF37),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "GLOBAL SPORTS API CONSOLE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFD4AF37),
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                                
                                Text(
                                    text = "Configure and fetch real-time sports JSON feeds using REST, or display quick widgets.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )

                                // API Provider Tabs/Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val providers = listOf(
                                        Pair("RAPIDAPI", "RapidAPI"),
                                        Pair("SPORTRADAR", "Sportradar"),
                                        Pair("CRICKETDATA", "CricketData"),
                                        Pair("CUSTOM_REST", "Custom REST")
                                    )
                                    providers.forEach { (src, label) ->
                                        val isSelected = selectedApiSource == src
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF008E47) else Color(0xFF1E1E22))
                                                .clickable {
                                                    selectedApiSource = src
                                                    // Auto-configure endpoints
                                                    when (src) {
                                                        "RAPIDAPI" -> {
                                                            customApiUrl = "https://api-football-v1.p.rapidapi.com/v3/fixtures?live=all"
                                                            customApiHost = "api-football-v1.p.rapidapi.com"
                                                        }
                                                        "SPORTRADAR" -> {
                                                            customApiUrl = "https://api.sportradar.com/soccer/trial/v4/en/schedules/live/summaries.json?api_key="
                                                            customApiHost = "api.sportradar.com"
                                                        }
                                                        "CRICKETDATA" -> {
                                                            customApiUrl = "https://api.cricapi.com/v1/currentMatches?apikey="
                                                            customApiHost = "api.cricapi.com"
                                                        }
                                                        "CUSTOM_REST" -> {
                                                            customApiUrl = "https://api.football-data.org/v4/matches"
                                                            customApiHost = "api.football-data.org"
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Inputs
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customApiUrl,
                                        onValueChange = { customApiUrl = it },
                                        label = { Text("REST API Endpoint URL", color = Color.Gray, fontSize = 11.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                        modifier = Modifier.fillMaxWidth().testTag("api_url_input"),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF008E47),
                                            unfocusedBorderColor = Color(0xFF3E3E42)
                                        )
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = customApiKey,
                                            onValueChange = { customApiKey = it },
                                            label = { Text("API Key / Token", color = Color.Gray, fontSize = 11.sp) },
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                            placeholder = { Text("Optional (pre-filled fallback used)", color = Color.DarkGray) },
                                            modifier = Modifier.weight(1f).testTag("api_key_input"),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF008E47),
                                                unfocusedBorderColor = Color(0xFF3E3E42)
                                            )
                                        )

                                        if (selectedApiSource == "RAPIDAPI") {
                                            OutlinedTextField(
                                                value = customApiHost,
                                                onValueChange = { customApiHost = it },
                                                label = { Text("API Host", color = Color.Gray, fontSize = 11.sp) },
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                                                modifier = Modifier.weight(1f).testTag("api_host_input"),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF008E47),
                                                    unfocusedBorderColor = Color(0xFF3E3E42)
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLiveApiLoading = true
                                                liveApiError = null
                                                try {
                                                    val url = customApiUrl.trim()
                                                    val host = customApiHost.trim()
                                                    val key = customApiKey.trim()
                                                    
                                                    val responseStr = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                                        val client = OkHttpClient()
                                                        val requestBuilder = Request.Builder().url(url)
                                                        if (key.isNotBlank()) {
                                                            if (selectedApiSource == "RAPIDAPI") {
                                                                requestBuilder.addHeader("X-RapidAPI-Key", key)
                                                                requestBuilder.addHeader("X-RapidAPI-Host", host)
                                                            } else if (selectedApiSource == "CUSTOM_REST") {
                                                                requestBuilder.addHeader("X-Auth-Token", key)
                                                                requestBuilder.addHeader("Authorization", "Bearer $key")
                                                            }
                                                        }
                                                        val req = requestBuilder.build()
                                                        client.newCall(req).execute().use { response ->
                                                            if (!response.isSuccessful) {
                                                                throw IOException("Response unsuccessful: ${response.code}")
                                                            }
                                                            response.body?.string() ?: ""
                                                        }
                                                    }
                                                    rawJsonResponse = responseStr
                                                    val parsed = parseLiveScoresJson(responseStr, selectedApiSource)
                                                    parsedLiveMatches = parsed
                                                    if (parsed.isEmpty()) {
                                                        liveApiError = "Query succeeded, but no live matches parsed. Please verify endpoint parameters."
                                                    } else {
                                                        Toast.makeText(context, "Fetched ${parsed.size} matches from real endpoint!", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("LiveMatchFeed", "REST Query failed", e)
                                                    liveApiError = "Query failed: ${e.localizedMessage}. Click 'LOAD DEMO REST PAYLOAD' below to test parser locally!"
                                                } finally {
                                                    isLiveApiLoading = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008E47)),
                                        modifier = Modifier.weight(1f).testTag("api_fetch_button"),
                                        enabled = !isLiveApiLoading
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Fetch API", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Fetch REST", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            // Load mock payload matching the chosen source
                                            val demoPayload = when (selectedApiSource) {
                                                "RAPIDAPI" -> MockSportsJson.API_FOOTBALL_JSON
                                                "SPORTRADAR" -> MockSportsJson.SPORTRADAR_JSON
                                                "CRICKETDATA" -> MockSportsJson.CRICKETDATA_JSON
                                                else -> MockSportsJson.API_FOOTBALL_JSON
                                            }
                                            rawJsonResponse = demoPayload
                                            parsedLiveMatches = parseLiveScoresJson(demoPayload, selectedApiSource)
                                            liveApiError = null
                                            Toast.makeText(context, "Loaded demo $selectedApiSource JSON locally!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.weight(1f).testTag("api_demo_button")
                                    ) {
                                        Icon(Icons.Default.BugReport, contentDescription = "Demo API", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Load Demo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Native Recycler view displaying live scores parsed from sports JSON
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NATIVE RECYCLER VIEW FEEDS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x1F00FF66))
                                        .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("REST Connected", color = Color(0xFF00FF66), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (isLiveApiLoading) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    repeat(3) {
                                        MatchCardSkeleton()
                                    }
                                }
                            } else if (liveApiError != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x1F2A0A0A)),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("REST Endpoint Notice", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(liveApiError ?: "", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }

                            if (parsedLiveMatches.isEmpty() && !isLiveApiLoading) {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No scores loaded. Select a provider above and click 'Fetch REST' or 'Load Demo' to bind parsed data to this Native View.",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                parsedLiveMatches.forEach { match ->
                                    GlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFF242427), RoundedCornerShape(12.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = match.competition,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC5A059)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF00FF66))
                                                    )
                                                    Text(
                                                        text = "${match.minute} - ${match.status}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF00FF66)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Home team
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0x1AFFFFFF)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(match.homeTeamCode, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text(match.homeTeam, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }

                                                // Scores
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                ) {
                                                    Text(
                                                        text = if (selectedApiSource == "CRICKETDATA") "" else match.homeScore.toString(),
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                    Text("-", fontSize = 14.sp, color = Color.Gray)
                                                    Text(
                                                        text = if (selectedApiSource == "CRICKETDATA") "" else match.awayScore.toString(),
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                }

                                                // Away team
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    Text(match.awayTeam, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0x1AFFFFFF)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(match.awayTeamCode, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            if (match.venue.isNotBlank()) {
                                                Text(
                                                    text = "📍 Venue: ${match.venue}",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            // Events (e.g. Scorers or wickets)
                                            if (match.events.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Divider(color = Color(0x1AFFFFFF))
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    match.events.forEach { ev ->
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.SportsSoccer,
                                                                contentDescription = "Event icon",
                                                                tint = Color(0xFFC5A059),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Text(ev, fontSize = 10.sp, color = Color.LightGray)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // WebView widget for quick display of responsive live scores
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "WEBVIEW RESPONSIVE QUICK WIDGET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF242427), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Loads interactive local HTML/JS/CSS live widgets designed to receive and draw simulated real-time WebSocket updates.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )

                                    // AndroidView wrapping a webview
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(280.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF030712))
                                    ) {
                                        AndroidView(
                                            factory = { context ->
                                                WebView(context).apply {
                                                    webViewClient = WebViewClient()
                                                    settings.javaScriptEnabled = true
                                                    settings.domStorageEnabled = true
                                                    
                                                    // Prepare html structure matching the chosen source
                                                    val htmlData = """
                                                        <!DOCTYPE html>
                                                        <html>
                                                        <head>
                                                            <meta name="viewport" content="width=device-width, initial-scale=1">
                                                            <style>
                                                                body {
                                                                    background-color: #030712;
                                                                    color: #ffffff;
                                                                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                                                                    margin: 0;
                                                                    padding: 10px;
                                                                    display: flex;
                                                                    flex-direction: column;
                                                                    align-items: center;
                                                                }
                                                                .widget-card {
                                                                    background: linear-gradient(135deg, #090d1e, #050814);
                                                                    border: 1px solid #c5a059;
                                                                    border-radius: 12px;
                                                                    padding: 14px;
                                                                    width: 95%;
                                                                    box-shadow: 0 4px 20px rgba(0,0,0,0.5);
                                                                    text-align: center;
                                                                }
                                                                .header {
                                                                    font-size: 10px;
                                                                    color: #c5a059;
                                                                    text-transform: uppercase;
                                                                    letter-spacing: 2px;
                                                                    margin-bottom: 8px;
                                                                    font-weight: bold;
                                                                }
                                                                .teams-container {
                                                                    display: flex;
                                                                    justify-content: space-around;
                                                                    align-items: center;
                                                                    margin: 10px 0;
                                                                }
                                                                .team {
                                                                    display: flex;
                                                                    flex-direction: column;
                                                                    align-items: center;
                                                                    width: 35%;
                                                                }
                                                                .team-flag {
                                                                    font-size: 32px;
                                                                    margin-bottom: 4px;
                                                                }
                                                                .team-name {
                                                                    font-size: 12px;
                                                                    font-weight: bold;
                                                                }
                                                                .score {
                                                                    font-size: 28px;
                                                                    font-weight: 800;
                                                                    color: #00ff66;
                                                                    letter-spacing: 2px;
                                                                    text-shadow: 0 0 8px rgba(0, 255, 102, 0.4);
                                                                }
                                                                .match-minute {
                                                                    font-size: 11px;
                                                                    color: #ffffff;
                                                                    background: rgba(0, 142, 71, 0.3);
                                                                    padding: 2px 8px;
                                                                    border-radius: 10px;
                                                                    margin-top: 4px;
                                                                    font-weight: bold;
                                                                    display: inline-block;
                                                                }
                                                                .live-tag {
                                                                    color: #ff3b30;
                                                                    font-size: 9px;
                                                                    font-weight: bold;
                                                                    text-transform: uppercase;
                                                                    display: flex;
                                                                    align-items: center;
                                                                    justify-content: center;
                                                                    gap: 4px;
                                                                }
                                                                .live-tag::before {
                                                                    content: '';
                                                                    width: 5px;
                                                                    height: 5px;
                                                                    background-color: #ff3b30;
                                                                    border-radius: 50%;
                                                                    display: inline-block;
                                                                    animation: blink 1s infinite;
                                                                }
                                                                @keyframes blink {
                                                                    0% { opacity: 0.2; }
                                                                    50% { opacity: 1; }
                                                                    100% { opacity: 0.2; }
                                                                }
                                                                .ticker-log {
                                                                    margin-top: 10px;
                                                                    border-top: 1px solid rgba(255, 255, 255, 0.1);
                                                                    padding-top: 8px;
                                                                    font-size: 10px;
                                                                    color: #a0aec0;
                                                                    text-align: left;
                                                                    height: 70px;
                                                                    overflow-y: auto;
                                                                }
                                                                .log-item {
                                                                    margin-bottom: 2px;
                                                                }
                                                            </style>
                                                        </head>
                                                        <body>
                                                            <div class="widget-card">
                                                                <div class="header">Rapid Sports API WebView Hub</div>
                                                                <div class="live-tag">WebSocket Live Syncing Active</div>
                                                                <div class="teams-container">
                                                                    <div class="team">
                                                                        <span class="team-flag">🇺🇸</span>
                                                                        <span class="team-name">United States</span>
                                                                    </div>
                                                                    <div style="display: flex; flex-direction: column; align-items: center;">
                                                                        <div class="score"><span id="home-score">2</span> - <span id="away-score">1</span></div>
                                                                        <div class="match-minute" id="match-minute">75'</div>
                                                                    </div>
                                                                    <div class="team">
                                                                        <span class="team-flag">🇩🇪</span>
                                                                        <span class="team-name">Germany</span>
                                                                    </div>
                                                                </div>
                                                                <div class="ticker-log" id="ticker-log">
                                                                    <div class="log-item">⚽ 12' - Goal USA! C. Pulisic with a stunner.</div>
                                                                    <div class="log-item">⚽ 34' - Goal GER! K. Havertz from close range.</div>
                                                                    <div class="log-item">⚽ 59' - Goal USA! T. Weah with a header.</div>
                                                                </div>
                                                            </div>

                                                            <script>
                                                                const events = [
                                                                    { type: 'goal', score: [3, 1], player: 'W. McKennie', min: "79'" },
                                                                    { type: 'card', player: 'E. Can (Yellow Card)', min: "84'" },
                                                                    { type: 'sub', player: 'B. Aaronson in for T. Weah', min: "88'" },
                                                                    { type: 'goal', score: [3, 2], player: 'N. Füllkrug', min: "90+2'" }
                                                                ];
                                                                let idx = 0;
                                                                setInterval(() => {
                                                                    if (idx < events.length) {
                                                                        const ev = events[idx];
                                                                        document.getElementById('match-minute').innerText = ev.min;
                                                                        let text = "";
                                                                        if (ev.type === 'goal') {
                                                                            document.getElementById('home-score').innerText = ev.score[0];
                                                                            document.getElementById('away-score').innerText = ev.score[1];
                                                                            text = "⚽ " + ev.min + " - Goal! " + ev.player;
                                                                        } else if (ev.type === 'card') {
                                                                            text = "🟨 " + ev.min + " - Card: " + ev.player;
                                                                        } else {
                                                                            text = "🔄 " + ev.min + " - Sub: " + ev.player;
                                                                        }
                                                                        const log = document.getElementById('ticker-log');
                                                                        const el = document.createElement('div');
                                                                        el.className = 'log-item';
                                                                        el.innerText = text;
                                                                        log.insertBefore(el, log.firstChild);
                                                                        idx++;
                                                                    }
                                                                }, 7000);
                                                            </script>
                                                        </body>
                                                        </html>
                                                    """.trimIndent()
                                                    
                                                    loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
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
}

data class WorldCupNewsItem(
    val title: String,
    val category: String, // UPDATE, INJURY, LINEUP
    val timestamp: String,
    val summary: String,
    val teams: String,
    val source: String,
    val impactLevel: String // HIGH, MEDIUM, LOW
)

fun parseNewsJson(jsonStr: String): List<WorldCupNewsItem> {
    val list = mutableListOf<WorldCupNewsItem>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                WorldCupNewsItem(
                    title = obj.optString("title", "No Title"),
                    category = obj.optString("category", "UPDATE").uppercase(),
                    timestamp = obj.optString("timestamp", "Just now"),
                    summary = obj.optString("summary", ""),
                    teams = obj.optString("teams", ""),
                    source = obj.optString("source", "FIFA Grounding"),
                    impactLevel = obj.optString("impactLevel", "LOW").uppercase()
                )
            )
        }
    } catch (e: Exception) {
        Log.e("LiveMatchFeed", "Error parsing news JSON: ", e)
    }
    return list
}

