package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.model.WordIrabResult
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.HeaderEmeraldEnd
import com.example.ui.theme.HeaderEmeraldStart
import com.example.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordDetailBottomSheet(
    word: WordIrabResult,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (pillBg, pillText) = categoryColors(word.category)
    val feedback = LocalFeedbackController.current
    val clipboardManager = LocalClipboardManager.current
    var copiedJustNow by remember { mutableStateOf(false) }

    val isDark = LocalIsDarkTheme.current
    val goldAccent = if (isDark) GoldSecondaryDark else GoldSecondary

    LaunchedEffect(copiedJustNow) {
        if (copiedJustNow) {
            delay(1600)
            copiedJustNow = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ornamental Emerald Calligraphic Word Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(HeaderEmeraldStart, HeaderEmeraldEnd)
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = word.diacritizedWord.ifBlank { word.word },
                            fontFamily = AmiriFontFamily,
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        if (word.diacritizedWord != word.word) {
                            Text(
                                text = "الأصل المدخل: ${word.word}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD0ECE2)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = pillBg,
                            border = BorderStroke(1.dp, pillText.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = word.syntacticRole,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = pillText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, GoldSecondaryDark.copy(alpha = 0.45f))
                        ) {
                            IconButton(
                                onClick = {
                                    feedback.perform(SoundEffectType.COPY, strongHaptic = true)
                                    val summary = "${word.diacritizedWord.ifBlank { word.word }} (${word.partOfSpeech}): ${word.fullIrab}"
                                    clipboardManager.setText(AnnotatedString(summary))
                                    copiedJustNow = true
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (copiedJustNow) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "نسخ إعراب الكلمة",
                                    tint = if (copiedJustNow) Color(0xFF9AF3D6) else GoldSecondaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Complete Grammatical Explanation Card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = goldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "الإعراب الكامل",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = word.fullIrab,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Root Letters Spotlight Card (when Arabic root is extracted)
            word.root?.takeIf { it.isNotBlank() }?.let { root ->
                RootSpotlightCard(root = root, pattern = word.pattern, accentColor = goldAccent)
            }

            OrnamentalDivider(accentColor = goldAccent)

            // Morphological Properties Grid (من محرك الفراهيدي)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "البطاقة الصرفية (محرك الفراهيدي)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MorphChip("نوع الكلمة", word.partOfSpeech)
                MorphChip("الصيغة المعجمية", word.lemma)
                if (word.stem.isNotBlank()) MorphChip("الجذع", word.stem)
                word.pattern?.let { MorphChip("الوزن الصرفي", it) }
                word.tense?.let { MorphChip("الزمن", it) }
                word.gender?.let { MorphChip("الجنس", it) }
                word.number?.let { MorphChip("العدد", it) }
            }

            if (word.proclitics.isNotEmpty() || word.enclitics.isNotEmpty()) {
                OrnamentalDivider(accentColor = goldAccent)
                Text(
                    text = "السوابق واللواحق",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    word.proclitics.forEach { affix ->
                        MorphChip(
                            label = "سابقة: ${affix.form}",
                            value = affix.description ?: "متصلة"
                        )
                    }
                    word.enclitics.forEach { affix ->
                        MorphChip(
                            label = "لاحقة: ${affix.form}",
                            value = affix.description ?: "متصلة"
                        )
                    }
                }
            }

            if (word.allReadings.size > 1) {
                OrnamentalDivider(accentColor = goldAccent)
                Text(
                    text = "قراءات صرفية أخرى محتملة في المعجم",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    word.allReadings.take(4).forEachIndexed { idx, reading ->
                        StaggeredReveal(index = idx, delayStepMs = 30) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = reading.diacritized.ifBlank { reading.lemma },
                                        fontFamily = AmiriFontFamily,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val details = listOfNotNull(
                                        reading.partOfSpeech,
                                        reading.tense ?: reading.subType,
                                        reading.caseOrMood
                                    ).joinToString(" • ")
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RootSpotlightCard(
    root: String,
    pattern: String?,
    accentColor: Color
) {
    val rootLetters = remember(root) {
        val cleaned = root.trim().replace(" ", "").filter { !it.isWhitespace() }
        if (cleaned.length in 2..6) cleaned.map { it.toString() } else listOf(root.trim())
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "الجذر اللغوي",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor
                )
                if (!pattern.isNullOrBlank()) {
                    Text(
                        text = "على وزن: $pattern",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rootLetters.forEachIndexed { index, ch ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ch,
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (index < rootLetters.lastIndex) {
                        Text(
                            text = "•",
                            color = accentColor,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MorphChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
