package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.engine.EngineStatus
import com.example.model.SentenceIrabResult
import com.example.model.WordIrabResult
import com.example.ui.AnalysisUiState
import com.example.ui.components.AnalysisResultSection
import com.example.ui.components.AnalyzerEmptyGuide
import com.example.ui.components.AnalyzingShimmerCard
import com.example.ui.components.ManuscriptHeroBanner
import com.example.ui.components.pressScale
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.LocalArabicFontMultiplier
import com.example.ui.theme.LocalIsDarkTheme

private val PRESET_SENTENCES = listOf(
    "ذهب الولد إلى المدرسة",
    "كَتَبَ الطَّالِبُ الدَّرْسَ",
    "كُتِبَ الدَّرْسُ",
    "الطلاب يكتبون الدرس",
    "العلمُ نورٌ",
    "إنّ اللهَ غفورٌ",
    "يا طالبُ اجتهدْ",
    "نجح الطالب نجاحاً"
)

private val QUICK_HARAKAT = listOf(
    "َ" to "فتحة",
    "ُ" to "ضمة",
    "ِ" to "كسرة",
    "ْ" to "سكون",
    "ً" to "تنوين فتح",
    "ٌ" to "تنوين ضم",
    "ٍ" to "تنوين كسر",
    "ّ" to "شدة"
)

@Composable
fun AnalyzerScreen(
    engineStatus: EngineStatus,
    sentenceInput: String,
    uiState: AnalysisUiState,
    onInputChange: (String) -> Unit,
    onAppendHaraka: (String) -> Unit,
    onClearClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onPresetClick: (String) -> Unit,
    onWordClick: (WordIrabResult) -> Unit,
    onRetryEngine: () -> Unit,
    onShareAppClick: () -> Unit,
    formatClipboard: (SentenceIrabResult) -> String,
    onCopiedMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val feedback = LocalFeedbackController.current
    val fontMult = LocalArabicFontMultiplier.current
    val isDark = LocalIsDarkTheme.current
    val goldAccent = if (isDark) GoldSecondaryDark else GoldSecondary

    // Play audio/haptic cues only on real state transitions (not initial composition)
    var previousUiState by remember { mutableStateOf(uiState) }
    LaunchedEffect(uiState) {
        if (uiState !== previousUiState) {
            when (uiState) {
                is AnalysisUiState.Success -> {
                    if (previousUiState is AnalysisUiState.Analyzing) {
                        feedback.perform(SoundEffectType.SUCCESS, strongHaptic = true)
                    }
                }
                is AnalysisUiState.Error -> {
                    feedback.perform(SoundEffectType.ERROR, strongHaptic = true)
                }
                else -> Unit
            }
            previousUiState = uiState
        }
    }

    val wordCount = remember(sentenceInput) {
        sentenceInput.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ManuscriptHeroBanner(
                engineStatus = engineStatus,
                onRetryEngine = onRetryEngine,
                onShareAppClick = onShareAppClick
            )

            // Input & Harakat Studio Card
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (sentenceInput.isNotBlank()) 1.25.dp else 1.dp,
                    color = if (sentenceInput.isNotBlank()) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    }
                ),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.input_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AnimatedVisibility(visible = wordCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            ) {
                                Text(
                                    text = "$wordCount كلمات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = sentenceInput,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sentence_input"),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.input_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = AmiriFontFamily,
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontMult,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        minLines = 2,
                        maxLines = 4,
                        trailingIcon = {
                            if (sentenceInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        feedback.perform(SoundEffectType.TAP)
                                        onClearClick()
                                    },
                                    modifier = Modifier.testTag("clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.btn_clear)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                feedback.perform(SoundEffectType.ANALYZE_START)
                                onAnalyzeClick()
                            }
                        ),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f)
                        )
                    )

                    // Harakat Studio Quick Bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "لوحة التشكيل السريع:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QUICK_HARAKAT.forEach { (mark, name) ->
                                val enabled = sentenceInput.isNotBlank()
                                val harakaInteraction = remember { MutableInteractionSource() }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (enabled) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (enabled) {
                                            goldAccent.copy(alpha = 0.35f)
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 52.dp)
                                        .then(
                                            if (enabled) {
                                                Modifier
                                                    .pressScale(harakaInteraction, pressedScale = 0.90f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable(
                                                        interactionSource = harakaInteraction,
                                                        indication = ripple(),
                                                        onClick = {
                                                            feedback.perform(SoundEffectType.HARAKA)
                                                            onAppendHaraka(mark)
                                                        }
                                                    )
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "ـ$mark",
                                            fontFamily = AmiriFontFamily,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (enabled) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                            }
                                        )
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (enabled) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Preset Example Sentences
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "أمثلة فصيحة للتجربة الفورية:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PRESET_SENTENCES.forEachIndexed { idx, sample ->
                                AssistChip(
                                    onClick = {
                                        keyboardController?.hide()
                                        feedback.perform(SoundEffectType.ANALYZE_START)
                                        onPresetClick(sample)
                                    },
                                    label = {
                                        Text(
                                            text = sample,
                                            fontFamily = AmiriFontFamily,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("preset_chip_$idx")
                                )
                            }
                        }
                    }

                    // Primary Analyze Action Button
                    val btnInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            feedback.perform(SoundEffectType.ANALYZE_START, strongHaptic = true)
                            onAnalyzeClick()
                        },
                        enabled = engineStatus is EngineStatus.Ready && uiState !is AnalysisUiState.Analyzing,
                        interactionSource = btnInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .pressScale(btnInteraction, pressedScale = 0.97f)
                            .testTag("analyze_button"),
                        shape = MaterialTheme.shapes.medium,
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 1.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState is AnalysisUiState.Analyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "جاري الإعراب...",
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_analyze),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Dynamic State Content Area
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(tween(240)) togetherWith fadeOut(tween(150))
                },
                label = "analyzer_state"
            ) { state ->
                when (state) {
                    is AnalysisUiState.Idle -> {
                        AnalyzerEmptyGuide()
                    }
                    is AnalysisUiState.Analyzing -> {
                        AnalyzingShimmerCard()
                    }
                    is AnalysisUiState.Error -> {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("analysis_error_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    is AnalysisUiState.Success -> {
                        AnalysisResultSection(
                            result = state.result,
                            formatClipboard = formatClipboard,
                            onWordClick = onWordClick,
                            onCopiedMessage = onCopiedMessage
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
