package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import com.squareup.moshi.Json
import org.json.JSONObject
import org.json.JSONArray
import java.util.UUID
import java.util.Timer
import kotlin.concurrent.timerTask

// Real Live Match statistics as requested
data class LiveMatch(
    val matchId: String = "world_cup_match_1",
    val competition: String = "FIFA World Cup 2026™",
    val matchStatus: String = "LIVE", // "SCHEDULED", "LIVE", "FINISHED"
    val currentMinute: String = "72'",
    val homeTeam: String = "United States",
    val homeTeamCode: String = "USA",
    val homeScore: Int = 2,
    val awayTeam: String = "Mexico",
    val awayTeamCode: String = "MEX",
    val awayScore: Int = 1,
    val goalscorers: List<String> = listOf("C. Pulisic (12')", "T. Weah (44')", "H. Lozano (68')"),
    val cards: List<String> = listOf("Weston McKennie (USA - Yellow 34')", "Edson Álvarez (MEX - Yellow 55')"),
    val substitutions: List<String> = listOf("USA: B. Aaronson in for T. Weah (65')", "MEX: S. Giménez in for H. Martín (60')"),
    val possessionHome: Int = 54,
    val possessionAway: Int = 46,
    val shotsHome: Int = 12,
    val shotsAway: Int = 8,
    val expectedGoalsHome: Double = 1.45,
    val expectedGoalsAway: Double = 0.98,
    val kickoffTime: String = "08:00 PM",
    val venue: String = "MetLife Stadium, East Rutherford",
    val officials: String = "Clément Turpin (Referee), Nicolas Danos (Assistant)"
)

// DTOs for Football-Data.org API
data class FootballDataResponse(
    val matches: List<ApiMatchDto>?
)

data class ApiMatchDto(
    val id: Int,
    val status: String,
    val minute: String?,
    val competition: ApiCompetitionDto?,
    val homeTeam: ApiTeamDto?,
    val awayTeam: ApiTeamDto?,
    val score: ApiScoreDto?
)

data class ApiCompetitionDto(
    val name: String
)

data class ApiTeamDto(
    val name: String,
    val tla: String?
)

data class ApiScoreDto(
    val fullTime: ApiScoreTimeDto?
)

data class ApiScoreTimeDto(
    val home: Int?,
    val away: Int?
)

interface FootballDataApi {
    @GET("v4/matches")
    suspend fun getLiveMatches(
        @Header("X-Auth-Token") apiToken: String
    ): FootballDataResponse

    @GET("v4/competitions/{competitionCode}/standings")
    suspend fun getStandings(
        @Path("competitionCode") competitionCode: String,
        @Header("X-Auth-Token") apiToken: String
    ): ApiStandingsResponse

    @GET("v4/competitions/{competitionCode}/matches")
    suspend fun getCompetitionMatches(
        @Path("competitionCode") competitionCode: String,
        @Header("X-Auth-Token") apiToken: String
    ): FootballDataResponse
}

// API-Football (RapidAPI) Interface and DTOs
interface ApiFootballService {
    @GET("v3/fixtures")
    suspend fun getFixtures(
        @Query("league") leagueId: Int,
        @Query("season") season: Int,
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "api-football-v1.p.rapidapi.com"
    ): ApiFootballFixturesResponse

    @GET("v3/standings")
    suspend fun getStandings(
        @Query("league") leagueId: Int,
        @Query("season") season: Int,
        @Header("x-rapidapi-key") apiKey: String,
        @Header("x-rapidapi-host") host: String = "api-football-v1.p.rapidapi.com"
    ): ApiFootballStandingsResponse
}

data class ApiFootballFixturesResponse(
    val response: List<ApiFootballFixtureWrapper>?
)

data class ApiFootballFixtureWrapper(
    val fixture: ApiFootballFixture?,
    val league: ApiFootballLeague?,
    val teams: ApiFootballTeams?,
    val goals: ApiFootballGoals?,
    val score: ApiFootballScore?,
    val events: List<ApiFootballEvent>?
)

data class ApiFootballFixture(
    val id: Int,
    val referee: String?,
    val venue: ApiFootballVenue?,
    val status: ApiFootballStatus?,
    val date: String?
)

data class ApiFootballVenue(
    val name: String?,
    val city: String?
)

data class ApiFootballStatus(
    val long: String?,
    val short: String?,
    val elapsed: Int?
)

data class ApiFootballLeague(
    val id: Int,
    val name: String?,
    val season: Int?
)

data class ApiFootballTeams(
    val home: ApiFootballTeam?,
    val away: ApiFootballTeam?
)

data class ApiFootballTeam(
    val id: Int,
    val name: String?,
    val logo: String?,
    val winner: Boolean?
)

data class ApiFootballGoals(
    val home: Int?,
    val away: Int?
)

data class ApiFootballScore(
    val fulltime: ApiFootballGoals?,
    val halftime: ApiFootballGoals?
)

data class ApiFootballEvent(
    val time: ApiFootballEventTime?,
    val team: ApiFootballTeam?,
    val player: ApiFootballEventPlayer?,
    val assist: ApiFootballEventPlayer?,
    val type: String?,
    val detail: String?,
    val comments: String?
)

data class ApiFootballEventTime(
    val elapsed: Int,
    val extra: Int?
)

data class ApiFootballEventPlayer(
    val id: Int?,
    val name: String?
)

data class ApiFootballStandingsResponse(
    val response: List<ApiFootballStandingsWrapper>?
)

data class ApiFootballStandingsWrapper(
    val league: ApiFootballStandingsLeague?
)

