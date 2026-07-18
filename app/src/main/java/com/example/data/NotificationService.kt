package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class MatchNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: String,
    val type: String, // "GOAL", "CARD", "SUB", "STATUS", "EMERGENCY"
    val teamCode: String,
    val matchTitle: String,
    val isRead: Boolean = false
)

object NotificationService {
    private const val TAG = "NotificationService"
    private const val CHANNEL_ID = "world_cup_match_alerts"
    private const val CHANNEL_NAME = "FIFA World Cup Alerts"
    private const val CHANNEL_DESC = "Notifications for goals, cards, and match events"

    // Application context fallback for system notifications
    var appContext: Context? = null

    // Set of favorite teams (e.g., "USA", "MEX", "ARG")
    private val _favouriteTeams = MutableStateFlow<Set<String>>(setOf("USA", "ARG"))
    val favouriteTeams: StateFlow<Set<String>> = _favouriteTeams.asStateFlow()

    // Notification toggles
    val alertOnGoals = MutableStateFlow(true)
    val alertOnCards = MutableStateFlow(true)
    val alertOnSubs = MutableStateFlow(false)
    val alertOnMatchStatus = MutableStateFlow(true)

    // Log of all received notifications
    private val _notificationsLog = MutableStateFlow<List<MatchNotification>>(emptyList())
    val notificationsLog: StateFlow<List<MatchNotification>> = _notificationsLog.asStateFlow()

    // Active in-app notification banner
    private val _activeInAppNotification = MutableStateFlow<MatchNotification?>(null)
    val activeInAppNotification: StateFlow<MatchNotification?> = _activeInAppNotification.asStateFlow()

    init {
        // Initialize with a default welcome notification
        _notificationsLog.value = listOf(
            MatchNotification(
                title = "Notification System Online",
                message = "You will receive real-time push alerts when goals or events occur for your favorite teams!",
                timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                type = "STATUS",
                teamCode = "ALL",
                matchTitle = "Welcome to World Cup 2026"
            )
        )
    }

    fun addFavouriteTeam(teamCode: String) {
        val updated = _favouriteTeams.value.toMutableSet()
        updated.add(teamCode.uppercase())
        _favouriteTeams.value = updated
        
        // Update user profile representation as well
        syncWithUserProfile()
    }

    fun removeFavouriteTeam(teamCode: String) {
        val updated = _favouriteTeams.value.toMutableSet()
        updated.remove(teamCode.uppercase())
        _favouriteTeams.value = updated
        
        // Update user profile representation as well
        syncWithUserProfile()
    }

    fun isTeamFavourite(teamCode: String): Boolean {
        return _favouriteTeams.value.contains(teamCode.uppercase())
    }

    fun clearFavouriteTeams() {
        _favouriteTeams.value = emptySet()
        syncWithUserProfile()
    }

    private fun syncWithUserProfile() {
        val commaSeparated = _favouriteTeams.value.joinToString(", ")
        val currentUser = DatabaseService.currentUser.value
        if (currentUser != null) {
            DatabaseService.updateProfileSettings(
                name = currentUser.name,
                role = currentUser.role,
                language = currentUser.language,
                highContrast = currentUser.highContrast,
                voiceOutput = currentUser.voiceOutput,
                textScale = currentUser.textScale,
                seatInfo = currentUser.seatInfo
            )
            // Save favorite team in DatabaseService extend
            DatabaseService.completeOnboarding(
                name = currentUser.name,
                country = currentUser.country,
                language = currentUser.language,
                favouriteTeam = commaSeparated,
                favouriteStadium = currentUser.favouriteStadium,
                ticketInfo = currentUser.seatInfo,
                role = currentUser.role,
                onSuccess = {}
            )
        }
    }

    fun loadFavouriteTeamsFromUserString(favTeamString: String) {
        if (favTeamString.isBlank()) return
        val teams = favTeamString.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        _favouriteTeams.value = teams
    }

    fun triggerNotification(
        context: Context?,
        title: String,
        message: String,
        type: String, // "GOAL", "CARD", "SUB", "STATUS", "EMERGENCY"
        teamCode: String,
        matchTitle: String
    ) {
        // Check if matching filter settings
        val isEnabled = when (type) {
            "GOAL" -> alertOnGoals.value
            "CARD" -> alertOnCards.value
            "SUB" -> alertOnSubs.value
            "STATUS" -> alertOnMatchStatus.value
            "EMERGENCY" -> true
            else -> true
        }

        if (!isEnabled) {
            Log.d(TAG, "Notification of type $type ignored due to user filter settings.")
            return
        }

        // Check if team is in favorite list (or if event affects ALL / is an emergency)
        val isFavorite = teamCode == "ALL" || type == "EMERGENCY" || isTeamFavourite(teamCode)
        if (!isFavorite) {
            Log.d(TAG, "Notification ignored: Team $teamCode is not in user's favorites ${_favouriteTeams.value}")
            return
        }

        val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val newNotification = MatchNotification(
            title = title,
            message = message,
            timestamp = timestamp,
            type = type,
            teamCode = teamCode,
            matchTitle = matchTitle
        )

        // Add to local historical logs
        _notificationsLog.value = listOf(newNotification) + _notificationsLog.value

        // Trigger in-app dynamic banner StateFlow
        _activeInAppNotification.value = newNotification

        // Trigger Android System Notification
        val ctx = context ?: appContext
        if (ctx != null) {
            try {
                val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // Create the NotificationChannel, but only on API 26+ because the NotificationChannel class is not in the Support Library
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val importance = if (type == "EMERGENCY" || type == "GOAL") {
                        NotificationManager.IMPORTANCE_HIGH
                    } else {
                        NotificationManager.IMPORTANCE_DEFAULT
                    }
                    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                        description = CHANNEL_DESC
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info) // Guaranteed to compile
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(if (type == "EMERGENCY" || type == "GOAL") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setCategory(if (type == "EMERGENCY") NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)

                // Push! Using unique ID to avoid overwriting existing alerts
                val notificationId = System.currentTimeMillis().toInt()
                notificationManager.notify(notificationId, builder.build())
                Log.d(TAG, "System Notification dispatched successfully. ID: $notificationId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push system notification: ", e)
            }
        }
    }

    fun dismissActiveInAppNotification() {
        _activeInAppNotification.value = null
    }

    fun markAllAsRead() {
        _notificationsLog.value = _notificationsLog.value.map { it.copy(isRead = true) }
    }

    fun clearLog() {
        _notificationsLog.value = emptyList()
    }
}
