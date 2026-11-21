package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.data.ArabicFontScale
import com.example.data.ThemeMode
import com.example.data.UserSettings
import com.example.engine.EngineStatus
import com.example.ui.components.PulsingStatusDot
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.LocalArabicFontMultiplier
import com.example.ui.theme.LocalIsDarkTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    engineStatus: EngineStatus,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFontScaleChange: (ArabicFontScale) -> Unit,
    onSoundEffectsChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onUseLayer2Change: (Boolean) -> Unit,
    onDetailedSignsChange: (Boolean) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onRetryEngine: () -> Unit,
    onClearHistory: () -> Unit,
    onNotifyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmClearHistory by remember { mutableStateOf(false) }
    val feedback = LocalFeedbackController.current
    val fontMult = LocalArabicFontMultiplier.current
    val isDark = LocalIsDarkTheme.current
    val goldAccent = if (isDark) GoldSecondaryDark else GoldSecondary

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "الإعدادات المتقدمة",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تخصيص المظهر والخطوط والتفاعل الصوتي ومحرك الفراهيدي",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Appearance & Typography
            SettingsSectionCard(
                title = "المظهر والخط العربي",
                icon = Icons.Default.Palette
            ) {
                Text(
                    text = "نمط الألوان",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = {
                                feedback.perform(SoundEffectType.TOGGLE)
                                onThemeModeChange(mode)
                            },
                            label = { Text(mode.labelAr) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("theme_chip_${mode.name}")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "حجم الخط العربي للجمل والجدول",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArabicFontScale.entries.forEach { scale ->
                        FilterChip(
                            selected = settings.fontScale == scale,
                            onClick = {
                                feedback.perform(SoundEffectType.TOGGLE)
                                onFontScaleChange(scale)
                            },
                            label = { Text(scale.labelAr) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                // Live Arabic Typography Preview
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "معاينة فورية للخط:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "يَكْتُبُ الطَّالِبُ الدَّرْسَ بِإِتْقَانٍ",
                            fontFamily = AmiriFontFamily,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = MaterialTheme.typography.titleLarge.fontSize * fontMult
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2. Sound Effects & Tactile Haptics
            SettingsSectionCard(
                title = "الأصوات والاهتزاز التفاعلي",
                icon = Icons.AutoMirrored.Filled.VolumeUp
            ) {
                SettingsSwitchRow(
                    title = "المؤثرات الصوتية التفاعلية",
                    subtitle = "نغمات هادئة مدمجة عند الضغط على الحركات وإتمام الإعراب وفتح البطاقات.",
                    checked = settings.soundEffectsEnabled,
                    onCheckedChange = { enabled ->
                        onSoundEffectsChange(enabled)
                        if (enabled) {
                            feedback.previewSoundOnly(SoundEffectType.BOOKMARK)
                        }
                    },
                    testTag = "switch_sound_effects"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                SettingsSwitchRow(
                    title = "الاهتزاز اللمسي (Haptics)",
                    subtitle = "استجابة لمسية لطيفة مع الأزرار وتبديل التبويبات.",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { enabled ->
                        onHapticsChange(enabled)
                        if (enabled) {
                            feedback.previewHapticOnly()
                        }
                    },
                    testTag = "switch_haptics"
                )

                OutlinedButton(
                    onClick = {
                        feedback.previewSoundOnly(SoundEffectType.SUCCESS)
                        feedback.previewHapticOnly()
                        onNotifyMessage("تم تشغيل نغمة النجاح التفاعلية")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تجربة نغمة الإعراب والاهتزاز")
                }
            }

            // 3. Grammar & Farahidi Engine
            SettingsSectionCard(
                title = "محرك الفراهيدي وقواعد الإعراب",
                icon = Icons.Default.Tune
            ) {
                SettingsSwitchRow(
                    title = "المحلل السياقي المتقدم (Layer 2)",
                    subtitle = "يستخدم النموذج اللغوي لترجيح المعنى الأنسب للكلمة داخل السياق.",
                    checked = settings.useLayer2Context,
                    onCheckedChange = { enabled ->
                        feedback.perform(SoundEffectType.TOGGLE)
                        onUseLayer2Change(enabled)
                    },
                    testTag = "switch_layer2"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                SettingsSwitchRow(
                    title = "تفصيل علامات الإعراب الفرعية",
                    subtitle = "عرض السبب الكامل في الجدول مثل «الألف لأنه مثنى» بدلاً من الصيغة المختصرة.",
                    checked = settings.detailedIrabSigns,
                    onCheckedChange = { enabled ->
                        feedback.perform(SoundEffectType.TOGGLE)
                        onDetailedSignsChange(enabled)
                    },
                    testTag = "switch_detailed_signs"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                SettingsSwitchRow(
                    title = "الحفظ التلقائي في السجل",
                    subtitle = "حفظ كل جملة يتم إعرابها تلقائياً في قاعدة البيانات المحلية Room.",
                    checked = settings.autoSaveHistory,
                    onCheckedChange = { enabled ->
                        feedback.perform(SoundEffectType.TOGGLE)
                        onAutoSaveChange(enabled)
                    },
                    testTag = "switch_auto_save"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val dotColor = when (engineStatus) {
                            is EngineStatus.Ready -> EmeraldGlow
                            is EngineStatus.Initializing -> goldAccent
                            is EngineStatus.Error -> Color(0xFFE57373)
                        }
                        PulsingStatusDot(color = dotColor, isPulsing = true, size = 9.dp)
                        Column {
                            Text(
                                text = "حالة محرك الفراهيدي الأوفلاين",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            val statusLabel = when (engineStatus) {
                                is EngineStatus.Initializing -> "جاري التجهيز..."
                                is EngineStatus.Ready -> if (engineStatus.layer2Ready) {
                                    "جاهز بالكامل (الصرفي + السياقي)"
                                } else {
                                    "جاهز (التحليل الصرفي الأساسي)"
                                }
                                is EngineStatus.Error -> "خطأ: ${engineStatus.message}"
                            }
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            feedback.perform(SoundEffectType.TAP)
                            onRetryEngine()
                            onNotifyMessage("جاري إعادة تهيئة محرك الفراهيدي...")
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة التهيئة")
                    }
                }
            }

            // 4. Local Data Management
            SettingsSectionCard(
                title = "إدارة البيانات المحلية",
                icon = Icons.Default.Storage
            ) {
                Text(
                    text = "يتم حفظ سجل الجمل المحلّلة محلياً على جهازك دون اتصال بالإنترنت.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        feedback.perform(SoundEffectType.TAP)
                        confirmClearHistory = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مسح سجل الجمل", color = MaterialTheme.colorScheme.error)
                }
            }

            // 5. About Card
            SettingsSectionCard(
                title = "عن تطبيق معرب",
                icon = Icons.Default.Info
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdded,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "معرب • الإصدار 1.1 (أوفلاين بالكامل)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "يعتمد التطبيق على مكتبة الفراهيدي الصرفية (AlKhalil Morpho Sys 2) المدمجة محلياً دون اتصال بالإنترنت، مع محرك قواعد نحوية عربي وقاعدة بيانات Room محلية لحفظ السجل.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("مسح سجل الجمل") },
            text = { Text("هل أنت متأكد من حذف جميع الجمل المحفوظة في السجل؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feedback.perform(SoundEffectType.ERROR, strongHaptic = true)
                        confirmClearHistory = false
                        onClearHistory()
                        onNotifyMessage("تم مسح سجل الجمل")
                    }
                ) {
                    Text("حذف الكل", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        feedback.perform(SoundEffectType.TAP)
                        confirmClearHistory = false
                    }
                ) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