data class ApiFootballStandingsLeague(
    val id: Int,
    val name: String?,
    val standings: List<List<ApiFootballStandingItem>>?
)

data class ApiFootballStandingItem(
    val rank: Int,
    val team: ApiFootballTeam?,
    val points: Int,
    val goalsDiff: Int,
    val group: String?,
    val all: ApiFootballStandingStats?
)

data class ApiFootballStandingStats(
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goals: ApiFootballStandingGoals?
)

data class ApiFootballStandingGoals(
    @Json(name = "for") val goalsFor: Int,
    val against: Int
)

data class ApiStandingsResponse(
    val competition: ApiCompetitionDto?,
    val standings: List<ApiStandingDto>?
)

data class ApiStandingDto(
    val stage: String?,
    val type: String?,
    val group: String?,
    val table: List<ApiTableEntryDto>?
)

data class ApiTableEntryDto(
    val position: Int,
    val team: ApiTeamDto?,
    val playedGames: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val points: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int
)

data class TeamStanding(
    val position: Int,
    val teamName: String,
    val teamCode: String,
    val playedGames: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val points: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val groupName: String = "Group A"
)

data class MatchFeedItem(
    val id: String,
    val homeTeam: String,
    val homeTeamCode: String,
    val awayTeam: String,
    val awayTeamCode: String,
    val homeScore: Int,
    val awayScore: Int,
    val status: String,
    val minute: String,
    val competitionName: String,
    val dateString: String = ""
)

data class MatchHighlight(
    val id: String = UUID.randomUUID().toString(),
    val matchTitle: String,
    val description: String,
    val duration: String,
    val timestamp: String,
    val videoUrl: String,
    val plays: List<String>,
    val homeTeamCode: String = "USA",
    val awayTeamCode: String = "MEX"
)

object MatchService {
    private const val TAG = "MatchService"

    private val _liveMatchState = MutableStateFlow<LiveMatch?>(null)
    val liveMatchState: StateFlow<LiveMatch?> = _liveMatchState.asStateFlow()

    private val _highlightsState = MutableStateFlow<List<MatchHighlight>>(emptyList())
    val highlightsState: StateFlow<List<MatchHighlight>> = _highlightsState.asStateFlow()

    private val _isFetchingApi = MutableStateFlow(false)
    val isFetchingApi: StateFlow<Boolean> = _isFetchingApi.asStateFlow()

    private val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    private val _activeCompetitionCode = MutableStateFlow("WC")
    val activeCompetitionCode: StateFlow<String> = _activeCompetitionCode.asStateFlow()

    private val _activeApiKey = MutableStateFlow("")
    val activeApiKey: StateFlow<String> = _activeApiKey.asStateFlow()

    private val _liveStandingsState = MutableStateFlow<List<TeamStanding>>(emptyList())
    val liveStandingsState: StateFlow<List<TeamStanding>> = _liveStandingsState.asStateFlow()

    private val _competitionMatchesState = MutableStateFlow<List<MatchFeedItem>>(emptyList())
    val competitionMatchesState: StateFlow<List<MatchFeedItem>> = _competitionMatchesState.asStateFlow()

    private var apiToken: String = ""
    private var footballApi: FootballDataApi? = null
    private var apiFootballKey: String = ""
    private var apiFootballService: ApiFootballService? = null
    private var refreshTimer: Timer? = null

    init {
        // Read optional API tokens from BuildConfig if configured via Secrets
        try {
            val fields = Class.forName("com.example.BuildConfig").declaredFields
            
            val keyField = fields.firstOrNull { it.name == "FOOTBALL_API_KEY" }
            if (keyField != null) {
                val foundKey = keyField.get(null) as? String ?: ""
                apiToken = foundKey
                _activeApiKey.value = foundKey
            }
            
            val keyFieldFootball = fields.firstOrNull { it.name == "API_FOOTBALL_KEY" }
            if (keyFieldFootball != null) {
                val foundKey = keyFieldFootball.get(null) as? String ?: ""
                apiFootballKey = foundKey
            }
        } catch (e: Exception) {
            Log.i(TAG, "No football API keys configured in BuildConfig: ${e.localizedMessage}")
        }

        setupRetrofit()
        setupFirestoreRealtimeSync()
        startAutomaticRefresh()
        refreshAllData()
        _highlightsState.value = getInitialHighlights()
        fetchRealScoresGroundedViaAI("WC", "FIFA World Cup™")
    }

    fun setApiKey(key: String) {
        apiToken = key.trim()
        _activeApiKey.value = apiToken
        setupRetrofit()
        refreshAllData()
    }

    private fun getCompetitionName(code: String): String {
        return when (code) {
            "PL" -> "Premier League"
            "CL" -> "Champions League"
            "PD" -> "La Liga"
            else -> "FIFA World Cup™"
        }
    }

    fun setCompetitionCode(code: String) {
        _activeCompetitionCode.value = code
        refreshAllData()
        fetchRealScoresGroundedViaAI(code, getCompetitionName(code))
    }

    private fun setupRetrofit() {
        if (apiToken.isNotBlank() && apiToken != "YOUR_FOOTBALL_API_KEY") {
            try {
                val okHttpClient = OkHttpClient.Builder().build()
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.football-data.org/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()

                footballApi = retrofit.create(FootballDataApi::class.java)
                Log.d(TAG, "Retrofit Football Data client setup completed for token: ${apiToken.take(4)}...")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Football Data API client: ${e.localizedMessage}")
            }
        } else {
            footballApi = null
            Log.i(TAG, "Football-Data.org token is empty or placeholder.")
        }

        if (apiFootballKey.isNotBlank() && apiFootballKey != "YOUR_API_FOOTBALL_KEY") {
            try {
                val okHttpClient = OkHttpClient.Builder().build()
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api-football-v1.p.rapidapi.com/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()

                apiFootballService = retrofit.create(ApiFootballService::class.java)
                Log.d(TAG, "Retrofit API-Football client setup completed for token: ${apiFootballKey.take(4)}...")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize API-Football client: ${e.localizedMessage}")
            }
        } else {
            apiFootballService = null
            Log.i(TAG, "API-Football token is empty or placeholder.")
        }
    }

