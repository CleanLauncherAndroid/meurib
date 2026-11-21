package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.model.IrabCategory
import com.example.model.WordIrabResult
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.IrabRolePalette
import com.example.ui.theme.LocalArabicFontMultiplier
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun IrabRoleLegendBar(modifier: Modifier = Modifier) {
    val entries = listOf(
        "مرفوع" to IrabCategory.MARFOU,
        "منصوب" to IrabCategory.MANSOUB,
        "مجرور" to IrabCategory.MAJROUR,
        "فعل مبني" to IrabCategory.VERB_MABNI,
        "حرف / أداة" to IrabCategory.PARTICLE
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entries.forEach { (label, category) ->
            val (bg, fg) = categoryColors(category)
            Surface(
                shape = RoundedCornerShape(50),
                color = bg.copy(alpha = 0.72f),
                border = BorderStroke(0.75.dp, fg.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(fg)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = fg
                    )
                }
            }
        }
    }
}

@Composable
fun IrabTable(
    words: List<WordIrabResult>,
    onWordClick: (WordIrabResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val goldAccent = if (isDark) GoldSecondaryDark else GoldSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("irab_table"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Manuscript Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderCell(
                    text = stringResource(R.string.col_word),
                    modifier = Modifier.weight(0.24f)
                )
                TableHeaderCell(
                    text = stringResource(R.string.col_pos),
                    modifier = Modifier.weight(0.18f)
                )
                TableHeaderCell(
                    text = stringResource(R.string.col_role),
                    modifier = Modifier.weight(0.24f)
                )
                TableHeaderCell(
                    text = stringResource(R.string.col_case),
                    modifier = Modifier.weight(0.34f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                goldAccent.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            words.forEachIndexed { index, item ->
                StaggeredReveal(index = index, delayStepMs = 35) {
                    IrabTableRow(
                        word = item,
                        isAlternate = index % 2 == 1,
                        onClick = { onWordClick(item) },
                        index = index
                    )
                }
                if (index < words.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun IrabTableRow(
    word: WordIrabResult,
    isAlternate: Boolean,
    onClick: () -> Unit,
    index: Int
) {
    val feedback = LocalFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }

    val rowBg = if (isAlternate) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    } else {
        Color.Transparent
    }

    val fontMult = LocalArabicFontMultiplier.current
    val (pillBg, pillText) = categoryColors(word.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 60.dp)
            .background(rowBg)
            .pressScale(interactionSource, pressedScale = 0.985f)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = {
                    feedback.perform(SoundEffectType.OPEN_CARD)
                    onClick()
                }
            )
            .padding(end = 10.dp)
            .testTag("irab_row_$index"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical color-coded category indicator stripe
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(pillText.copy(alpha = 0.72f))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Word + diacritized subtitle
            Column(modifier = Modifier.weight(0.24f)) {
                Text(
                    text = word.word,
                    fontFamily = AmiriFontFamily,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * fontMult
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (word.diacritizedWord.isNotBlank() && word.diacritizedWord != word.word) {
                    Text(
                        text = word.diacritizedWord,
                        fontFamily = AmiriFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 2. Part of speech
            Text(
                text = word.partOfSpeech,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.18f)
            )

            // 3. Syntactic Role (color-coded pill)
            Box(modifier = Modifier.weight(0.24f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pillBg,
                    border = BorderStroke(0.75.dp, pillText.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = word.syntacticRole,
                        style = MaterialTheme.typography.labelMedium,
                        color = pillText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 4. Grammatical State + subtle chevron affordance
            Row(
                modifier = Modifier.weight(0.34f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = word.grammaticalState,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun categoryColors(category: IrabCategory): Pair<Color, Color> {
    val dark = LocalIsDarkTheme.current
    return when (category) {
        IrabCategory.MARFOU -> if (dark) {
            IrabRolePalette.MarfouContainerDark to IrabRolePalette.MarfouTextDark
        } else {
            IrabRolePalette.MarfouContainer to IrabRolePalette.MarfouText
        }
        IrabCategory.MANSOUB -> if (dark) {
            IrabRolePalette.MansoubContainerDark to IrabRolePalette.MansoubTextDark
        } else {
            IrabRolePalette.MansoubContainer to IrabRolePalette.MansoubText
        }
        IrabCategory.MAJROUR -> if (dark) {
            IrabRolePalette.MajrourContainerDark to IrabRolePalette.MajrourTextDark
        } else {
            IrabRolePalette.MajrourContainer to IrabRolePalette.MajrourText
        }
        IrabCategory.VERB_MABNI -> if (dark) {
            IrabRolePalette.VerbMabniContainerDark to IrabRolePalette.VerbMabniTextDark
        } else {
            IrabRolePalette.VerbMabniContainer to IrabRolePalette.VerbMabniText
        }
        IrabCategory.PARTICLE, IrabCategory.OTHER -> if (dark) {
            IrabRolePalette.ParticleContainerDark to IrabRolePalette.ParticleTextDark
        } else {
            IrabRolePalette.ParticleContainer to IrabRolePalette.ParticleText
        }
    }
}
