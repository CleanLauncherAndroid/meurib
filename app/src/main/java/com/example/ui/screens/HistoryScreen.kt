package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.model.SentenceIrabResult
import com.example.ui.components.StaggeredReveal
import com.example.ui.components.categoryColors
import com.example.ui.components.pressScale
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.LocalArabicFontMultiplier
import com.example.ui.theme.LocalIsDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    historyItems: List<SentenceIrabResult>,
    totalCount: Int,
    pinnedCount: Int,
    searchQuery: String,
    showPinnedOnly: Boolean,
    onSearchChange: (String) -> Unit,
    onTogglePinnedFilter: () -> Unit,
    onSelectSentence: (SentenceIrabResult) -> Unit,
    onToggleItemPin: (SentenceIrabResult) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit,
    formatClipboard: (SentenceIrabResult) -> String,
    onCopiedMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val feedback = LocalFeedbackController.current
    val clipboardManager = LocalClipboardManager.current
    val fontMult = LocalArabicFontMultiplier.current
    val isDark = LocalIsDarkTheme.current
    val goldAccent = if (isDark) GoldSecondaryDark else GoldSecondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Stats Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "سجل الجمل المحللة",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "محفوظ محلياً في قاعدة بيانات الجهاز • $totalCount جملة ($pinnedCount مميزة)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (totalCount > 0 || searchQuery.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                    ) {
                        IconButton(
                            onClick = {
                                feedback.perform(SoundEffectType.TAP)
                                showClearDialog = true
                            },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح السجل بالكامل",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Sleek Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                placeholder = { Text("ابحث في الجمل المحفوظة أو نوع التركيب...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                feedback.perform(SoundEffectType.TAP)
                                onSearchChange("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح البحث"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showPinnedOnly,
                    onClick = {
                        feedback.perform(SoundEffectType.TOGGLE)
                        onTogglePinnedFilter()
                    },
                    label = { Text("المحفوظات المميزة ($pinnedCount)") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showPinnedOnly) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }

            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, goldAccent.copy(alpha = 0.55f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(120.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_muerib_clean),
                                contentDescription = "أيقونة تطبيق معرب",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank() || showPinnedOnly) {
                                "لا توجد نتائج مطابقة لبحثك"
                            } else {
                                stringResource(R.string.empty_history_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || showPinnedOnly) {
                                "جرّب إزالة عامل التصفية أو البحث بكلمة مختلفة."
                            } else {
                                stringResource(R.string.empty_history_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(historyItems, key = { _, it -> it.id }) { idx, item ->
                        StaggeredReveal(index = idx, delayStepMs = 30) {
                            val cardInteraction = remember { MutableInteractionSource() }
                            val bookmarkScale by animateFloatAsState(
                                targetValue = if (item.isPinned) 1.15f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "pin_scale"
                            )

                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (item.isPinned) 1.25.dp else 1.dp,
                                    color = if (item.isPinned) {
                                        goldAccent.copy(alpha = 0.72f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                    }
                                ),
                                tonalElevation = 1.dp,
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(cardInteraction, pressedScale = 0.985f)
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable(
                                        interactionSource = cardInteraction,
                                        indication = ripple(),
                                        onClick = {
                                            feedback.perform(SoundEffectType.OPEN_CARD)
                                            onSelectSentence(item)
                                        }
                                    )
                                    .testTag("history_item_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // Accent stripe on leading side
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .fillMaxHeight()
                                            .background(
                                                if (item.isPinned) goldAccent else MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                            )
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = item.sentenceType,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        feedback.perform(SoundEffectType.COPY, strongHaptic = true)
                                                        clipboardManager.setText(AnnotatedString(formatClipboard(item)))
                                                        onCopiedMessage()
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "نسخ الإعراب",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        feedback.perform(
                                                            if (item.isPinned) SoundEffectType.TOGGLE else SoundEffectType.BOOKMARK,
                                                            strongHaptic = !item.isPinned
                                                        )
                                                        onToggleItemPin(item)
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.isPinned) {
                                                            Icons.Default.Bookmark
                                                        } else {
                                                            Icons.Default.BookmarkBorder
                                                        },
                                                        contentDescription = "تثبيت الجملة",
                                                        tint = if (item.isPinned) {
                                                            goldAccent
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.graphicsLayer {
                                                            scaleX = bookmarkScale
                                                            scaleY = bookmarkScale
                                                        }
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        feedback.perform(SoundEffectType.TAP)
                                                        onDeleteItem(item.id)
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "حذف الجملة",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = item.sentence,
                                            fontFamily = AmiriFontFamily,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontSize = MaterialTheme.typography.headlineSmall.fontSize * fontMult
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            item.words.take(5).forEach { w ->
                                                val (pillBg, pillFg) = categoryColors(w.category)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = pillBg.copy(alpha = 0.78f),
                                                    border = BorderStroke(0.5.dp, pillFg.copy(alpha = 0.25f))
                                                ) {
                                                    Text(
                                                        text = "${w.word}: ${w.syntacticRole}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = pillFg,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = formatTimestamp(item.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("مسح سجل الجمل") },
            text = { Text("هل تريد حذف جميع الجمل المحفوظة في السجل المحلي؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feedback.perform(SoundEffectType.ERROR, strongHaptic = true)
                        showClearDialog = false
                        onClearAll()
                    }
                ) {
                    Text("حذف الكل", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        feedback.perform(SoundEffectType.TAP)
                        showClearDialog = false
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

private fun formatTimestamp(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}
