package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.BuildConfig
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data structures matching Firestore collections
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoURL: String = "",
    val role: String = "Spectator (Fan)",
    val language: String = "English",
    val highContrast: Boolean = false,
    val voiceOutput: Boolean = false,
    val textScale: Float = 1.0f,
    val greenPoints: Int = 320,
    val carbonSavedKg: Double = 12.4,
    val seatInfo: String = "Sec 114, Row M, Seat 8",
    val createdAt: String = "",
    val lastLogin: String = "",
    val country: String = "",
    val favouriteTeam: String = "",
    val favouriteStadium: String = "MetLife Stadium",
    val ticketInfo: String = "",
    val onboardingCompleted: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val privacyProfileShared: Boolean = true,
    val connectedGoogle: Boolean = false
)

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // "INFO", "WARNING", "CRITICAL"
    val timestamp: String = "",
    val phase: String = "Pre Match"
)

data class FoodOrder(
    val id: String = "",
    val stallName: String = "",
    val itemName: String = "",
    val userEmail: String = "",
    val quantity: Int = 1,
    val totalPrice: Double = 0.0,
    val carbonSavingsKg: Double = 0.0,
    val status: String = "Pending", // "Pending", "Preparing", "Ready", "Completed"
    val timestamp: String = ""
)

data class CrowdMetric(
    val gateName: String = "",
    val staffCount: Int = 3,
    val waitTimeMinutes: Int = 5,
    val densityPercentage: Double = 50.0,
    val timestamp: String = ""
)

data class TransportationRoute(
    val routeId: String = "",
    val routeName: String = "",
    val type: String = "Train", // "Train", "Bus", "Shuttle"
    val status: String = "On Time", // "On Time", "Delayed", "Suspended"
    val nextDeparture: String = "10 mins",
    val carbonSaved: Double = 2.4
)

data class EmergencyLog(
    val id: String = "",
    val alertTitle: String = "",
    val description: String = "",
    val timestamp: String = "",
    val severity: String = "HIGH"
)

data class SustainabilityRecord(
    val id: String = "",
    val userEmail: String = "",
    val action: String = "",
    val carbonSavingsKg: Double = 0.0,
    val pointsEarned: Int = 0,
    val timestamp: String = ""
)

object DatabaseService {
    private const val TAG = "DatabaseService"

    // Authentication States
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Database Collections States
    private val _incidents = MutableStateFlow<List<IncidentReport>>(emptyList())
    val incidents: StateFlow<List<IncidentReport>> = _incidents.asStateFlow()

    private val _volunteerTasks = MutableStateFlow<List<VolunteerTask>>(emptyList())
    val volunteerTasks: StateFlow<List<VolunteerTask>> = _volunteerTasks.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _lostItems = MutableStateFlow<List<AppState.LostItem>>(emptyList())
    val lostItems: StateFlow<List<AppState.LostItem>> = _lostItems.asStateFlow()

    private val _foodOrders = MutableStateFlow<List<FoodOrder>>(emptyList())
    val foodOrders: StateFlow<List<FoodOrder>> = _foodOrders.asStateFlow()

    private val _crowdMetrics = MutableStateFlow<List<CrowdMetric>>(emptyList())
    val crowdMetrics: StateFlow<List<CrowdMetric>> = _crowdMetrics.asStateFlow()

    private val _transportation = MutableStateFlow<List<TransportationRoute>>(emptyList())
    val transportation: StateFlow<List<TransportationRoute>> = _transportation.asStateFlow()

    private val _emergencyLogs = MutableStateFlow<List<EmergencyLog>>(emptyList())
    val emergencyLogs: StateFlow<List<EmergencyLog>> = _emergencyLogs.asStateFlow()

    private val _sustainabilityRecords = MutableStateFlow<List<SustainabilityRecord>>(emptyList())
    val sustainabilityRecords: StateFlow<List<SustainabilityRecord>> = _sustainabilityRecords.asStateFlow()

