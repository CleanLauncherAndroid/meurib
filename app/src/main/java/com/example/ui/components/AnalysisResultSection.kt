package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.model.SentenceIrabResult
import com.example.model.WordIrabResult
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.LocalArabicFontMultiplier
import com.example.ui.theme.LocalIsDarkTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisResultSection(
    result: SentenceIrabResult,
    formatClipboard: (SentenceIrabResult) -> String,
    onWordClick: (WordIrabResult) -> Unit,
    onCopiedMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val feedback = LocalFeedbackController.current
    val fontMult = LocalArabicFontMultiplier.current
    val isDark = LocalIsDarkTheme.current
    val goldAccent = if (isDark) GoldSecondaryDark else GoldSecondary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sentence Summary Banner + Copy & Share
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
            border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.45f)),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "نوع التركيب: ${result.sentenceType}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val engineTag = if (result.layer2Used) "التحليل السياقي المتقدم" else "التحليل الصرفي الأساسي"
                        Text(
                            text = "عدد الكلمات: ${result.words.size} • $engineTag",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            feedback.perform(SoundEffectType.COPY, strongHaptic = true)
                            clipboardManager.setText(AnnotatedString(formatClipboard(result)))
                            onCopiedMessage()
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("copy_irab_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_copy),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            feedback.perform(SoundEffectType.TAP)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, formatClipboard(result))
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "مشاركة الإعراب")
                            )
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("share_irab_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مشاركة",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Visual Sentence Role Pills Strip
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            result.words.forEachIndexed { idx, w ->
                StaggeredReveal(index = idx, delayStepMs = 30) {
                    val (bg, fg) = categoryColors(w.category)
                    val interaction = remember { MutableInteractionSource() }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bg,
                        border = BorderStroke(1.dp, fg.copy(alpha = 0.28f)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .pressScale(interaction, pressedScale = 0.94f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = ripple(),
                                onClick = {
                                    feedback.perform(SoundEffectType.OPEN_CARD)
                                    onWordClick(w)
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = w.diacritizedWord.ifBlank { w.word },
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = MaterialTheme.typography.titleMedium.fontSize * fontMult
                                ),
                                fontWeight = FontWeight.Bold,
                                color = fg
                            )
                            Text(
                                text = w.syntacticRole,
                                style = MaterialTheme.typography.labelSmall,
                                color = fg.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }
        }

        // Grammar Category Color Legend
        IrabRoleLegendBar()

        // 4-Column Arabic I'rab Table
        IrabTable(
            words = result.words,
            onWordClick = onWordClick
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "اضغط على أي كلمة أو صف لعرض الإعراب الكامل والجذر والوزن الصرفي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
