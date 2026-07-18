package com.example.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object FootballPhotos {
    const val HERO_STADIUM = "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?q=80&w=1200"
    const val MATCH_BALL = "https://images.unsplash.com/photo-1518152006812-edab29b069ac?q=80&w=600"
    const val WORLD_CUP_TROPHY = "https://images.unsplash.com/photo-1578269174936-2709b5a19368?q=80&w=600"
    const val PITCH_TEXTURE = "https://images.unsplash.com/photo-1531415074968-036ba1b575da?q=80&w=1200"
    const val PLAYER_ACTION_1 = "https://images.unsplash.com/photo-1518063319789-7217e6706b04?q=80&w=600"
    const val MATCH_ACTION_1 = "https://images.unsplash.com/photo-1431324155629-1a6edd1d1418?q=80&w=800"
}

enum class Language(val displayName: String, val code: String, val greeting: String) {
    ENGLISH("English", "en", "Welcome to MetLife Stadium!"),
    SPANISH("Español", "es", "¡Bienvenido al Estadio MetLife!"),
    FRENCH("Français", "fr", "Bienvenue au Stade MetLife!"),
    PORTUGUESE("Português", "pt", "Bem-vindo ao Estádio MetLife!"),
    ARABIC("العربية", "ar", "مرحباً بكم في استاد ميتلايف!"),
    HINDI("हिन्दी", "hi", "मेटलाइफ स्टेडियम में आपका स्वागत है!"),
    JAPANESE("日本語", "ja", "メットライフ・スタジアムへようこそ！"),
    GERMAN("Deutsch", "de", "Willkommen im MetLife-Stadion!"),
    ITALIAN("Italiano", "it", "Benvenuto al MetLife Stadium!"),
    MARATHI("मराठी", "mr", "मेटलाइफ स्टेडियममध्ये आपले स्वागत आहे!")
}

enum class Severity(val color: Long) {
    LOW(0xFF00C853),       // Green
    MEDIUM(0xFFFF9100),    // Orange
    CRITICAL(0xFFFF1744)   // Red
}

enum class IncidentStatus {
    REPORTED, INVESTIGATING, RESOLVED
}

data class IncidentReport(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val location: String,
    val severity: Severity,
    val status: IncidentStatus = IncidentStatus.REPORTED,
    val timestamp: String = "05:13 PM"
)

data class VolunteerTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val location: String,
    val isCompleted: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: String = "Just now"
)

data class FoodStall(
    val name: String,
    val category: String,
    val location: String,
    val waitTimeMinutes: Int,
    val hasRecyclingBonus: Boolean,
    val carbonSavingsKg: Double,
    val image: String
)

enum class MatchPhase(val displayName: String, val desc: String) {
    GATES_OPEN("Gates Open", "Fans starting to arrive, ticket scanning and bag checks active."),
    PRE_MATCH("Pre Match", "Opening ceremonies, national anthems. Maximum entrance density."),
    KICKOFF("Kickoff / First Half", "Match underway. Main concourse clear, food stall lines low."),
    HALFTIME("Halftime", "Peak restroom and concession queue growth. High noise levels."),
    SECOND_HALF("Second Half", "Match drama peaks. Medical demand and security vigilance high."),
    EMERGENCY("Emergency Alert", "Active tactical evacuation and responder coordination active."),
    POST_MATCH("Post Match", "Fans egressing. Main exits heavily loaded, transit express active.")
}

enum class UserRole(val displayName: String) {
    ORGANIZER("Stadium Director (Organizer)"),
    VOLUNTEER("Field Support (Volunteer)"),
    FAN("Spectator (Fan)")
}

data class ProactiveAlert(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: String, // "INFO", "WARNING", "CRITICAL"
    val timestamp: String = "18:19",
    val phase: MatchPhase
)

data class VisionAnalysisResult(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val priority: String,
    val confidence: Double,
    val action: String,
    val suggestedTeam: String,
    val location: String = "Sector 114"
)

object AppState {
    // --- 1. Language & Accessibility State ---
    val currentLanguage = MutableStateFlow(Language.ENGLISH)
    val highContrastMode = MutableStateFlow(false)
    val isDarkMode = MutableStateFlow(true)
    val voiceOutputEnabled = MutableStateFlow(false)
    val textScale = MutableStateFlow(1.0f) // Scalable font text helper