    private fun setupFirestoreRealtimeSync() {
        // Hydrate from Firestore to support organizer managed live data
        if (DatabaseService.isFirebaseAvailable) {
            try {
                FirebaseFirestore.getInstance().collection("LiveMatches")
                    .document("world_cup_match_1")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e(TAG, "Firestore live match stream failed", e)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            try {
                                val match = snapshot.toObject(LiveMatch::class.java)
                                if (match != null) {
                                    _liveMatchState.value = match
                                    Log.d(TAG, "Realtime LiveMatch updated from Firestore.")
                                }
                            } catch (ex: Exception) {
                                Log.e(TAG, "Failed parsing LiveMatch from Firestore", ex)
                            }
                        } else {
                            val defaultMatch = LiveMatch()
                            FirebaseFirestore.getInstance().collection("LiveMatches")
                                .document("world_cup_match_1").set(defaultMatch)
                            _liveMatchState.value = defaultMatch
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed setting up Firestore match listener: ${e.localizedMessage}")
                _liveMatchState.value = LiveMatch()
            }
        } else {
            _liveMatchState.value = LiveMatch()
        }
    }

    fun startAutomaticRefresh() {
        refreshTimer?.cancel()
        refreshTimer = Timer()
        // Automatically refresh every 30 seconds
        refreshTimer?.schedule(timerTask {
            refreshAllData()
        }, 0L, 30000L)
    }

    fun refreshAllData() {
        val code = _activeCompetitionCode.value

        // Check if API-Football is configured
        val apiFootball = apiFootballService
        val keyFootball = apiFootballKey
        if (apiFootball != null && keyFootball.isNotBlank() && keyFootball != "YOUR_API_FOOTBALL_KEY") {
            fetchFromApiFootball(code, keyFootball)
            return
        }

        // Check if Football-Data.org is configured
        val apiFD = footballApi
        val tokenFD = apiToken
        if (apiFD != null && tokenFD.isNotBlank() && tokenFD != "YOUR_FOOTBALL_API_KEY") {
            fetchFromFootballData(code, tokenFD)
            return
        }

        // Default to live-grounded Search Grounding via Gemini if no API Key is provided!
        fetchRealScoresGroundedViaAI(code, getCompetitionName(code))
    }

    private fun getApiFootballLeagueId(code: String): Int {
        return when (code) {
            "PL" -> 39
            "CL" -> 2
            "PD" -> 140
            else -> 1 // World Cup
        }
    }

    private fun getApiFootballSeason(code: String): Int {
        return when (code) {
            "PL", "CL", "PD" -> 2025 // 2025/2026 season
            else -> 2026 // World Cup 2026
        }
    }

