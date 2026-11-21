package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay

/**
 * Adds tactile spring scale compression on press without interrupting ripples or clicks.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Staggered reveal animation (fade + upward settle) for list cards, pills, and table rows.
 */
@Composable
fun StaggeredReveal(
    index: Int,
    delayStepMs: Int = 38,
    content: @Composable () -> Unit
) {
    var visible by remember(index) { mutableStateOf(false) }
    LaunchedEffect(index) {
        if (index > 0) {
            delay((index.coerceAtMost(10) * delayStepMs).toLong())
        }
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "stagger_alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "stagger_offset"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}

/**
 * Breathing live status indicator dot with halo ring.
 */
@Composable
fun PulsingStatusDot(
    color: Color,
    isPulsing: Boolean = true,
    size: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 2.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 0.55f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_alpha"
    )

    Box(
        modifier = modifier.size(size * 2.2f),
        contentAlignment = Alignment.Center
    ) {
        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = haloScale
                        scaleY = haloScale
                        alpha = haloAlpha
                    }
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Delicate Andalusian manuscript eight-pointed star divider between sections.
 */
@Composable
fun OrnamentalDivider(
    modifier: Modifier = Modifier,
    accentColor: Color = if (LocalIsDarkTheme.current) GoldSecondaryDark else GoldSecondary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.5f))
                    )
                )
        )
        Canvas(modifier = Modifier.size(16.dp)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val square = Path().apply {
                val half = r * 0.68f
                moveTo(center.x - half, center.y - half)
                lineTo(center.x + half, center.y - half)
                lineTo(center.x + half, center.y + half)
                lineTo(center.x - half, center.y + half)
                close()
            }
            drawPath(square, color = accentColor, style = Stroke(width = 1.4.dp.toPx()))
            rotate(degrees = 45f, pivot = center) {
                drawPath(square, color = accentColor, style = Stroke(width = 1.4.dp.toPx()))
            }
            drawCircle(color = accentColor, radius = r * 0.22f, center = center)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * Animated geometric shimmering card shown while the Farahidi engine analyzes a sentence.
 */
@Composable
fun AnalyzingShimmerCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing_shimmer")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star_spin"
    )

    val primary = MaterialTheme.colorScheme.primary
    val gold = if (LocalIsDarkTheme.current) GoldSecondaryDark else GoldSecondary

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            primary.copy(alpha = 0.09f),
                            gold.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerProgress * 600f, 0f),
                        end = Offset((shimmerProgress + 0.5f) * 600f, 220f)
                    )
                )
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Canvas(modifier = Modifier.size(38.dp)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val half = size.minDimension * 0.30f
                    val boxPath = Path().apply {
                        moveTo(c.x - half, c.y - half)
                        lineTo(c.x + half, c.y - half)
                        lineTo(c.x + half, c.y + half)
                        lineTo(c.x - half, c.y + half)
                        close()
                    }
                    rotate(rotation, pivot = c) {
                        drawPath(boxPath, color = primary, style = Stroke(width = 2.dp.toPx()))
                    }
                    rotate(rotation + 45f, pivot = c) {
                        drawPath(boxPath, color = gold, style = Stroke(width = 2.dp.toPx()))
                    }
                    drawCircle(color = primary, radius = 4.dp.toPx(), center = c)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "جاري التفكيك الصرفي واستنباط الإعراب...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                    Text(
                        text = "يمسح محرك الفراهيدي الجذور والأوزان والسياق النحوي محلياً",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}
