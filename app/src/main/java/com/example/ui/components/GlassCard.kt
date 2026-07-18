package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AppState
import com.example.ui.theme.CardGlassBg
import com.example.ui.theme.CardGlassBorder

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isAiTheme: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isHighContrast = AppState.highContrastMode.collectAsState().value
    val isDarkMode = AppState.isDarkMode.collectAsState().value
    
    // In high-contrast mode, use pure high contrast border and absolute flat dark backdrops
    val backgroundBrush = if (isHighContrast) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF111111), Color(0xFF111111))
        )
    } else if (isAiTheme) {
        // FIFA AI Assistant premium style: deep solid forest graphite
        if (isDarkMode) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF111714), 
                    Color(0xFF0A0E0C)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE8F5E9), 
                    Color(0xFFC8E6C9)
                )
            )
        }
    } else {
        // Elegant minimal Very Dark Graphite background or elegant light surface
        if (isDarkMode) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF161618), 
                    Color(0xFF131315)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF), 
                    Color(0xFFF3F4F6)
                )
            )
        }
    }

    val borderStroke = if (isHighContrast) {
        Modifier.border(
            width = 2.dp,
            color = Color(0xFF00FF00), // High-contrast green
            shape = RoundedCornerShape(cornerRadius)
        )
    } else if (isAiTheme) {
        Modifier.border(
            width = 1.dp,
            color = Color(0xFF008E47).copy(alpha = 0.4f), // Official FIFA Green accent border
            shape = RoundedCornerShape(cornerRadius)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = if (isDarkMode) Color(0xFF242427) else Color(0xFFE5E7EB), // Crisp minimal graphite or light grey border
            shape = RoundedCornerShape(cornerRadius)
        )
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val contentColor = if (isHighContrast) {
        Color.White
    } else if (isDarkMode) {
        Color.White
    } else {
        Color(0xFF131315)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isHighContrast) 0.dp else 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0x22000000),
                spotColor = Color(0x44000000)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundBrush)
            .then(borderStroke)
            .then(clickModifier)
            .padding(16.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
