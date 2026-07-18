package com.example.ui.screens

import android.speech.tts.TextToSpeech
import android.speech.SpeechRecognizer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppState
import com.example.data.ChatMessage
import com.example.data.GeminiRepository
import com.example.data.Language
import com.example.ui.components.BeautifulAiMessage
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textScale by AppState.textScale.collectAsState()
    val currentLanguage by AppState.currentLanguage.collectAsState()
    val highContrast by AppState.highContrastMode.collectAsState()
    val voiceEnabled by AppState.voiceOutputEnabled.collectAsState()
    val currentUser by com.example.data.DatabaseService.currentUser.collectAsState()

    val messages by com.example.data.DatabaseService.aiConversations.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isListeningMode by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // --- TTS Engine Integration ---
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    
    DisposableEffect(Unit) {
        val initializedTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("TTS", "TextToSpeech successfully initialized.")
            } else {
                Log.e("TTS", "TextToSpeech initialization failed.")
            }
        }
        tts = initializedTts
        
        onDispose {
            initializedTts.stop()
            initializedTts.shutdown()
        }
    }

    // --- Speech Recognizer & Voice-to-Text Integration ---
    var isRecordingSpeech by remember { mutableStateOf(false) }
    var speechRecognitionError by remember { mutableStateOf<String?>(null) }
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }
    var currentRmsDb by remember { mutableStateOf(0f) }

    LaunchedEffect(currentLanguage, tts) {
        val ttsEngine = tts ?: return@LaunchedEffect
        val locale = when (currentLanguage) {
            Language.SPANISH -> Locale("es", "ES")
            Language.FRENCH -> Locale.FRENCH
            Language.PORTUGUESE -> Locale("pt", "PT")
            Language.ARABIC -> Locale("ar")
            Language.HINDI -> Locale("hi", "IN")
            Language.JAPANESE -> Locale.JAPANESE
            Language.GERMAN -> Locale.GERMAN
            Language.ITALIAN -> Locale.ITALY
            else -> Locale.US
        }
        ttsEngine.language = locale
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    val systemPrompt = """
        You are the FIFA AI Stadium Companion, an elite concierge assistant operating at MetLife Stadium during the FIFA World Cup 2026.
        Your goal is to guide fans, organizers, and staff with ultimate precision, accessibility, and class.
        Speak in a clear, highly professional, warm tone.
        Always translate your entire response into the user's selected language: ${currentLanguage.displayName}.
        Current User Profile:
        - Fan Name: ${currentUser?.name ?: "David Beckham"}
        - Ticket Seat: ${currentUser?.seatInfo ?: "Section 114, Row M, Seat 8"}
        - Email: ${currentUser?.email ?: "spectator@fifa.com"}
        - Account Role: ${currentUser?.role ?: "Spectator"}
    """.trimIndent()

    val quickPrompts = listOf(
        "Nearest wheelchair elevator from Section 114?",
        "NJ Transit Express schedule & carbon savings?",
        "Where is sensory room 101?",
        "Recycling bonuses near Section 114?",
        "Translate standard safety directions into Spanish."
    )

    fun handleSend(queryText: String) {
        if (queryText.isBlank()) return
        
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isRecordingSpeech = false
        speechRecognitionError = null

        com.example.data.DatabaseService.addAIChatMessage(isUser = true, text = queryText)
        inputQuery = ""
        isThinking = true
        isListeningMode = false

        coroutineScope.launch {
            val aiResponseText = GeminiRepository.generateResponse(queryText, systemPrompt)
            com.example.data.DatabaseService.addAIChatMessage(isUser = false, text = aiResponseText)
            isThinking = false

            if (voiceEnabled) {
                val spokenText = com.example.ui.components.StructuredAiResponse.fromJson(aiResponseText)?.let {
                    "${it.title}. ${it.summary}. ${it.explanation}"
                } ?: aiResponseText.replace(Regex("[*#_]"), "")
                tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "gemini_tts")
            }
        }
    }

    // --- Speech Recognizer Lambdas & Helper Definitions ---
    val startSpeechRecognizer = {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (currentLanguage) {
                    Language.SPANISH -> "es-ES"
                    Language.FRENCH -> "fr-FR"
                    Language.PORTUGUESE -> "pt-PT"
                    Language.ARABIC -> "ar-SA"
                    Language.HINDI -> "hi-IN"
                    Language.JAPANESE -> "ja-JP"
                    Language.GERMAN -> "de-DE"
                    Language.ITALIAN -> "it-IT"
                    else -> "en-US"
                })
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isRecordingSpeech = true
                        isListeningMode = true
                        speechRecognitionError = null
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        currentRmsDb = rmsdB
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isRecordingSpeech = false
                    }
                    override fun onError(error: Int) {
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                            else -> "Speech recognition error ($error)"
                        }
                        speechRecognitionError = errorMsg
                        isRecordingSpeech = false
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val speechText = matches[0]
                            inputQuery = speechText
                            handleSend(speechText)
                        }
                        isRecordingSpeech = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            inputQuery = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            speechRecognizer?.destroy()
            speechRecognizer = recognizer
            try {
                recognizer.startListening(intent)
            } catch (e: Exception) {
                speechRecognitionError = "Launch failed: ${e.localizedMessage}"
                isRecordingSpeech = false
            }
        } else {
            speechRecognitionError = "Native speech engine is not active in this environment. Running hands-free speech simulator mode."
            isListeningMode = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                startSpeechRecognizer()
            } else {
                speechRecognitionError = "Microphone permission is required for real voice-to-text. Using hands-free simulator mode."
                isListeningMode = true
            }
        }
    )

    val toggleVoiceInput = {
        if (isListeningMode) {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListeningMode = false
            isRecordingSpeech = false
            speechRecognitionError = null
        } else {
            isListeningMode = true
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                startSpeechRecognizer()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val runOnDispose = DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun handleVisionAnalysis(category: String) {
        com.example.data.DatabaseService.addAIChatMessage(isUser = true, text = "📷 [Captured Photo: $category]")
        isThinking = true
        isListeningMode = false

        coroutineScope.launch {
            kotlinx.coroutines.delay(1500) // Realistic neural analysis delay
            val result = AppState.simulateVisionAnalysis(category)
            val aiResponseText = """
                📷 **FIFA AI Vision Intel Report:**
                
                • **Detected Target**: ${result.category}
                • **Neural Confidence**: ${(result.confidence * 100)}%
                • **Calculated Severity**: ${result.priority}
                • **Recommended Mitigation**: ${result.action}
                • **Suggested Responder**: ${result.suggestedTeam}
                
                *Incident successfully registered in Operations Command. Responder team dispatched.*
            """.trimIndent()

            com.example.data.DatabaseService.addAIChatMessage(isUser = false, text = aiResponseText)
            isThinking = false

            if (voiceEnabled) {
                tts?.speak("Vision analysis: $category detected with ${result.confidence * 100} percent confidence. Dispatching ${result.suggestedTeam}.", TextToSpeech.QUEUE_FLUSH, null, "gemini_tts")
            }
        }
    }

    // Thinking/Listening Orb scale animations
    val infiniteTransition = rememberInfiniteTransition(label = "assistant_orb")
    val orbSize by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 54f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbSize"
    )

    val soundwavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * java.lang.Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("ai_assistant_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Block with AI Status ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161618))
                        .border(1.dp, Color(0xFF242427), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = "AI Companion Active",
                        tint = Color(0xFF008E47), // Official FIFA Green
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "FIFA MATCHDAY ASSISTANT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFC5A059), // Editorial Gold
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10 * textScale).sp
                        )
                    )
                    Text(
                        text = if (isThinking) "Generating expert stadium advice..." else if (isListeningMode) "Listening to voice request..." else "Live assistant active • ${currentLanguage.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (12 * textScale).sp
                        )
                    )
                }
            }

            // Quick Voice output toggle indicator
            IconButton(
                onClick = { AppState.voiceOutputEnabled.value = !voiceEnabled },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (voiceEnabled) Color(0xFF008E47).copy(alpha = 0.15f) else Color(0x11FFFFFF))
            ) {
                Icon(
                    imageVector = if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle text to speech",
                    tint = if (voiceEnabled) Color(0xFF008E47) else Color.White
                )
            }
        }

        // --- Language Selector LazyRow ---
        Text(
            text = "SELECT ASSISTANCE LANGUAGE:",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MutedColor(highContrast),
                fontSize = (10 * textScale).sp
            )
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Language.values()) { lang ->
                val isSelected = lang == currentLanguage
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) {
                                if (highContrast) Color.White else Color(0xFFC5A059).copy(alpha = 0.2f)
                            } else Color(0xFF1C1C1E)
                        )
                        .border(1.dp, if (isSelected) Color(0xFFC5A059) else Color(0xFF242427), RoundedCornerShape(20.dp))
                        .clickable { AppState.currentLanguage.value = lang }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = lang.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                if (highContrast) Color.Black else Color(0xFFC5A059)
                            } else Color.White,
                            fontSize = (12 * textScale).sp
                        )
                    )
                }
            }
        }

        // --- Apple Intelligence Hands-Free Voice Console ---
        if (isListeningMode) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(),
                isAiTheme = true
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title/Header of Voice Mode
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecordingSpeech) AccentEmerald else Color(0xFFFBBF24))
                            )
                            Text(
                                text = if (isRecordingSpeech) "REAL-TIME NEURAL SPEECH INPUT" else "HANDS-FREE SIMULATOR ACTIVE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isRecordingSpeech) AccentEmerald else Color(0xFFC5A059),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                        
                        // Close Voice Mode button
                        IconButton(
                            onClick = { toggleVoiceInput() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Speech Input",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Soundwave Animation Panel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141416))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Canvas(modifier = Modifier.weight(1f).height(36.dp)) {
                            val width = size.width
                            val height = size.height
                            val midY = height / 2f
                            val amplitudeMultiplier = if (isRecordingSpeech) {
                                1f + (currentRmsDb.coerceIn(-2f, 10f) + 2f) * 1.5f
                            } else {
                                8f
                            }
                            
                            for (layer in 0..2) {
                                val waveColor = when (layer) {
                                    0 -> Color(0xAA10B981) // FIFA Green
                                    1 -> Color(0xAAFBBF24) // World Cup Gold
                                    else -> Color(0xAA00C2FF) // Cyan
                                }
                                val baseAmplitude = (4f - layer) * (amplitudeMultiplier / 4f)
                                val frequency = 0.03f + (layer * 0.012f)
                                val path = androidx.compose.ui.graphics.Path()
                                
                                for (x in 0..width.toInt() step 5) {
                                    val y = midY + baseAmplitude * sin(frequency * x + soundwavePhase + (layer * 1.5f))
                                    if (x == 0) {
                                        path.moveTo(x.toFloat(), y)
                                    } else {
                                        path.lineTo(x.toFloat(), y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = waveColor,
                                    style = Stroke(width = 2.5f)
                                )
                            }
                        }

                        // Pulse/Listening indicator label
                        Text(
                            text = if (isRecordingSpeech) "LISTENING..." else "TAP PRESET OR SAY...",
                            fontWeight = FontWeight.Black,
                            color = if (isRecordingSpeech) AccentEmerald else Color.LightGray,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Display errors beautifully
                    speechRecognitionError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x22EF4444))
                                .border(1.dp, Color(0x44EF4444), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                Text(
                                    text = err,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                // Try again button
                                Text(
                                    text = "RETRY",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .clickable { 
                                            speechRecognitionError = null
                                            startSpeechRecognizer() 
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Hands-Free Preset Verbal Prompts Chips (simulate spoken input)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SIMULATE SPOKEN HANDS-FREE PHRASES:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF8E8E93),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val handsFreeCommands = listOf(
                            "Where is the nearest wheelchair elevator?",
                            "Show transit schedule & carbon points",
                            "Is there a sensory room active nearby?",
                            "What recycling bonuses can I claim?",
                            "Give me a quick multilingual translation"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(handsFreeCommands) { cmd ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x11FFFFFF))
                                        .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable {
                                            inputQuery = cmd
                                            handleSend(cmd)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Simulate Speak",
                                            tint = Color(0xFFC5A059),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = cmd,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Active Chat Message Log ---
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val alignEnd = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .testTag(if (alignEnd) "user_message" else "ai_message"),
                        cornerRadius = 14.dp,
                        isAiTheme = !msg.isUser
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (msg.isUser) "DAVID BECKHAM" else "FIFA MATCHDAY COMPANION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (msg.isUser) Color(0xFF008E47) else Color(0xFFC5A059), // FIFA Green vs Gold
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (10 * textScale).sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = msg.timestamp,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF8E8E93),
                                        fontSize = (9 * textScale).sp
                                    )
                                )
                            }
                            BeautifulAiMessage(
                                text = msg.text,
                                textScale = textScale
                            )

                            // --- INTERACTIVE RICH CARDS INSIDE RESPONSE ---
                            if (!msg.isUser) {
                                val textLower = msg.text.lowercase()
                                
                                // 1. Wayfinding locator / accessibility map mini-widget
                                if (textLower.contains("section 114") || textLower.contains("elevator") || textLower.contains("sensory") || textLower.contains("wheelchair")) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1C1C1E))
                                            .border(1.dp, Color(0xFF242427), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Map, contentDescription = "Seat map", tint = Color(0xFFC5A059), modifier = Modifier.size(14.dp))
                                                Text("FIFA OFFICIAL WAYFINDING LOCATOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5A059), letterSpacing = 0.5.sp)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Origin: Sec 114, Row M", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text("Access Route: Elevator Core B", fontSize = 10.sp, color = Color(0xFF8E8E93))
                                                }
                                                Text("Est: 3 min", fontSize = 11.sp, color = Color(0xFFC5A059), fontWeight = FontWeight.Black)
                                            }
                                            // Mini custom canvas of seating
                                            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                                drawRect(Color(0xFF242427), size = size)
                                                // Draw elevator point
                                                drawCircle(Color(0xFFC5A059), center = Offset(size.width * 0.2f, size.height / 2f), radius = 4f)
                                                // Draw path line
                                                drawLine(Color(0xFF008E47), start = Offset(size.width * 0.2f, size.height / 2f), end = Offset(size.width * 0.8f, size.height / 2f), strokeWidth = 2f)
                                                // Draw seat point
                                                drawCircle(Color.White, center = Offset(size.width * 0.8f, size.height / 2f), radius = 4f)
                                            }
                                        }
                                    }
                                }

                                // 2. Live gate queue status / crowd flow mini-widget
                                if (textLower.contains("gate") || textLower.contains("concourse") || textLower.contains("queue") || textLower.contains("wait")) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1C1C1E))
                                            .border(1.dp, Color(0xFF242427), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Groups, contentDescription = "Queue status", tint = Color(0xFFC5A059), modifier = Modifier.size(14.dp))
                                                Text("OFFICIAL GATE FLOW MONITOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5A059), letterSpacing = 0.5.sp)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Gate C Queue", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text("Wait: < 3 mins", fontSize = 11.sp, color = Color(0xFF008E47), fontWeight = FontWeight.Bold)
                                            }
                                            // Mini custom density bar
                                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0xFF242427))) {
                                                Box(modifier = Modifier.fillMaxWidth(0.18f).fillMaxHeight().background(Color(0xFF008E47)))
                                            }
                                        }
                                    }
                                }

                                // 3. Eco rewards summary mini-widget
                                if (textLower.contains("carbon") || textLower.contains("sustainability") || textLower.contains("recycle") || textLower.contains("eco")) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1C1C1E))
                                            .border(1.dp, Color(0xFF242427), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Eco, contentDescription = "Eco points", tint = Color(0xFF008E47), modifier = Modifier.size(14.dp))
                                                Text("FIFA ECO INITIATIVE REWARDS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF008E47), letterSpacing = 0.5.sp)
                                            }
                                            Text("Recycling bonus near Sec 114: Active", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Current Session Saving: +1.2kg CO2 • +80 PTS", fontSize = 10.sp, color = Color(0xFF8E8E93))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        GlassCard(
                            modifier = Modifier.width(180.dp),
                            cornerRadius = 12.dp
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF10B981) // FIFA Green
                                )
                                Text(
                                    text = "AI is thinking...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.LightGray,
                                        fontSize = (12 * textScale).sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Suggested Prompts / Chips ---
        if (messages.size <= 2) {
            Text(
                text = "SUGGESTED CONCIERGE QUESTIONS:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutedColor(highContrast),
                    fontSize = (10 * textScale).sp
                )
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1410B981))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { handleSend(prompt) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontSize = (11 * textScale).sp
                            )
                        )
                    }
                }
            }
        }

        // --- Typing Field & Controls Row ---
        var showVisionMenu by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Camera / Vision Button
            Box {
                IconButton(
                    onClick = { showVisionMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x0FFFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                        .testTag("ai_vision_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Simulate Photo Vision",
                        tint = Color(0xFFFBBF24) // World Cup Gold
                    )
                }

                DropdownMenu(
                    expanded = showVisionMenu,
                    onDismissRequest = { showVisionMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.background).border(1.dp, Color(0x22FFFFFF))
                ) {
                    val simulatedPhotos = listOf(
                        "Trash Overflow",
                        "Blocked Exit",
                        "Broken Equipment",
                        "Damaged Seating",
                        "Long Queues",
                        "Medical Incident"
                    )
                    Text(
                        text = " SIMULATE AI VISION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24), // World Cup Gold
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    simulatedPhotos.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showVisionMenu = false
                                handleVisionAnalysis(category)
                            }
                        )
                    }
                }
            }

            // Speech activation toggle / Microphone Button
            IconButton(
                onClick = { toggleVoiceInput() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListeningMode) Color(0x33E11D48) else Color(0x0FFFFFFF))
                    .border(1.dp, if (isListeningMode) Color(0x80E11D48) else Color(0x1AFFFFFF), CircleShape)
                    .testTag("activate_voice_to_text_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Activate voice control",
                    tint = if (isListeningMode) Color(0xFFF43F5E) else Color.White
                )
            }

            TextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask the AI Match Co-Pilot...") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("chat_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x11FFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { handleSend(inputQuery) }
                )
            )

            FloatingActionButton(
                onClick = { handleSend(inputQuery) },
                shape = CircleShape,
                containerColor = if (highContrast) Color.White else Color(0xFF10B981), // FIFA Green
                contentColor = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_chat_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}
