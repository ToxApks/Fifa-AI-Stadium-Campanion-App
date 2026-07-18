package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )

        Brush.linearGradient(
            colors = listOf(
                Color(0x0FFFFFFF),
                Color(0x22FFFFFF),
                Color(0x0FFFFFFF)
            ),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    cornerRadius: Dp = 8.dp
) {
    val brush = ShimmerBrush()
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun MatchCardSkeleton() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(width = 80.dp, height = 16.dp)
                SkeletonBox(width = 40.dp, height = 16.dp)
            }
            
            // Score Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team 1 Logo & Name
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(width = 48.dp, height = 48.dp, cornerRadius = 24.dp)
                    SkeletonBox(width = 70.dp, height = 12.dp)
                }
                
                // Score placeholder
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(width = 24.dp, height = 28.dp)
                    SkeletonBox(width = 12.dp, height = 12.dp)
                    SkeletonBox(width = 24.dp, height = 28.dp)
                }
                
                // Team 2 Logo & Name
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(width = 48.dp, height = 48.dp, cornerRadius = 24.dp)
                    SkeletonBox(width = 70.dp, height = 12.dp)
                }
            }
            
            Divider(color = Color(0x11FFFFFF))
            
            // Status & Action Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(width = 120.dp, height = 14.dp)
                SkeletonBox(width = 90.dp, height = 28.dp, cornerRadius = 14.dp)
            }
        }
    }
}

@Composable
fun NewsCardSkeleton() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(width = 70.dp, height = 12.dp)
                SkeletonBox(width = 50.dp, height = 12.dp)
            }
            SkeletonBox(height = 20.dp)
            SkeletonBox(width = 180.dp, height = 14.dp)
            
            Divider(color = Color(0x08FFFFFF))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(width = 90.dp, height = 12.dp)
                SkeletonBox(width = 60.dp, height = 12.dp)
            }
        }
    }
}

@Composable
fun TravelRouteSkeleton() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(width = 24.dp, height = 24.dp, cornerRadius = 12.dp)
                    SkeletonBox(width = 140.dp, height = 16.dp)
                }
                SkeletonBox(width = 60.dp, height = 16.dp)
            }
            
            SkeletonBox(height = 14.dp)
            SkeletonBox(width = 220.dp, height = 14.dp)
            
            Divider(color = Color(0x11FFFFFF))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(width = 80.dp, height = 24.dp, cornerRadius = 6.dp)
                SkeletonBox(width = 80.dp, height = 24.dp, cornerRadius = 6.dp)
            }
        }
    }
}

@Composable
fun ProfileFormSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Avatar and Title Skeleton
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            SkeletonBox(width = 54.dp, height = 54.dp, cornerRadius = 27.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(width = 150.dp, height = 18.dp)
                SkeletonBox(width = 220.dp, height = 12.dp)
            }
        }
        
        // Form Fields (3 blocks)
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(width = 80.dp, height = 12.dp)
                SkeletonBox(height = 56.dp, cornerRadius = 12.dp)
            }
        }
    }
}