    // --- 2. User & Fan State ---
    val fanName = MutableStateFlow("David Beckham")
    val fanSeat = MutableStateFlow("Sec 114, Row M, Seat 8")
    val ecoRewardPoints = MutableStateFlow(320)
    val carbonSavedKg = MutableStateFlow(12.4)

    // --- 3. Chat Messages (Generative Assistant Screen) ---
    private val _chatMessages = MutableStateFlow(listOf(
        ChatMessage(isUser = false, text = "Hello Cristiano! I'm your FIFA AI Stadium Companion. I can help with directions, queue lengths, eco-rewards, translations, or emergency coordination. What do you need?")
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun addChatMessage(message: ChatMessage) {
        _chatMessages.value = _chatMessages.value + message
    }

    // --- 4. Centralized Global AI Brain Settings ---
    val currentMatchPhase = MutableStateFlow(MatchPhase.PRE_MATCH)
    val currentUserRole = MutableStateFlow(UserRole.FAN)
    val demoModeEnabled = MutableStateFlow(false)

    // --- 5. Proactive Alerts State ---
    private val _proactiveAlerts = MutableStateFlow(listOf(
        ProactiveAlert(
            title = "Gate C Wait Time Warning",
            message = "Gate C arrival flow has exceeded optimal entry threshold by 12%. Recommend re-routing fans.",
            type = "WARNING",
            timestamp = "06:15 PM",
            phase = MatchPhase.PRE_MATCH
        ),
        ProactiveAlert(
            title = "Medical Station 2 Load Spike",
            message = "Heatstroke reports spiking near Sector 220. Auxiliary hydration carts dispatched.",
            type = "INFO",
            timestamp = "06:17 PM",
            phase = MatchPhase.PRE_MATCH
        )
    ))
    val proactiveAlerts: StateFlow<List<ProactiveAlert>> = _proactiveAlerts.asStateFlow()

    fun addProactiveAlert(title: String, message: String, type: String) {
        val newAlert = ProactiveAlert(
            title = title,
            message = message,
            type = type,
            timestamp = "06:19 PM",
            phase = currentMatchPhase.value
        )
        _proactiveAlerts.value = listOf(newAlert) + _proactiveAlerts.value
    }

    fun clearAlerts() {
        _proactiveAlerts.value = emptyList()
    }

    // --- 6. Interactive Live Operational Controls ---
    val gateAVolunteers = MutableStateFlow(2)
    val gateBVolunteers = MutableStateFlow(5)
    val gateCVolunteers = MutableStateFlow(1)

    fun getGateWaitTime(gate: String): Int {
        return when (gate) {
            "Gate A (North)" -> maxOf(2, 20 - (gateAVolunteers.value * 3))
            "Gate B (South)" -> maxOf(2, 18 - (gateBVolunteers.value * 2))
            "Gate C (East)" -> maxOf(2, 15 - (gateCVolunteers.value * 4))
            "Gate A (North Entrance)" -> maxOf(2, 20 - (gateAVolunteers.value * 3))
            "Gate B (South Plaza)" -> maxOf(2, 18 - (gateBVolunteers.value * 2))
            "Gate C (East Concourse)" -> maxOf(2, 15 - (gateCVolunteers.value * 4))
            else -> 5
        }
    }

    // --- 7. Incident Reports List ---
    private val _incidents = MutableStateFlow(listOf(
        IncidentReport(
            title = "Water Spill Concourse Section 108",
            description = "Slip hazard noticed near the pretzel stand. Requires cleaning.",
            location = "Concourse Level 1, Sec 108",
            severity = Severity.LOW,
            status = IncidentStatus.INVESTIGATING
        ),
        IncidentReport(
            title = "Lost Child (Wearing Brazil Jersey)",
            description = "Child separated from parents near Gate B entrance. Wearing yellow Neymar jersey, age 7.",
            location = "Gate B Plaza",
            severity = Severity.CRITICAL,
            status = IncidentStatus.REPORTED
        )
    ))
    val incidents: StateFlow<List<IncidentReport>> = _incidents.asStateFlow()

    fun reportIncident(title: String, description: String, location: String, severity: Severity) {
        val newReport = IncidentReport(
            title = title,
            description = description,
            location = location,
            severity = severity
        )
        _incidents.value = listOf(newReport) + _incidents.value
    }

    fun updateIncidentStatus(id: String, newStatus: IncidentStatus) {
        _incidents.value = _incidents.value.map {
            if (it.id == id) it.copy(status = newStatus) else it
        }
    }

    // --- 8. Volunteer Tasks List ---
    private val _volunteerTasks = MutableStateFlow(listOf(
        VolunteerTask(
            title = "Distribute zero-waste flyers",
            description = "Hand out sustainability guidelines near East gates.",
            location = "Gate C Plaza"
        ),
        VolunteerTask(
            title = "Assist elderly guest to Elevator B",
            description = "Accompany guest from Section 114 to Elevator Level 3.",
            location = "Section 114 Corridor"
        ),
        VolunteerTask(
            title = "Monitor water refill station",
            description = "Ensure dispenser at Sec 112 is clean and full.",
            location = "Section 112 Water Fountain",
            isCompleted = true
        )
    ))
    val volunteerTasks: StateFlow<List<VolunteerTask>> = _volunteerTasks.asStateFlow()

    fun completeTask(id: String) {
        _volunteerTasks.value = _volunteerTasks.value.map {
            if (it.id == id) it.copy(isCompleted = true) else it
        }
    }

    fun addVolunteerTask(title: String, description: String, location: String) {
        val newTask = VolunteerTask(title = title, description = description, location = location)
        _volunteerTasks.value = listOf(newTask) + _volunteerTasks.value
    }

    // --- 9. Food Stalls List ---
    val foodStalls = listOf(
        FoodStall(
            name = "Organic Kickoff",
            category = "Burgers & Plant-Based",
            location = "Section 114",
            waitTimeMinutes = 4,
            hasRecyclingBonus = true,
            carbonSavingsKg = 1.2,
            image = "organic_kickoff"
        ),
        FoodStall(
            name = "Eco-Grill Express",
            category = "Sausages & Wraps",
            location = "Section 102",
            waitTimeMinutes = 11,
            hasRecyclingBonus = true,
            carbonSavingsKg = 0.8,
            image = "eco_grill"
        ),
        FoodStall(
            name = "Stadium Slice",
            category = "Pizza & Refreshments",
            location = "Section 124",
            waitTimeMinutes = 15,
            hasRecyclingBonus = false,
            carbonSavingsKg = 0.0,
            image = "stadium_slice"
        )
    )

    // --- 10. Lost & Found State ---
    data class LostItem(
        val id: String = UUID.randomUUID().toString(),
        val itemName: String,
        val category: String,
        val lastSeenLocation: String,
        val description: String,
        val status: String = "Reported"
    )

    val lostItems = MutableStateFlow(listOf(
        LostItem(itemName = "Leather Wallet", category = "Personal Valuables", lastSeenLocation = "Sec 114, Row F", description = "Brown leather wallet containing ID and souvenir card."),
        LostItem(itemName = "iPhone 15 Pro", category = "Electronics", lastSeenLocation = "Washroom near Gate A", description = "Titanium blue, with clear soccer-patterned case.")
    ))

    fun reportLostItem(name: String, category: String, location: String, desc: String) {
        val newItem = LostItem(itemName = name, category = category, lastSeenLocation = location, description = desc)
        lostItems.value = listOf(newItem) + lostItems.value
    }

    private data class VisionMockDetails(
        val priority: String,
        val confidence: Double,
        val action: String,
        val team: String,
        val reportTitle: String,
        val reportDesc: String
    )

    // --- 11. AI Vision Simulation ---
    fun simulateVisionAnalysis(photoCategory: String, location: String = "Sector 114"): VisionAnalysisResult {
        val details = when (photoCategory) {
            "Trash Overflow" -> VisionMockDetails(
                "MEDIUM", 0.96, "Deploy cleaning crew with heavy-duty zero-waste sorting bins.", "Sanitation Team",
                "Severe Trash Build-up detected via AI Vision", "An overflow of plastic souvenir cups and packaging is blocking concourse recycling points."
            )
            "Blocked Exit" -> VisionMockDetails(
                "CRITICAL", 0.99, "Clear obstacles instantly. Notify fire warden.", "Safety Stewards",
                "Emergency Exit Blocked by promotional banners", "Promotional scaffolding has shifted, blocking 50% of the egress pathway."
            )
            "Broken Equipment" -> VisionMockDetails(
                "LOW", 0.88, "Log ticket for overnight technical maintenance.", "Facility Techs",
                "Damaged digital ticket scanner reported", "Automatic ticket scanner #4 at Gate B is displaying a critical firmware boot error."
            )
            "Damaged Seating" -> VisionMockDetails(
                "LOW", 0.91, "Tag seat as unusable. Route replacement booking.", "Arena Services",
                "Structural bracket crack on seat Row 12, Seat 4", "The structural steel support for plastic seat Row 12 Seat 4 exhibits minor stress cracks."
            )
            "Long Queues" -> VisionMockDetails(
                "MEDIUM", 0.94, "Open secondary concession register and divert flow.", "Concessions Core",
                "Critical Queue Bottleneck at Sector 114", "Pre-match hotdog concession queues have extended past 25 meters, obstructing hallway circulation."
            )
            "Medical Incident" -> VisionMockDetails(
                "CRITICAL", 0.98, "Dispatch emergency first responders with oxygen/AED kit immediately.", "First Aid Squad",
                "Heat exhaustion incident reported near Row A", "An elderly fan is showing signs of acute heat exhaustion and dehydration."
            )
            else -> VisionMockDetails(
                "HIGH", 0.95, "Initiate safety cordon and dispatch inspector.", "Security Command",
                "Suspicious unattended backpack on concourse floor", "A black heavy-duty nylon backpack left unattended beneath seat Row M 18."
            )
        }

        // Auto-report incident!
        val severity = when (details.priority) {
            "LOW" -> Severity.LOW
            "MEDIUM" -> Severity.MEDIUM
            else -> Severity.CRITICAL
        }
        com.example.data.DatabaseService.addIncident(details.reportTitle, details.reportDesc, location, severity)

        return VisionAnalysisResult(
            category = photoCategory,
            priority = details.priority,
            confidence = details.confidence,
            action = details.action,
            suggestedTeam = details.team,
            location = location
        )
    }

    // --- 12. Centralized Simulation Loop ---
    private var demoJob: Job? = null

    fun startDemoSimulation() {
        demoModeEnabled.value = true
        demoJob?.cancel()
        demoJob = CoroutineScope(Dispatchers.Main).launch {
            var counter = 0
            while (demoModeEnabled.value) {
                delay(6000)
                counter++

                // 1. Cycle Match Phases to show how the system completely adapts!
                val phaseList = MatchPhase.values()
                val nextPhaseIndex = (currentMatchPhase.value.ordinal + 1) % phaseList.size
                currentMatchPhase.value = phaseList[nextPhaseIndex]

                // 2. Flucluate staff allocations & wait times
                gateAVolunteers.value = (1..6).random()
                gateBVolunteers.value = (1..6).random()
                gateCVolunteers.value = (1..6).random()

                // 3. Trigger dynamic proactive alerts based on phase
                when (currentMatchPhase.value) {
                    MatchPhase.GATES_OPEN -> {
                        addProactiveAlert(
                            "Ticket Scanners Optimized",
                            "All Gate A scanners are operating under optimal neural processing. Flow rate looks stable.",
                            "INFO"
                        )
                    }
                    MatchPhase.PRE_MATCH -> {
                        addProactiveAlert(
                            "Gate B High Influx Peak",
                            "Crowd densities hitting 94% near south plazas. Security channels fully open.",
                            "WARNING"
                        )
                    }
                    MatchPhase.KICKOFF -> {
                        addProactiveAlert(
                            "Kickoff Green Transition",
                            "Concourses are fully clear. Re-routing excess sanitation stewards to restock recycling centers.",
                            "INFO"
                        )
                    }
                    MatchPhase.HALFTIME -> {
                        addProactiveAlert(
                            "Extreme Concession Congestion",
                            "Diverting hot dog fans from Section 114 (15m wait) to Sector 102 (under 4m wait).",
                            "WARNING"
                        )
                    }
                    MatchPhase.SECOND_HALF -> {
                        addProactiveAlert(
                            "Transit Express Pre-warmup",
                            "Engines pre-heating for Meadowlands Express line to secure instantaneous post-match egress.",
                            "INFO"
                        )
                    }
                    MatchPhase.EMERGENCY -> {
                        addProactiveAlert(
                            "TACTICAL EVACUATION DISPATCHED",
                            "Clear Gate C corridors instantly. Direct safety teams to emergency exit stairs 4-A.",
                            "CRITICAL"
                        )
                    }
                    MatchPhase.POST_MATCH -> {
                        addProactiveAlert(
                            "Meadowlands Express Departure Max",
                            "Trains departing every 5 minutes. Direct post-game crowds via East gate to minimize walking strain.",
                            "INFO"
                        )
                    }
                }

                // 4. Inject automated AI notifications into user chat
                addChatMessage(
                    ChatMessage(
                        isUser = false,
                        text = "🏟️ **Demo Operating System update:** Stadium is now in **${currentMatchPhase.value.displayName}** phase. ${currentMatchPhase.value.desc} Dynamic wait times adjusted based on optimized volunteer routing.",
                        timestamp = "Live"
                    )
                )
            }
        }
    }

    fun stopDemoSimulation() {
        demoModeEnabled.value = false
        demoJob?.cancel()
        demoJob = null
    }

    // --- 13. Centralized AI Brain Prompt Builder ---
    fun getGlobalContextPrompt(): String {
        val phase = currentMatchPhase.value
        val role = currentUserRole.value
        val alerts = _proactiveAlerts.value.take(2).joinToString("; ") { "[${it.type}] ${it.title}: ${it.message}" }
        val unresolvedIncidents = _incidents.value.filter { it.status != IncidentStatus.RESOLVED }
        val incText = unresolvedIncidents.joinToString("; ") { "${it.title} at ${it.location} (${it.severity})" }

        // Get dynamic live match data
        val match = MatchService.liveMatchState.value
        val matchText = if (match != null) {
            """
            - Live Match: ${match.homeTeam} vs ${match.awayTeam} (${match.competition})
            - Current Score: ${match.homeTeamCode} ${match.homeScore} - ${match.awayScore} ${match.awayTeamCode}
            - Match Time: ${match.currentMinute} (${match.matchStatus})
            - Goalscorers: ${match.goalscorers.joinToString(", ")}
            - Bookings: ${match.cards.joinToString(", ")}
            - Subs: ${match.substitutions.joinToString(", ")}
            - Match Stats: Possession ${match.homeTeamCode} ${match.possessionHome}% - ${match.possessionAway}% ${match.awayTeamCode}, Shots Home ${match.shotsHome} vs Away ${match.shotsAway}, xG Home ${match.expectedGoalsHome} vs Away ${match.expectedGoalsAway}
            - Venue: ${match.venue}
            - Officials: ${match.officials}
            """.trimIndent()
        } else {
            "- Live Match Info: No active live matches currently available."
        }

        return """
            You are the MetLife Stadium AI Brain operating for the FIFA World Cup 2026.
            Your response must adapt to this central context:
            - Selected Language: ${currentLanguage.value.displayName} (Code: ${currentLanguage.value.code})
            - User Role: ${role.displayName}
            - Match Phase: ${phase.displayName} (${phase.desc})
            - Active Incidents: $incText
            - Proactive Alerts: $alerts
            - High Contrast: ${highContrastMode.value}
            
            REAL-TIME MATCH CONTEXT:
            $matchText
            
            Every decision or advice you give must balance:
            1. **Situation**: Acknowledge the current conditions.
            2. **Reasoning**: Explain why a suggestion is made using real numbers.
            3. **Recommendation**: Clear, actionable directions.
            4. **Expected Impact**: Show carbon/congestion benefits.
            5. **Confidence Score**: Out of 100%.
        """.trimIndent()
    }
}