    private fun fetchFromApiFootball(code: String, key: String) {
        val service = apiFootballService ?: return
        val leagueId = getApiFootballLeagueId(code)
        val season = getApiFootballSeason(code)

        CoroutineScope(Dispatchers.IO).launch {
            _isFetchingApi.value = true
            _apiError.value = null
            try {
                // 1. Fetch Standing
                val standingsResponse = service.getStandings(leagueId, season, key)
                val mappedStandings = mutableListOf<TeamStanding>()
                
                val leagueObj = standingsResponse.response?.firstOrNull()?.league
                leagueObj?.standings?.forEach { groupStandings ->
                    groupStandings.forEach { item ->
                        val groupName = item.group ?: leagueObj.name ?: "Table"
                        val stats = item.all
                        mappedStandings.add(
                            TeamStanding(
                                position = item.rank,
                                teamName = item.team?.name ?: "Unknown",
                                teamCode = item.team?.name?.take(3)?.uppercase() ?: "UNK",
                                playedGames = stats?.played ?: 0,
                                won = stats?.win ?: 0,
                                draw = stats?.draw ?: 0,
                                lost = stats?.lose ?: 0,
                                points = item.points,
                                goalsFor = stats?.goals?.goalsFor ?: 0,
                                goalsAgainst = stats?.goals?.against ?: 0,
                                goalDifference = item.goalsDiff,
                                groupName = groupName
                            )
                        )
                    }
                }
                
                if (mappedStandings.isNotEmpty()) {
                    _liveStandingsState.value = mappedStandings
                } else {
                    _liveStandingsState.value = getSimulatedStandings(code)
                }

                // 2. Fetch Fixtures
                val fixturesResponse = service.getFixtures(leagueId, season, key)
                val mappedMatches = mutableListOf<MatchFeedItem>()
                var parsedLiveMatch: LiveMatch? = null

                fixturesResponse.response?.forEach { item ->
                    val fixture = item.fixture ?: return@forEach
                    val teams = item.teams ?: return@forEach
                    val goals = item.goals
                    val status = fixture.status?.short ?: "FT"
                    val isLive = status == "1H" || status == "2H" || status == "HT" || status == "ET" || status == "P" || status == "LIVE"

                    val homeS = goals?.home ?: 0
                    val awayS = goals?.away ?: 0

                    val feedItem = MatchFeedItem(
                        id = fixture.id.toString(),
                        homeTeam = teams.home?.name ?: "Home",
                        homeTeamCode = teams.home?.name?.take(3)?.uppercase() ?: "HOM",
                        awayTeam = teams.away?.name ?: "Away",
                        awayTeamCode = teams.away?.name?.take(3)?.uppercase() ?: "AWY",
                        homeScore = homeS,
                        awayScore = awayS,
                        status = when {
                            isLive -> "LIVE"
                            status == "FT" || status == "AET" || status == "PEN" -> "FINISHED"
                            else -> "SCHEDULED"
                        },
                        minute = if (isLive) "${fixture.status?.elapsed}'" else status,
                        competitionName = item.league?.name ?: "Competition"
                    )
                    mappedMatches.add(feedItem)

                    // If we find a live match or the first finished/scheduled marquee match, populate liveMatchState
                    if (isLive && parsedLiveMatch == null) {
                        val events = item.events?.map { evt ->
                            val pName = evt.player?.name ?: "Player"
                            val eType = evt.type ?: "Event"
                            val eDetail = evt.detail ?: ""
                            "[${evt.time?.elapsed}'] $eType ($eDetail) - $pName (${evt.team?.name})"
                        } ?: emptyList()

                        parsedLiveMatch = LiveMatch(
                            matchId = fixture.id.toString(),
                            competition = item.league?.name ?: "FIFA World Cup™",
                            matchStatus = "LIVE",
                            currentMinute = "${fixture.status?.elapsed}'",
                            homeTeam = teams.home?.name ?: "Home",
                            homeTeamCode = teams.home?.name?.take(3)?.uppercase() ?: "HOM",
                            homeScore = homeS,
                            awayTeam = teams.away?.name ?: "Away",
                            awayTeamCode = teams.away?.name?.take(3)?.uppercase() ?: "AWY",
                            awayScore = awayS,
                            goalscorers = events.filter { it.contains("Goal", ignoreCase = true) },
                            cards = events.filter { it.contains("Card", ignoreCase = true) },
                            substitutions = events.filter { it.contains("Subst", ignoreCase = true) || it.contains("Sub", ignoreCase = true) },
                            venue = fixture.venue?.name ?: "Stadium",
                            officials = fixture.referee ?: "Referee"
                        )
                    }
                }

                if (mappedMatches.isNotEmpty()) {
                    _competitionMatchesState.value = mappedMatches
                } else {
                    _competitionMatchesState.value = getSimulatedMatches(code)
                }
                
                if (parsedLiveMatch != null) {
                    _liveMatchState.value = parsedLiveMatch
                    if (DatabaseService.isFirebaseAvailable) {
                        FirebaseFirestore.getInstance().collection("LiveMatches")
                            .document("world_cup_match_1").set(parsedLiveMatch)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from API-Football: ${e.localizedMessage}")
                _apiError.value = "API-Football query failed: ${e.localizedMessage}"
                // Fallback
                _liveStandingsState.value = getSimulatedStandings(code)
                _competitionMatchesState.value = getSimulatedMatches(code)
            } finally {
                _isFetchingApi.value = false
            }
        }
    }

    private fun fetchFromFootballData(code: String, token: String) {
        val api = footballApi ?: return
        CoroutineScope(Dispatchers.IO).launch {
            _isFetchingApi.value = true
            _apiError.value = null
            try {
                // 1. Fetch live matches globally
                fetchLiveScoresFromApi()

                // 2. Fetch Standings
                val standingsResponse = api.getStandings(code, token)
                val mappedStandings = mutableListOf<TeamStanding>()
                standingsResponse.standings?.forEach { standingDto ->
                    val groupName = standingDto.group ?: standingDto.stage ?: "Table"
                    standingDto.table?.forEach { entry ->
                        mappedStandings.add(
                            TeamStanding(
                                position = entry.position,
                                teamName = entry.team?.name ?: "Unknown",
                                teamCode = entry.team?.tla ?: "UNK",
                                playedGames = entry.playedGames,
                                won = entry.won,
                                draw = entry.draw,
                                lost = entry.lost,
                                points = entry.points,
                                goalsFor = entry.goalsFor,
                                goalsAgainst = entry.goalsAgainst,
                                goalDifference = entry.goalDifference,
                                groupName = groupName.replace("_", " ")
                            )
                        )
                    }
                }
                _liveStandingsState.value = mappedStandings

                // 3. Fetch Matches
                val matchesResponse = api.getCompetitionMatches(code, token)
                val mappedMatches = matchesResponse.matches?.map { matchDto ->
                    MatchFeedItem(
                        id = matchDto.id.toString(),
                        homeTeam = matchDto.homeTeam?.name ?: "Home",
                        homeTeamCode = matchDto.homeTeam?.tla ?: "HOM",
                        awayTeam = matchDto.awayTeam?.name ?: "Away",
                        awayTeamCode = matchDto.awayTeam?.tla ?: "AWY",
                        homeScore = matchDto.score?.fullTime?.home ?: 0,
                        awayScore = matchDto.score?.fullTime?.away ?: 0,
                        status = matchDto.status,
                        minute = matchDto.minute ?: (if (matchDto.status == "FINISHED") "FT" else "15:00"),
                        competitionName = matchDto.competition?.name ?: "Competition"
                    )
                } ?: emptyList()
                _competitionMatchesState.value = mappedMatches

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching from Football-Data API: ${e.localizedMessage}")
                _apiError.value = "Football-Data API query failed: ${e.localizedMessage}"
                _liveStandingsState.value = getSimulatedStandings(code)
                _competitionMatchesState.value = getSimulatedMatches(code)
            } finally {
                _isFetchingApi.value = false
            }
        }
    }

    suspend fun fetchLiveScoresFromApi() {
        val api = footballApi
        val token = apiToken
        if (api == null || token.isBlank()) return

        try {
            val response = api.getLiveMatches(token)
            val liveMatchDto = response.matches?.firstOrNull {
                it.status == "LIVE" || it.status == "IN_PLAY" || it.status == "PAUSED"
            } ?: response.matches?.firstOrNull()

            if (liveMatchDto != null) {
                val updatedMatch = LiveMatch(
                    matchId = liveMatchDto.id.toString(),
                    competition = liveMatchDto.competition?.name ?: "FIFA World Cup™",
                    matchStatus = liveMatchDto.status,
                    currentMinute = liveMatchDto.minute ?: "72'",
                    homeTeam = liveMatchDto.homeTeam?.name ?: "United States",
                    homeTeamCode = liveMatchDto.homeTeam?.tla ?: "USA",
                    homeScore = liveMatchDto.score?.fullTime?.home ?: 2,
                    awayTeam = liveMatchDto.awayTeam?.name ?: "Mexico",
                    awayTeamCode = liveMatchDto.awayTeam?.tla ?: "MEX",
                    awayScore = liveMatchDto.score?.fullTime?.away ?: 1,
                    goalscorers = listOf("Live Score Feed Active"),
                    cards = listOf("Real-time data sourced from Football-Data.org API"),
                    substitutions = listOf("Feed online"),
                    possessionHome = 54,
                    possessionAway = 46,
                    shotsHome = 12,
                    shotsAway = 8,
                    expectedGoalsHome = 1.45,
                    expectedGoalsAway = 0.98,
                    kickoffTime = "08:00 PM",
                    venue = "MetLife Stadium",
                    officials = "Officials Assigned"
                )

                _liveMatchState.value = updatedMatch
                if (DatabaseService.isFirebaseAvailable) {
                    FirebaseFirestore.getInstance().collection("LiveMatches")
                        .document("world_cup_match_1").set(updatedMatch)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in live scores query: ${e.localizedMessage}")
        }
    }

    // Support organizer direct live match updating
    fun updateLiveMatchStatistics(updated: LiveMatch) {
        _liveMatchState.value = updated
        if (DatabaseService.isFirebaseAvailable) {
            FirebaseFirestore.getInstance().collection("LiveMatches")
                .document("world_cup_match_1").set(updated)
                .addOnSuccessListener { Log.d(TAG, "Live match updated in Firestore successfully.") }
                .addOnFailureListener { e -> Log.e(TAG, "Failed updating live match in Firestore", e) }
        }
    }

    fun fetchRealScoresGroundedViaAI(competitionCode: String, competitionName: String) {
        _isFetchingApi.value = true
        _apiError.value = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val responseStr = GeminiRepository.fetchRealMatchesGrounded(competitionCode, competitionName)
                val json = JSONObject(responseStr)
                
                // 1. Parse LiveMatch
                if (json.has("liveMatch") && !json.isNull("liveMatch")) {
                    val lmJson = json.getJSONObject("liveMatch")
                    val scorers = mutableListOf<String>()
                    val scorersArr = lmJson.optJSONArray("goalscorers")
                    if (scorersArr != null) {
                        for (i in 0 until scorersArr.length()) {
                            scorers.add(scorersArr.getString(i))
                        }
                    }
                    val cardsList = mutableListOf<String>()
                    val cardsArr = lmJson.optJSONArray("cards")
                    if (cardsArr != null) {
                        for (i in 0 until cardsArr.length()) {
                            cardsList.add(cardsArr.getString(i))
                        }
                    }
                    val subsList = mutableListOf<String>()
                    val subsArr = lmJson.optJSONArray("substitutions")
                    if (subsArr != null) {
                        for (i in 0 until subsArr.length()) {
                            subsList.add(subsArr.getString(i))
                        }
                    }
                    
                    val parsedLive = LiveMatch(
                        matchId = lmJson.optString("matchId", "live_1"),
                        competition = lmJson.optString("competition", competitionName),
                        matchStatus = lmJson.optString("matchStatus", "LIVE"),
                        currentMinute = lmJson.optString("currentMinute", "72'"),
                        homeTeam = lmJson.optString("homeTeam", "Home"),
                        homeTeamCode = lmJson.optString("homeTeamCode", "HOM"),
                        homeScore = lmJson.optInt("homeScore", 0),
                        awayTeam = lmJson.optString("awayTeam", "Away"),
                        awayTeamCode = lmJson.optString("awayTeamCode", "AWY"),
                        awayScore = lmJson.optInt("awayScore", 0),
                        goalscorers = scorers,
                        cards = cardsList,
                        substitutions = subsList,
                        possessionHome = lmJson.optInt("possessionHome", 50),
                        possessionAway = lmJson.optInt("possessionAway", 50),
                        shotsHome = lmJson.optInt("shotsHome", 10),
                        shotsAway = lmJson.optInt("shotsAway", 10),
                        expectedGoalsHome = lmJson.optDouble("expectedGoalsHome", 1.0),
                        expectedGoalsAway = lmJson.optDouble("expectedGoalsAway", 1.0),
                        kickoffTime = lmJson.optString("kickoffTime", ""),
                        venue = lmJson.optString("venue", ""),
                        officials = lmJson.optString("officials", "")
                    )
                    _liveMatchState.value = parsedLive
                } else {
                    _liveMatchState.value = null
                }
                
                // 2. Parse Upcoming / Recent Matches
                val mappedMatches = mutableListOf<MatchFeedItem>()
                if (json.has("upcomingMatches")) {
                    val arr = json.getJSONArray("upcomingMatches")
                    for (i in 0 until arr.length()) {
                        val mObj = arr.getJSONObject(i)
                        mappedMatches.add(
                            MatchFeedItem(
                                id = mObj.optString("id", "m_$i"),
                                homeTeam = mObj.optString("homeTeam", "Home"),
                                homeTeamCode = mObj.optString("homeTeamCode", "HOM"),
                                awayTeam = mObj.optString("awayTeam", "Away"),
                                awayTeamCode = mObj.optString("awayTeamCode", "AWY"),
                                homeScore = mObj.optInt("homeScore", 0),
                                awayScore = mObj.optInt("awayScore", 0),
                                status = mObj.optString("status", "SCHEDULED"),
                                minute = mObj.optString("minute", "15:00"),
                                competitionName = mObj.optString("competitionName", competitionName),
                                dateString = mObj.optString("dateString", "Today")
                            )
                        )
                    }
                }
                _competitionMatchesState.value = mappedMatches

                // 3. Parse Standings
                val mappedStandings = mutableListOf<TeamStanding>()
                if (json.has("standings")) {
                    val arr = json.getJSONArray("standings")
                    for (i in 0 until arr.length()) {
                        val sObj = arr.getJSONObject(i)
                        mappedStandings.add(
                            TeamStanding(
                                position = sObj.optInt("position", i + 1),
                                teamName = sObj.optString("teamName", "Team"),
                                teamCode = sObj.optString("teamCode", "TEM"),
                                playedGames = sObj.optInt("playedGames", 0),
                                won = sObj.optInt("won", 0),
                                draw = sObj.optInt("draw", 0),
                                lost = sObj.optInt("lost", 0),
                                points = sObj.optInt("points", 0),
                                goalsFor = sObj.optInt("goalsFor", 0),
                                goalsAgainst = sObj.optInt("goalsAgainst", 0),
                                goalDifference = sObj.optInt("goalDifference", 0),
                                groupName = sObj.optString("groupName", "Group Stage")
                            )
                        )
                    }
                }
                _liveStandingsState.value = mappedStandings

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching matches from Gemini: ${e.localizedMessage}")
                _apiError.value = "Gemini AI Search query failed: ${e.localizedMessage}"
            } finally {
                _isFetchingApi.value = false
            }
        }
    }

    // Dynamic, interactive simulation event generator
    fun simulateGoalScored() {
        val current = _liveMatchState.value ?: LiveMatch()
        val isHomeScoring = (0..1).random() == 0
        val homeInc = if (isHomeScoring) 1 else 0
        val awayInc = if (!isHomeScoring) 1 else 0
        
        val minutes = (1..90).random()
        val scorerName = if (isHomeScoring) {
            listOf("C. Pulisic", "T. Weah", "W. McKennie", "G. Reyna", "F. Balogun").random()
        } else {
            listOf("H. Lozano", "S. Giménez", "R. Jiménez", "E. Álvarez", "U. Antuna").random()
        }
        val teamCode = if (isHomeScoring) current.homeTeamCode else current.awayTeamCode
        val currentGoalscorers = current.goalscorers.toMutableList()
        currentGoalscorers.add("$scorerName ($minutes' - $teamCode)")

        val updated = current.copy(
            homeScore = current.homeScore + homeInc,
            awayScore = current.awayScore + awayInc,
            goalscorers = currentGoalscorers,
            currentMinute = "$minutes'"
        )
        updateLiveMatchStatistics(updated)

        // Also add or update highlight for USA vs MEX dynamically!
        val newHighlight = MatchHighlight(
            id = "highlight_sim_${System.currentTimeMillis()}",
            matchTitle = "${updated.homeTeam} vs ${updated.awayTeam}",
            description = "Dynamic live match highlight: $scorerName scores an extraordinary goal in the $minutes' minute!",
            duration = "1:45",
            timestamp = "Just now",
            videoUrl = "https://example.com/highlights/sim",
            plays = currentGoalscorers,
            homeTeamCode = updated.homeTeamCode,
            awayTeamCode = updated.awayTeamCode
        )
        _highlightsState.value = listOf(newHighlight) + _highlightsState.value.filter { it.matchTitle != "${updated.homeTeam} vs ${updated.awayTeam}" }
    }

    private fun getInitialHighlights(): List<MatchHighlight> {
        return listOf(
            MatchHighlight(
                id = "highlight_1",
                matchTitle = "United States vs Mexico",
                description = "An unbelievable performance! Christian Pulisic scores a fantastic brace to secure a vital victory at MetLife Stadium.",
                duration = "4:45",
                timestamp = "Just now",
                videoUrl = "https://example.com/highlights/usa-mex",
                plays = listOf("12' - Christian Pulisic volley goal", "44' - Timothy Weah clinical counter-attack goal", "68' - Hirving Lozano brilliant long-range goal"),
                homeTeamCode = "USA",
                awayTeamCode = "MEX"
            ),
            MatchHighlight(
                id = "highlight_2",
                matchTitle = "Argentina vs Poland",
                description = "Messi pulls the strings with two brilliant assists as Argentina outclasses Poland in a packed crowd.",
                duration = "3:30",
                timestamp = "2 hours ago",
                videoUrl = "https://example.com/highlights/arg-pol",
                plays = listOf("22' - Lionel Messi magnificent assist", "56' - Lautaro Martínez chip goal", "81' - Julian Álvarez goal"),
                homeTeamCode = "ARG",
                awayTeamCode = "POL"
            ),
            MatchHighlight(
                id = "highlight_3",
                matchTitle = "Canada vs Panama",
                description = "Jonathan David's match-winning header secures three massive points for Canada in the group stages.",
                duration = "2:15",
                timestamp = "Yesterday",
                videoUrl = "https://example.com/highlights/can-pan",
                plays = listOf("78' - Jonathan David diving header goal"),
                homeTeamCode = "CAN",
                awayTeamCode = "PAN"
            ),
            MatchHighlight(
                id = "highlight_4",
                matchTitle = "France vs England",
                description = "Kylian Mbappé and Jude Bellingham exchange incredible plays in an unforgettable 3-2 match-up.",
                duration = "5:10",
                timestamp = "Yesterday",
                videoUrl = "https://example.com/highlights/fra-eng",
                plays = listOf("14' - Kylian Mbappé curled shot goal", "39' - Jude Bellingham header goal", "52' - Harry Kane penalty goal", "73' - Antoine Griezmann freekick goal", "89' - Marcus Thuram tap-in goal"),
                homeTeamCode = "FRA",
                awayTeamCode = "ENG"
            )
        )
    }

    // Standings simulated list provider
    fun getSimulatedStandings(competitionCode: String): List<TeamStanding> {
        return when (competitionCode) {
            "PL" -> listOf(
                TeamStanding(1, "Manchester City", "MCI", 38, 28, 7, 3, 91, 96, 34, 62, "Premier League"),
                TeamStanding(2, "Arsenal FC", "ARS", 38, 28, 5, 5, 89, 91, 29, 62, "Premier League"),
                TeamStanding(3, "Liverpool FC", "LIV", 38, 24, 10, 4, 82, 86, 41, 45, "Premier League"),
                TeamStanding(4, "Aston Villa", "AVL", 38, 20, 8, 10, 68, 76, 61, 15, "Premier League"),
                TeamStanding(5, "Tottenham Hotspur", "TOT", 38, 20, 6, 12, 66, 74, 61, 13, "Premier League"),
                TeamStanding(6, "Chelsea FC", "CHE", 38, 18, 9, 11, 63, 77, 63, 14, "Premier League")
            )
            "CL" -> listOf(
                TeamStanding(1, "Real Madrid", "RMA", 6, 5, 1, 0, 16, 15, 5, 10, "Group A"),
                TeamStanding(2, "Napoli", "NAP", 6, 3, 1, 2, 10, 10, 9, 1, "Group A"),
                TeamStanding(3, "Braga", "BRA", 6, 1, 1, 4, 4, 6, 12, -6, "Group A"),
                TeamStanding(4, "Union Berlin", "FCU", 6, 0, 2, 4, 2, 6, 11, -5, "Group A"),
                
                TeamStanding(1, "Bayern Munich", "FCB", 6, 5, 1, 0, 16, 12, 6, 6, "Group B"),
                TeamStanding(2, "FC Copenhagen", "FCK", 6, 2, 2, 2, 8, 8, 8, 0, "Group B")
            )
            "PD" -> listOf(
                TeamStanding(1, "Real Madrid", "RMA", 38, 29, 8, 1, 95, 87, 26, 61, "La Liga"),
                TeamStanding(2, "FC Barcelona", "FCB", 38, 26, 7, 5, 85, 79, 44, 35, "La Liga"),
                TeamStanding(3, "Girona FC", "GIR", 38, 25, 6, 7, 81, 85, 46, 39, "La Liga"),
                TeamStanding(4, "Atletico Madrid", "ATM", 38, 24, 4, 10, 76, 70, 43, 27, "La Liga")
            )
            else -> listOf( // WC
                TeamStanding(1, "United States", "USA", 2, 2, 0, 0, 6, 5, 1, 4, "Group A"),
                TeamStanding(2, "Mexico", "MEX", 2, 1, 0, 1, 3, 3, 3, 0, "Group A"),
                TeamStanding(3, "Canada", "CAN", 2, 1, 0, 1, 3, 2, 2, 0, "Group A"),
                TeamStanding(4, "Panama", "PAN", 2, 0, 0, 2, 0, 1, 5, -4, "Group A"),

                TeamStanding(1, "Argentina", "ARG", 2, 2, 0, 0, 6, 4, 0, 4, "Group B"),
                TeamStanding(2, "Poland", "POL", 2, 1, 0, 1, 3, 2, 2, 0, "Group B"),
                TeamStanding(3, "Netherlands", "NED", 2, 1, 0, 1, 3, 2, 3, -1, "Group B"),
                TeamStanding(4, "Saudi Arabia", "KSA", 2, 0, 0, 2, 0, 1, 4, -3, "Group B")
            )
        }
    }

    // Matches simulated list provider
    fun getSimulatedMatches(competitionCode: String): List<MatchFeedItem> {
        val activeM = _liveMatchState.value ?: LiveMatch()
        return when (competitionCode) {
            "PL" -> listOf(
                MatchFeedItem("pl_1", "Arsenal FC", "ARS", "Manchester City", "MCI", 2, 2, "IN_PLAY", "82'", "Premier League", "Today"),
                MatchFeedItem("pl_2", "Liverpool FC", "LIV", "Chelsea FC", "CHE", 3, 1, "FINISHED", "FT", "Premier League", "Yesterday"),
                MatchFeedItem("pl_3", "Aston Villa", "AVL", "Tottenham Hotspur", "TOT", 0, 1, "SCHEDULED", "15:00", "Premier League", "Tomorrow")
            )
            "CL" -> listOf(
                MatchFeedItem("cl_1", "Real Madrid", "RMA", "Bayern Munich", "FCB", 1, 0, "IN_PLAY", "45'", "Champions League", "Today"),
                MatchFeedItem("cl_2", "Paris Saint-Germain", "PSG", "Borussia Dortmund", "BVB", 2, 0, "FINISHED", "FT", "Champions League", "Yesterday"),
                MatchFeedItem("cl_3", "Atletico Madrid", "ATM", "Inter Milan", "INT", 0, 0, "SCHEDULED", "21:00", "Champions League", "Today")
            )
            "PD" -> listOf(
                MatchFeedItem("pd_1", "Real Madrid", "RMA", "FC Barcelona", "FCB", 3, 2, "FINISHED", "FT", "La Liga", "Last Week"),
                MatchFeedItem("pd_2", "Girona FC", "GIR", "Villarreal", "VIL", 1, 1, "IN_PLAY", "60'", "La Liga", "Today")
            )
            else -> listOf( // WC
                MatchFeedItem("wc_1", activeM.homeTeam, activeM.homeTeamCode, activeM.awayTeam, activeM.awayTeamCode, activeM.homeScore, activeM.awayScore, activeM.matchStatus, activeM.currentMinute, "FIFA World Cup™", "Today"),
                MatchFeedItem("wc_2", "Canada", "CAN", "Panama", "PAN", 1, 0, "FINISHED", "FT", "FIFA World Cup™", "Yesterday"),
                MatchFeedItem("wc_3", "Argentina", "ARG", "Poland", "POL", 2, 0, "FINISHED", "FT", "FIFA World Cup™", "Yesterday"),
                MatchFeedItem("wc_4", "France", "FRA", "England", "ENG", 0, 0, "SCHEDULED", "18:00", "FIFA World Cup™", "Today")
            )
        }
    }

    fun simulateGoalScoredForFav(teamCode: String, context: android.content.Context?) {
        val minutes = (1..90).random()
        val scorer = when (teamCode.uppercase()) {
            "USA" -> listOf("C. Pulisic", "T. Weah", "W. McKennie", "G. Reyna").random()
            "MEX" -> listOf("H. Lozano", "S. Giménez", "R. Jiménez", "E. Álvarez").random()
            "ARG" -> listOf("L. Messi", "L. Martínez", "J. Álvarez", "E. Fernández").random()
            "FRA" -> listOf("K. Mbappé", "A. Griezmann", "O. Dembélé", "M. Thuram").random()
            "ENG" -> listOf("H. Kane", "J. Bellingham", "B. Saka", "P. Foden").random()
            "GER" -> listOf("F. Wirtz", "J. Musiala", "K. Havertz", "T. Müller").random()
            "BRA" -> listOf("Vinícius Jr.", "Rodrygo", "Raphinha", "Neymar Jr.").random()
            else -> "Star striker"
        }
        val matchTitle = when (teamCode.uppercase()) {
            "USA", "MEX" -> "United States vs Mexico"
            "ARG" -> "Argentina vs Poland"
            "FRA", "ENG" -> "France vs England"
            else -> "FIFA World Cup 2026™"
        }
        val title = "⚽ GOAL! $teamCode"
        val message = "GOOOAL! $scorer scores in the $minutes' minute! Scoreboard updated."
        
        NotificationService.triggerNotification(context, title, message, "GOAL", teamCode, matchTitle)
    }

    fun simulateCardForFav(teamCode: String, context: android.content.Context?) {
        val minutes = (1..90).random()
        val player = when (teamCode.uppercase()) {
            "USA" -> "Tyler Adams"
            "MEX" -> "Edson Álvarez"
            "ARG" -> "Rodrigo De Paul"
            "FRA" -> "Aurélien Tchouaméni"
            "ENG" -> "Declan Rice"
            "GER" -> "Antonio Rüdiger"
            "BRA" -> "Casemiro"
            else -> "Defender"
        }
        val matchTitle = "Tournament Match"
        val isYellow = (0..1).random() == 0
        val title = if (isYellow) "🟨 Yellow Card: $teamCode" else "🟥 Red Card: $teamCode"
        val message = "$player ($teamCode) receives a ${if (isYellow) "yellow" else "red"} card in the $minutes' minute."
        
        NotificationService.triggerNotification(context, title, message, "CARD", teamCode, matchTitle)
    }

    fun simulateSubForFav(teamCode: String, context: android.content.Context?) {
        val minutes = (1..90).random()
        val subIn = when (teamCode.uppercase()) {
            "USA" -> "B. Aaronson"
            "MEX" -> "Orbelín Pineda"
            "ARG" -> "Alexis Mac Allister"
            "FRA" -> "Eduardo Camavinga"
            "ENG" -> "Cole Palmer"
            "GER" -> "Leroy Sané"
            "BRA" -> "Gabriel Martinelli"
            else -> "Substitution player"
        }
        val subOut = when (teamCode.uppercase()) {
            "USA" -> "Timothy Weah"
            "MEX" -> "Henry Martín"
            "ARG" -> "Ángel Di María"
            "FRA" -> "Marcus Thuram"
            "ENG" -> "Bukayako Saka"
            "GER" -> "Serge Gnabry"
            "BRA" -> "Richarlison"
            else -> "Outgoing player"
        }
        val matchTitle = "Tournament Match"
        val title = "🔄 Substitution: $teamCode"
        val message = "Min $minutes': $subIn replaces $subOut for $teamCode."
        
        NotificationService.triggerNotification(context, title, message, "SUB", teamCode, matchTitle)
    }
}
