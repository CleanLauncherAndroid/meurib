package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.engine.EngineStatus
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.HeaderEmeraldEnd
import com.example.ui.theme.HeaderEmeraldMid
import com.example.ui.theme.HeaderEmeraldStart
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun ManuscriptHeroBanner(
    engineStatus: EngineStatus,
    onRetryEngine: () -> Unit,
    onShareAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val feedback = LocalFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }
    val infiniteTransition = rememberInfiniteTransition(label = "hero_ornament")
    val slowSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_spin"
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, GoldSecondaryDark.copy(alpha = 0.50f)),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource, pressedScale = 0.985f)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = GoldSecondaryDark),
                onClick = {
                    feedback.perform(SoundEffectType.OPEN_CARD, strongHaptic = true)
                    onShareAppClick()
                }
            )
            .testTag("hero_share_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Subtle decorative geometric texture under emerald gradient
            Image(
                painter = painterResource(id = R.drawable.img_manuscript_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.35f)
            )

            // Deep emerald editorial gradient overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                HeaderEmeraldStart.copy(alpha = 0.88f),
                                HeaderEmeraldMid.copy(alpha = 0.86f),
                                HeaderEmeraldEnd.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            // Rotating delicate octagram filigree watermark
            Canvas(modifier = Modifier.matchParentSize()) {
                val emblemCenter = Offset(size.width * 0.14f, size.height * 0.44f)
                val r = size.height * 0.52f
                drawCircle(
                    color = GoldSecondaryDark.copy(alpha = 0.13f),
                    radius = r,
                    center = emblemCenter,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                val sq = Path().apply {
                    val half = r * 0.68f
                    moveTo(emblemCenter.x - half, emblemCenter.y - half)
                    lineTo(emblemCenter.x + half, emblemCenter.y - half)
                    lineTo(emblemCenter.x + half, emblemCenter.y + half)
                    lineTo(emblemCenter.x - half, emblemCenter.y + half)
                    close()
                }
                rotate(slowSpin, pivot = emblemCenter) {
                    drawPath(sq, color = GoldSecondaryDark.copy(alpha = 0.16f), style = Stroke(width = 1.dp.toPx()))
                }
                rotate(slowSpin + 45f, pivot = emblemCenter) {
                    drawPath(sq, color = Color.White.copy(alpha = 0.10f), style = Stroke(width = 1.dp.toPx()))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = GoldSecondaryDark.copy(alpha = 0.22f),
                                border = BorderStroke(0.75.dp, GoldSecondaryDark.copy(alpha = 0.60f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = GoldSecondaryDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "مشاركة APK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldSecondaryDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "محلل النحو والإعراب العربي الصرفي",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE3F5EE)
                        )
                    }

                    // Official App Icon Badge
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, GoldSecondaryDark.copy(alpha = 0.65f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_muerib_clean),
                            contentDescription = "أيقونة تطبيق معرب",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Live Animated Engine Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.34f),
                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.14f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (engineStatus) {
                            is EngineStatus.Initializing -> {
                                PulsingStatusDot(color = GoldSecondaryDark, isPulsing = true, size = 8.dp)
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = GoldSecondaryDark
                                )
                                Text(
                                    text = "جاري تجهيز قاموس الفراهيدي المحلي...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                            is EngineStatus.Ready -> {
                                PulsingStatusDot(color = EmeraldGlow, isPulsing = true, size = 8.dp)
                                Icon(
                                    imageVector = if (engineStatus.layer2Ready) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.OfflinePin
                                    },
                                    contentDescription = null,
                                    tint = EmeraldGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                val label = if (engineStatus.layer2Ready) {
                                    "يعمل دون إنترنت • اضغط هنا لمشاركة التطبيق"
                                } else {
                                    stringResource(R.string.offline_badge)
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                            is EngineStatus.Error -> {
                                PulsingStatusDot(color = Color(0xFFFF8A80), isPulsing = true, size = 8.dp)
                                Text(
                                    text = "خطأ في التهيئة: ${engineStatus.message}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFFD2CC),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Surface(
                                    onClick = {
                                        feedback.perform(SoundEffectType.TAP)
                                        onRetryEngine()
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.20f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "إعادة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
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

@Composable
fun AnalyzerEmptyGuide(modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkTheme.current
    val goldAccent = if (isDark) GoldSecondaryDark else GoldSecondary

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.5.dp, goldAccent.copy(alpha = 0.55f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(84.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_muerib_clean),
                        contentDescription = "أيقونة تطبيق معرب",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "كيف يعمل «معرب» دون إنترنت؟",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "اكتب جملتك أو اختر مثالاً بالأعلى ليفككها محرك الفراهيدي إلى جذور وأوزان وإعراب كامل.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OrnamentalDivider(accentColor = goldAccent)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GuideFeatureTile(
                    icon = Icons.Default.AccountTree,
                    title = "تحليل صرفي دقيق",
                    subtitle = "يستخرج الجذر الثلاثي والوزن الصرفي والسوابق واللواحق لكل كلمة."
                )
                GuideFeatureTile(
                    icon = Icons.Default.Psychology,
                    title = "إعراب الجمل والتراكيب",
                    subtitle = "يميز الجملة الفعلية والاسمية، الفاعل والمفعول به والمبتدأ والخبر وإنّ وكان."
                )
                GuideFeatureTile(
                    icon = Icons.Default.AutoAwesome,
                    title = "علامات الإعراب الفرعية",
                    subtitle = "يدعم الأفعال الخمسة والممنوع من الصرف والمثنى وجمع المذكر والمؤنث السالم."
                )
            }
        }
    }
}

@Composable
private fun GuideFeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
