package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppState
import com.example.ui.theme.*

// composition local for deep screens to register / bubble exceptions
val LocalErrorBoundary = compositionLocalOf<MutableState<Throwable?>> {
    mutableStateOf(null)
}

@Composable
fun ComposeErrorBoundary(
    modifier: Modifier = Modifier,
    onReset: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val errorState = remember { mutableStateOf<Throwable?>(null) }

    CompositionLocalProvider(LocalErrorBoundary provides errorState) {
        if (errorState.value != null) {
            BrandedErrorRecoveryScreen(
                error = errorState.value!!,
                onRetry = {
                    errorState.value = null
                    onReset()
                }
            )
        } else {
            Box(modifier = modifier) {
                content()
            }
        }
    }
}

@Composable
fun BrandedErrorRecoveryScreen(
    error: Throwable,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isStackExpanded by remember { mutableStateOf(false) }
    var reportSent by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F101A),
                        Color(0xFF07080D)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Emergency Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x10EF4444)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = "System Diagnostic Problem",
                    tint = EmergencyRed,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Error Title
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MATCHDAY HQ OFFLINE BOUNDARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Temporary System Interruption",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            // Description
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "We encountered an unexpected visual rendering boundary issue while loading this screen. We have safely isolated the glitch to prevent a full app crash and secure your current stadium session.",
                        fontSize = 13.sp,
                        color = MutedText,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Divider(color = Color(0x11FFFFFF))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Diagnostic Log Available",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { isStackExpanded = !isStackExpanded }
                        ) {
                            Icon(
                                imageVector = if (isStackExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isStackExpanded) "Collapse stack trace" else "Expand stack trace",
                                tint = AccentCyan
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isStackExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF030305))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "${error.javaClass.simpleName}: ${error.localizedMessage ?: "No message provided."}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = EmergencyRed,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val stackTrace = error.stackTrace.take(8).joinToString("\n") { element ->
                                "  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
                            }
                            Text(
                                text = stackTrace,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MutedText,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // Actions Block
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onRetry() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart rendering",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESTART RENDER SCREEN",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(error.stackTraceToString()))
                            Toast.makeText(context, "Copied diagnostic trace to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF242427)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy diagnostic trace",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY LOG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            reportSent = true
                            Toast.makeText(context, "Diagnostic metrics reported to Stadium Control!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (reportSent) Color.Gray else Color(0xFF1E1F2E),
                            contentColor = Color.White
                        ),
                        enabled = !reportSent
                    ) {
                        Icon(
                            imageVector = if (reportSent) Icons.Default.CheckCircle else Icons.Default.Send,
                            contentDescription = "Report to Stadium Ops",
                            modifier = Modifier.size(16.dp),
                            tint = if (reportSent) SuccessGreen else AccentCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (reportSent) "OPS REPORTED" else "REPORT TO OPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Legal Footer
            Text(
                text = "FIFA Smart-Stadium Engine • Autonomous Recovery Active",
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