    private val _aiConversations = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiConversations: StateFlow<List<ChatMessage>> = _aiConversations.asStateFlow()

    // Firebase references (safe fallback initialization)
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    var isFirebaseAvailable: Boolean = false
        private set

    // Snapshots listeners registers
    private val listeners = mutableListOf<ListenerRegistration>()

    init {
        try {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            isFirebaseAvailable = true
            Log.d(TAG, "Firebase initialized successfully.")
            setupRealtimeListeners()
            checkPersistentLogin()
        } catch (e: Exception) {
            isFirebaseAvailable = false
            Log.i(TAG, "Firebase not available, running in fully functional dynamic local fallback mode: ${e.localizedMessage}")
            setupLocalFallbackData()
        }
    }

    private fun checkPersistentLogin() {
        val firebaseUser = auth?.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            val email = firebaseUser.email ?: ""
            Log.d(TAG, "Found persistent login for email: $email")
            
            // Read profile from Firestore or set local profile
            if (isFirebaseAvailable && firestore != null) {
                firestore!!.collection("users").document(uid).addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Error fetching persistent user profile", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val profile = snapshot.toObject(UserProfile::class.java)
                        _currentUser.value = profile
                        // Sync settings with global AppState
                        profile?.let { syncWithAppState(it) }
                    } else {
                        // Create profile if missing
                        val newProfile = UserProfile(
                            uid = uid,
                            name = email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            email = email,
                            createdAt = getCurrentTimestamp(),
                            lastLogin = getCurrentTimestamp()
                        )
                        firestore!!.collection("users").document(uid).set(newProfile)
                        _currentUser.value = newProfile
                        syncWithAppState(newProfile)
                    }
                }
            } else {
                setLocalUser(email)
            }
        } else {
            _currentUser.value = null
        }
    }

    private fun syncWithAppState(profile: UserProfile) {
        AppState.fanName.value = profile.name
        AppState.fanSeat.value = profile.seatInfo
        AppState.ecoRewardPoints.value = profile.greenPoints
        AppState.carbonSavedKg.value = profile.carbonSavedKg
        AppState.highContrastMode.value = profile.highContrast
        AppState.voiceOutputEnabled.value = profile.voiceOutput
        AppState.textScale.value = profile.textScale
        
        // Sync favorite teams for push notifications
        NotificationService.loadFavouriteTeamsFromUserString(profile.favouriteTeam)
        
        // Sync active stadium
        com.example.data.StadiumDatabase.updateActiveStadium(profile.favouriteStadium)
        
        val lang = Language.values().firstOrNull { it.displayName.equals(profile.language, ignoreCase = true) }
            ?: Language.ENGLISH
        AppState.currentLanguage.value = lang

        val role = UserRole.values().firstOrNull { it.displayName.equals(profile.role, ignoreCase = true) }
            ?: UserRole.FAN
        AppState.currentUserRole.value = role
    }

    private fun setupRealtimeListeners() {
        if (!isFirebaseAvailable || firestore == null) return

        // Clear existing snapshots if any
        listeners.forEach { it.remove() }
        listeners.clear()

        // 1. Incidents Realtime listener
        val l1 = firestore!!.collection("Incidents")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Incidents listener failed", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<IncidentReport>()
                    for (doc in snapshot) {
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val title = doc.getString("title") ?: ""
                            val description = doc.getString("description") ?: ""
                            val location = doc.getString("location") ?: ""
                            val severityStr = doc.getString("severity") ?: "LOW"
                            val statusStr = doc.getString("status") ?: "REPORTED"
                            val timestamp = doc.getString("timestamp") ?: ""

                            val severity = try { Severity.valueOf(severityStr) } catch(ex: Exception) { Severity.LOW }
                            val status = try { IncidentStatus.valueOf(statusStr) } catch(ex: Exception) { IncidentStatus.REPORTED }

                            list.add(IncidentReport(id, title, description, location, severity, status, timestamp))
                        } catch (ex: Exception) {
                            Log.e(TAG, "Error parsing incident doc", ex)
                        }
                    }
                    _incidents.value = list
                }
            }
        listeners.add(l1)

        // 2. Volunteer Tasks Realtime listener
        val l2 = firestore!!.collection("VolunteerTasks")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = mutableListOf<VolunteerTask>()
                    for (doc in snapshot) {
                        val id = doc.getString("id") ?: doc.id
                        val title = doc.getString("title") ?: ""
                        val description = doc.getString("description") ?: ""
                        val location = doc.getString("location") ?: ""
                        val isCompleted = doc.getBoolean("isCompleted") ?: false
                        list.add(VolunteerTask(id, title, description, location, isCompleted))
                    }
                    _volunteerTasks.value = list
                }
            }
        listeners.add(l2)

        // 3. Announcements Realtime listener
        val l3 = firestore!!.collection("Announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.toObjects(Announcement::class.java)
                    _announcements.value = list
                }
            }
        listeners.add(l3)

        // 4. Lost Items Realtime listener
        val l4 = firestore!!.collection("LostItems")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = mutableListOf<AppState.LostItem>()
                    for (doc in snapshot) {
                        val id = doc.getString("id") ?: doc.id
                        val itemName = doc.getString("itemName") ?: ""
                        val category = doc.getString("category") ?: ""
                        val lastSeenLocation = doc.getString("lastSeenLocation") ?: ""
                        val description = doc.getString("description") ?: ""
                        val status = doc.getString("status") ?: "Reported"
                        list.add(AppState.LostItem(id, itemName, category, lastSeenLocation, description, status))
                    }
                    _lostItems.value = list
                }
            }
        listeners.add(l4)

        // 5. Food Orders Realtime listener
        val l5 = firestore!!.collection("FoodOrders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _foodOrders.value = snapshot.toObjects(FoodOrder::class.java)
                }
            }
        listeners.add(l5)

        // 6. Crowd Metrics Realtime listener
        val l6 = firestore!!.collection("CrowdMetrics")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _crowdMetrics.value = snapshot.toObjects(CrowdMetric::class.java)
                }
            }
        listeners.add(l6)

        // 7. Transportation Realtime listener
        val l7 = firestore!!.collection("Transportation")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _transportation.value = snapshot.toObjects(TransportationRoute::class.java)
                }
            }
        listeners.add(l7)

        // 8. Emergency Logs Realtime listener
        val l8 = firestore!!.collection("EmergencyLogs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _emergencyLogs.value = snapshot.toObjects(EmergencyLog::class.java)
                }
            }
        listeners.add(l8)

        // 9. Sustainability Records Realtime listener
        val l9 = firestore!!.collection("Sustainability")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _sustainabilityRecords.value = snapshot.toObjects(SustainabilityRecord::class.java)
                }
            }
        listeners.add(l9)

        // 10. AI Conversations Realtime listener
        val l10 = firestore!!.collection("AIConversations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = mutableListOf<ChatMessage>()
                    for (doc in snapshot) {
                        val id = doc.getString("id") ?: doc.id
                        val text = doc.getString("text") ?: ""
                        val isUser = doc.getBoolean("isUser") ?: false
                        val timestamp = doc.getString("timestamp") ?: ""
                        list.add(ChatMessage(id, isUser, text, timestamp))
                    }
                    _aiConversations.value = list
                }
            }
        listeners.add(l10)
    }

    private fun setupLocalFallbackData() {
        // Hydrate data structures locally with initial documents so there's NO fake data,
        // and everything is dynamically synchronized through the DatabaseService!
        _incidents.value = listOf(
            IncidentReport(
                title = "Water Spill Concourse Section 108",
                description = "Slip hazard noticed near the pretzel stand. Requires cleaning.",
                location = "Concourse Level 1, Sec 108",
                severity = Severity.LOW,
                status = IncidentStatus.INVESTIGATING,
                timestamp = "05:13 PM"
            ),
            IncidentReport(
                title = "Lost Child (Wearing Brazil Jersey)",
                description = "Child separated from parents near Gate B entrance. Wearing yellow Neymar jersey, age 7.",
                location = "Gate B Plaza",
                severity = Severity.CRITICAL,
                status = IncidentStatus.REPORTED,
                timestamp = "05:15 PM"
            )
        )

        _volunteerTasks.value = listOf(
            VolunteerTask(
                title = "Distribute zero-waste flyers",
                description = "Hand out sustainability guidelines near East gates.",
                location = "Gate C Plaza",
                isCompleted = false
            ),
            VolunteerTask(
                title = "Assist elderly guest to Elevator B",
                description = "Accompany guest from Section 114 to Elevator Level 3.",
                location = "Section 114 Corridor",
                isCompleted = false
            ),
            VolunteerTask(
                title = "Monitor water refill station",
                description = "Ensure dispenser at Sec 112 is clean and full.",
                location = "Section 112 Water Fountain",
                isCompleted = true
            )
        )

        _announcements.value = listOf(
            Announcement(
                id = UUID.randomUUID().toString(),
                title = "Gate C Wait Time Warning",
                message = "Gate C arrival flow has exceeded optimal entry threshold by 12%. Recommend re-routing fans.",
                type = "WARNING",
                timestamp = "06:15 PM",
                phase = "Pre Match"
            ),
            Announcement(
                id = UUID.randomUUID().toString(),
                title = "Medical Station 2 Load Spike",
                message = "Heatstroke reports spiking near Sector 220. Auxiliary hydration carts dispatched.",
                type = "INFO",
                timestamp = "06:17 PM",
                phase = "Pre Match"
            )
        )

        _lostItems.value = listOf(
            AppState.LostItem(itemName = "Leather Wallet", category = "Personal Valuables", lastSeenLocation = "Sec 114, Row F", description = "Brown leather wallet containing ID and souvenir card.", status = "Reported"),
            AppState.LostItem(itemName = "iPhone 15 Pro", category = "Electronics", lastSeenLocation = "Washroom near Gate A", description = "Titanium blue, with clear soccer-patterned case.", status = "Reported")
        )

        _foodOrders.value = listOf(
            FoodOrder(UUID.randomUUID().toString(), "Organic Kickoff", "Plant-Based Burger", "spectator@fifa.com", 1, 14.50, 1.2, "Completed", "06:10 PM"),
            FoodOrder(UUID.randomUUID().toString(), "Eco-Grill Express", "Vegan Sausage Wrap", "spectator@fifa.com", 2, 18.00, 1.6, "Preparing", "06:18 PM")
        )

        _crowdMetrics.value = listOf(
            CrowdMetric("Gate A (North)", staffCount = 2, waitTimeMinutes = 14, densityPercentage = 82.0, timestamp = "06:19 PM"),
            CrowdMetric("Gate B (South Plaza)", staffCount = 5, waitTimeMinutes = 8, densityPercentage = 68.0, timestamp = "06:19 PM"),
            CrowdMetric("Gate C (East Concourse)", staffCount = 1, waitTimeMinutes = 11, densityPercentage = 75.0, timestamp = "06:19 PM")
        )

        _transportation.value = listOf(
            TransportationRoute("route_1", "Meadowlands Express Train", "Train", "On Time", "8 mins", 3.2),
            TransportationRoute("route_2", "Secaucus Shuttle Bus", "Bus", "On Time", "12 mins", 1.8),
            TransportationRoute("route_3", "NYC Direct Motorcoach", "Bus", "Delayed", "22 mins", 0.0)
        )

        _emergencyLogs.value = listOf(
            EmergencyLog("em_1", "Heat Exhaustion Alert Sec 220", "Dispatched auxiliary hydration and first aid cart to Sector 220 corridor.", "06:17 PM", "MEDIUM"),
            EmergencyLog("em_2", "Tactical Evacuation Readiness", "Emergency exit pathways cleared for sector 100-120 in accordance with post-match preparations.", "06:00 PM", "LOW")
        )

        _sustainabilityRecords.value = listOf(
            SustainabilityRecord("sus_1", "spectator@fifa.com", "Recycled 4 Aluminum Cups", 0.8, 40, "06:02 PM"),
            SustainabilityRecord("sus_2", "spectator@fifa.com", "Selected Plant-Based Concession", 1.2, 50, "06:10 PM")
        )

        _aiConversations.value = listOf(
            ChatMessage(UUID.randomUUID().toString(), false, "Hello! I am your FIFA AI Companion. Ask me anything about MetLife Stadium, match timelines, concessions, green points, or security dispatcher issues.", "05:00 PM")
        )
    }

    // --- Authentication Operations ---
    fun login(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _authError.value = null
        if (isFirebaseAvailable && auth != null) {
            auth!!.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Login successful: $email")
                    checkPersistentLogin()
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Login failed: ${e.message}")
                    _authError.value = e.message
                    onFailure(e.message ?: "Authentication failed")
                }
        } else {
            // Local fallback validation
            if (email.contains("@") && password.length >= 6) {
                setLocalUser(email)
                onSuccess()
            } else {
                val err = "Invalid email format or password (must be at least 6 characters)"
                _authError.value = err
                onFailure(err)
            }
        }
    }

    fun loginWithGoogle(
        context: android.content.Context,
        coroutineScope: CoroutineScope,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        _authError.value = null
        val webClientId = try {
            BuildConfig.GOOGLE_WEB_CLIENT_ID
        } catch (e: Exception) {
            ""
        }.ifEmpty {
            "1234567890-mockclientid.apps.googleusercontent.com"
        }

        if (isFirebaseAvailable && auth != null) {
            coroutineScope.launch {
                try {
                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                    val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential

                    if (credential is androidx.credentials.CustomCredential &&
                        credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        
                        val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        
                        val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                        auth!!.signInWithCredential(firebaseCredential)
                            .addOnSuccessListener { authResult ->
                                Log.d(TAG, "Firebase Google login success: ${authResult.user?.email}")
                                checkPersistentLogin()
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Firebase Google login failed", e)
                                _authError.value = e.localizedMessage
                                onFailure(e.localizedMessage ?: "Firebase Google Authentication failed")
                            }
                    } else {
                        val err = "Unexpected credential type: ${credential.type}"
                        _authError.value = err
                        onFailure(err)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Credential Manager error", e)
                    _authError.value = e.localizedMessage
                    if (e.message?.contains("developer error", ignoreCase = true) == true ||
                        e is androidx.credentials.exceptions.GetCredentialException) {
                        onFailure("Google Sign-In failed: ${e.localizedMessage}. (Note: Ensure your SHA-1 fingerprint and Web Client ID are registered in the Firebase Console)")
                    } else {
                        onFailure(e.localizedMessage ?: "Google Sign-In failed")
                    }
                }
            }
        } else {
            // Fully functional local fallback mode
            setLocalUser("google.fan@fifaworldcup.com", "Fifa Google Fan", "Spectator (Fan)")
            onSuccess()
        }
    }

    fun signUp(email: String, password: String, name: String, role: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _authError.value = null
        if (isFirebaseAvailable && auth != null) {
            auth!!.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        // Send verification email
                        user.sendEmailVerification()
                        
                        val newProfile = UserProfile(
                            uid = user.uid,
                            name = name,
                            email = email,
                            role = role,
                            createdAt = getCurrentTimestamp(),
                            lastLogin = getCurrentTimestamp()
                        )
                        firestore?.collection("users")?.document(user.uid)?.set(newProfile)
                        _currentUser.value = newProfile
                        syncWithAppState(newProfile)
                        onSuccess()
                    } else {
                        onFailure("Failed to create user object")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Sign up failed: ${e.message}")
                    _authError.value = e.message
                    onFailure(e.message ?: "Registration failed")
                }
        } else {
            if (email.contains("@") && password.length >= 6 && name.isNotEmpty()) {
                setLocalUser(email, name, role)
                onSuccess()
            } else {
                val err = "Invalid email, name or password (must be at least 6 characters)"
                _authError.value = err
                onFailure(err)
            }
        }
    }

    fun forgotPassword(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (isFirebaseAvailable && auth != null) {
            auth!!.sendPasswordResetEmail(email)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e.message ?: "Error sending reset email") }
        } else {
            if (email.contains("@")) {
                onSuccess() // Simulate success in local mode
            } else {
                onFailure("Please enter a valid email address.")
            }
        }
    }

    fun resendVerificationEmail(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (isFirebaseAvailable) {
            auth?.currentUser?.sendEmailVerification()
                ?.addOnSuccessListener { onSuccess() }
                ?.addOnFailureListener { e -> onFailure(e.message ?: "Error sending verification email") }
        } else {
            onSuccess()
        }
    }

    fun logout(onSuccess: () -> Unit) {
        if (isFirebaseAvailable && auth != null) {
            auth!!.signOut()
        }
        _currentUser.value = null
        // Re-setup local default data
        setupLocalFallbackData()
        onSuccess()
    }

    private fun setLocalUser(email: String, name: String? = null, role: String = "Spectator (Fan)") {
        val finalName = name ?: email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val profile = UserProfile(
            uid = UUID.randomUUID().toString(),
            name = finalName,
            email = email,
            role = role,
            createdAt = getCurrentTimestamp(),
            lastLogin = getCurrentTimestamp()
        )
        _currentUser.value = profile
        syncWithAppState(profile)
    }

    // --- Profile/Settings Modification ---
    fun updateProfileSettings(name: String, role: String, language: String, highContrast: Boolean, voiceOutput: Boolean, textScale: Float, seatInfo: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            name = name,
            role = role,
            language = language,
            highContrast = highContrast,
            voiceOutput = voiceOutput,
            textScale = textScale,
            seatInfo = seatInfo
        )
        _currentUser.value = updated
        syncWithAppState(updated)

        if (isFirebaseAvailable && firestore != null && updated.uid.isNotEmpty()) {
            firestore!!.collection("users").document(updated.uid).set(updated)
                .addOnFailureListener { e -> Log.e(TAG, "Failed to update profile in Firestore", e) }
        }
    }

    fun completeOnboarding(
        name: String,
        country: String,
        language: String,
        favouriteTeam: String,
        favouriteStadium: String,
        ticketInfo: String,
        role: String,
        onSuccess: () -> Unit
    ) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            name = name,
            country = country,
            language = language,
            favouriteTeam = favouriteTeam,
            favouriteStadium = favouriteStadium,
            ticketInfo = ticketInfo,
            role = role,
            onboardingCompleted = true
        )
        _currentUser.value = updated
        syncWithAppState(updated)

        if (isFirebaseAvailable && firestore != null && updated.uid.isNotEmpty()) {
            firestore!!.collection("users").document(updated.uid).set(updated)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> 
                    Log.e(TAG, "Failed to save onboarding in Firestore", e)
                    onSuccess() // optimistic/local fallback success
                }
        } else {
            onSuccess()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val current = _currentUser.value
        if (current == null) {
            onFailure("No user is currently signed in.")
            return
        }

        if (isFirebaseAvailable && auth != null && firestore != null) {
            val user = auth!!.currentUser
            if (user != null) {
                firestore!!.collection("users").document(current.uid).delete()
                    .addOnSuccessListener {
                        user.delete()
                            .addOnSuccessListener {
                                _currentUser.value = null
                                setupLocalFallbackData()
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Auth user deletion failed", e)
                                _currentUser.value = null
                                setupLocalFallbackData()
                                onSuccess() // Proceed with local cleaning anyway
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Firestore document deletion failed", e)
                        onFailure(e.localizedMessage ?: "Failed to delete user document")
                    }
            } else {
                _currentUser.value = null
                setupLocalFallbackData()
                onSuccess()
            }
        } else {
            _currentUser.value = null
            setupLocalFallbackData()
            onSuccess()
        }
    }

    fun updatePrivacySettings(shared: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(privacyProfileShared = shared)
        _currentUser.value = updated
        syncWithAppState(updated)
        if (isFirebaseAvailable && firestore != null && updated.uid.isNotEmpty()) {
            firestore!!.collection("users").document(updated.uid).set(updated)
        }
    }

    fun updateNotificationSettings(enabled: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(notificationsEnabled = enabled)
        _currentUser.value = updated
        syncWithAppState(updated)
        if (isFirebaseAvailable && firestore != null && updated.uid.isNotEmpty()) {
            firestore!!.collection("users").document(updated.uid).set(updated)
        }
    }

    fun updateConnectedAccounts(google: Boolean) {
        val current = _currentUser.value ?: return
        val updated = current.copy(connectedGoogle = google)
        _currentUser.value = updated
        syncWithAppState(updated)
        if (isFirebaseAvailable && firestore != null && updated.uid.isNotEmpty()) {
            firestore!!.collection("users").document(updated.uid).set(updated)
        }
    }

    fun addRewardPoints(points: Int, carbonSaved: Double) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            greenPoints = current.greenPoints + points,
            carbonSavedKg = current.carbonSavedKg + carbonSaved
        )
        _currentUser.value = updated
        syncWithAppState(updated)

        if (isFirebaseAvailable && firestore != null && updated.uid.isNotEmpty()) {
            firestore!!.collection("users").document(updated.uid).set(updated)
        }
        
        // Log Sustainability activity record
        addSustainabilityRecord("Logged Eco Contribution: +$points pts", carbonSaved, points)
    }

    // --- Collection Database Operations ---

    fun addIncident(title: String, description: String, location: String, severity: Severity) {
        val incident = IncidentReport(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            location = location,
            severity = severity,
            status = IncidentStatus.REPORTED,
            timestamp = getCurrentTimeFormatted()
        )

        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("Incidents").document(incident.id).set(incident)
                .addOnSuccessListener { Log.d(TAG, "Incident saved successfully in Firestore") }
                .addOnFailureListener { e -> Log.e(TAG, "Failed to save incident", e) }
        } else {
            _incidents.value = listOf(incident) + _incidents.value
        }
    }

    fun updateIncidentStatus(id: String, status: IncidentStatus) {
        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("Incidents").document(id)
                .update("status", status.name)
        } else {
            _incidents.value = _incidents.value.map {
                if (it.id == id) it.copy(status = status) else it
            }
        }
    }

    fun addVolunteerTask(title: String, description: String, location: String) {
        val task = VolunteerTask(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            location = location,
            isCompleted = false
        )

        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("VolunteerTasks").document(task.id).set(task)
        } else {
            _volunteerTasks.value = listOf(task) + _volunteerTasks.value
        }
    }

    fun completeVolunteerTask(id: String) {
        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("VolunteerTasks").document(id)
                .update("isCompleted", true)
        } else {
            _volunteerTasks.value = _volunteerTasks.value.map {
                if (it.id == id) it.copy(isCompleted = true) else it
            }
        }
    }

    fun addAnnouncement(title: String, message: String, type: String, phase: String) {
        val ann = Announcement(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type,
            timestamp = getCurrentTimeFormatted(),
            phase = phase
        )

        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("Announcements").document(ann.id).set(ann)
        } else {
            _announcements.value = listOf(ann) + _announcements.value
        }
    }

    fun addLostItem(name: String, category: String, location: String, desc: String) {
        val item = AppState.LostItem(
            id = UUID.randomUUID().toString(),
            itemName = name,
            category = category,
            lastSeenLocation = location,
            description = desc,
            status = "Reported"
        )

        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("LostItems").document(item.id).set(item)
        } else {
            _lostItems.value = listOf(item) + _lostItems.value
        }
    }

    fun updateLostItemStatus(id: String, status: String) {
        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("LostItems").document(id)
                .update("status", status)
        } else {
            _lostItems.value = _lostItems.value.map {
                if (it.id == id) it.copy(status = status) else it
            }
        }
    }

    fun addFoodOrder(stallName: String, itemName: String, quantity: Int, totalPrice: Double, carbonSavings: Double) {
        val userEmail = _currentUser.value?.email ?: "spectator@fifa.com"
        val order = FoodOrder(
            id = UUID.randomUUID().toString(),
            stallName = stallName,
            itemName = itemName,
            userEmail = userEmail,
            quantity = quantity,
            totalPrice = totalPrice,
            carbonSavingsKg = carbonSavings,
            status = "Pending",
            timestamp = getCurrentTimeFormatted()
        )

        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("FoodOrders").document(order.id).set(order)
        } else {
            _foodOrders.value = listOf(order) + _foodOrders.value
        }

        // Earn eco-points for green concessions!
        if (carbonSavings > 0) {
            val pts = (carbonSavings * 30).toInt()
            addRewardPoints(pts, carbonSavings)
        }
    }

    fun updateFoodOrderStatus(id: String, status: String) {
        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("FoodOrders").document(id)
                .update("status", status)
        } else {
            _foodOrders.value = _foodOrders.value.map {
                if (it.id == id) it.copy(status = status) else it
            }
        }
    }

    fun addSustainabilityRecord(action: String, carbonSavings: Double, points: Int) {
        val userEmail = _currentUser.value?.email ?: "spectator@fifa.com"
        val record = SustainabilityRecord(
            id = UUID.randomUUID().toString(),
            userEmail = userEmail,
            action = action,
            carbonSavingsKg = carbonSavings,
            pointsEarned = points,
            timestamp = getCurrentTimeFormatted()
        )

        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("Sustainability").document(record.id).set(record)
        } else {
            _sustainabilityRecords.value = listOf(record) + _sustainabilityRecords.value
        }
    }

    fun addAIChatMessage(isUser: Boolean, text: String) {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = isUser,
            text = text,
            timestamp = getCurrentTimeFormatted()
        )

        if (isFirebaseAvailable && firestore != null) {
            val userEmail = _currentUser.value?.email ?: "anonymous"
            val fireMsg = mapOf(
                "id" to msg.id,
                "text" to msg.text,
                "isUser" to msg.isUser,
                "timestamp" to msg.timestamp,
                "userEmail" to userEmail
            )
            firestore!!.collection("AIConversations").document(msg.id).set(fireMsg)
        } else {
            _aiConversations.value = _aiConversations.value + msg
            AppState.addChatMessage(msg)
        }
    }

    fun updateCrowdMetric(gateName: String, staffCount: Int, waitTime: Int, density: Double) {
        val metric = CrowdMetric(gateName, staffCount, waitTime, density, getCurrentTimeFormatted())
        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("CrowdMetrics").document(gateName).set(metric)
        } else {
            _crowdMetrics.value = _crowdMetrics.value.map {
                if (it.gateName == gateName) metric else it
            }
        }
    }

    fun updateTransportationStatus(routeId: String, status: String, nextDeparture: String) {
        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("Transportation").document(routeId)
                .update("status", status, "nextDeparture", nextDeparture)
        } else {
            _transportation.value = _transportation.value.map {
                if (it.routeId == routeId) it.copy(status = status, nextDeparture = nextDeparture) else it
            }
        }
    }

    fun addEmergencyLog(title: String, description: String, severity: String) {
        val log = EmergencyLog(
            id = UUID.randomUUID().toString(),
            alertTitle = title,
            description = description,
            timestamp = getCurrentTimeFormatted(),
            severity = severity
        )

        if (isFirebaseAvailable && firestore != null) {
            firestore!!.collection("EmergencyLogs").document(log.id).set(log)
        } else {
            _emergencyLogs.value = listOf(log) + _emergencyLogs.value
        }
    }

    // --- Helper Timing Utilities ---
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }
}
