package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.share.ApkShareSession
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.HeaderEmeraldEnd
import com.example.ui.theme.HeaderEmeraldStart
import com.example.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkShareBottomSheet(
    session: ApkShareSession?,
    isGenerating: Boolean,
    publicShareUrl: String,
    onSavePublicShareUrl: (String) -> Unit,
    onRegenerateLink: () -> Unit,
    onFeedbackMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val feedback = LocalFeedbackController.current
    val isDark = LocalIsDarkTheme.current
    val goldAccent = if (isDark) GoldSecondaryDark else GoldSecondary

    val hasPublicUrl = publicShareUrl.isNotBlank()
    var usePublicMode by remember(hasPublicUrl) { mutableStateOf(hasPublicUrl) }
    var editingPublicUrl by remember(publicShareUrl) { mutableStateOf(!hasPublicUrl) }
    var publicUrlDraft by remember(publicShareUrl) { mutableStateOf(publicShareUrl) }
    var copiedRecently by remember { mutableStateOf(false) }

    LaunchedEffect(copiedRecently) {
        if (copiedRecently) {
            delay(1800)
            copiedRecently = false
        }
    }

    val activeShareUrl: String? = if (usePublicMode) {
        publicShareUrl.takeIf { it.isNotBlank() }
    } else {
        session?.shareUrl
    }

    val activeOpenInBrowserUrl: String? = if (usePublicMode) {
        publicShareUrl.takeIf { it.isNotBlank() }
    } else {
        session?.localPreviewUrl
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Header Card with App Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(HeaderEmeraldStart, HeaderEmeraldEnd)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, GoldSecondaryDark.copy(alpha = 0.7f)),
                        modifier = Modifier.size(62.dp)
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
                            text = "مشاركة تطبيق معرب (APK)",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        val sizeText = session?.let { "الحجم: ${it.apkSizeMb} ميجابايت • جاهز للتحميل" }
                            ?: "جاري تجهيز رابط الصفحة..."
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6EFE7)
                        )
                    }
                }
            }

            // Switch between Public Internet Link vs Offline Local LAN Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = usePublicMode,
                    onClick = {
                        feedback.perform(SoundEffectType.TOGGLE)
                        usePublicMode = true
                    },
                    label = { Text("رابط عام (عبر الإنترنت)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !usePublicMode,
                    onClick = {
                        feedback.perform(SoundEffectType.TOGGLE)
                        usePublicMode = false
                    },
                    label = { Text("شبكة محلية (أوفلاين)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Link Box + Actions
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (usePublicMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (hasPublicUrl) "رابط التحميل العام (يعمل للجميع)" else "إعداد رابط الإنترنت العام",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (hasPublicUrl && !editingPublicUrl) {
                                IconButton(
                                    onClick = { editingPublicUrl = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تعديل الرابط العام",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (editingPublicUrl || !hasPublicUrl) {
                            Text(
                                text = "ارفع مجلد docs المجهّز داخل المشروع إلى GitHub Pages أو Netlify Drop ثم الصق رابط الصفحة هنا ليعمل مع أي صديق عبر الإنترنت:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                OutlinedTextField(
                                    value = publicUrlDraft,
                                    onValueChange = { publicUrlDraft = it },
                                    singleLine = true,
                                    placeholder = { Text("https://username.github.io/muerib/") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("public_url_input")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        feedback.perform(SoundEffectType.SUCCESS, strongHaptic = true)
                                        onSavePublicShareUrl(publicUrlDraft)
                                        editingPublicUrl = false
                                        onFeedbackMessage("تم حفظ رابط التحميل العام بنجاح")
                                    },
                                    enabled = publicUrlDraft.trim().isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("save_public_url_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("حفظ الرابط العام")
                                }
                                if (hasPublicUrl) {
                                    TextButton(
                                        onClick = {
                                            feedback.perform(SoundEffectType.TAP)
                                            onSavePublicShareUrl("")
                                            publicUrlDraft = ""
                                            onFeedbackMessage("تمت إزالة الرابط العام")
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("مسح")
                                    }
                                }
                            }
                        } else {
                            UrlDisplayAndButtons(
                                displayUrl = publicShareUrl,
                                openUrl = activeOpenInBrowserUrl ?: publicShareUrl,
                                copiedRecently = copiedRecently,
                                showRegenerate = false,
                                onCopy = {
                                    feedback.perform(SoundEffectType.COPY, strongHaptic = true)
                                    clipboard.setText(AnnotatedString(publicShareUrl))
                                    copiedRecently = true
                                    onFeedbackMessage("تم نسخ رابط التحميل العام")
                                },
                                onRegenerate = {},
                                onOpenBrowser = { url ->
                                    feedback.perform(SoundEffectType.OPEN_CARD)
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }.onFailure {
                                        onFeedbackMessage("تعذر فتح المتصفح، تأكد من صحة الرابط")
                                    }
                                }
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "رابط صفحة التحميل المحلية (نفس الواي فاي / Hotspot)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isGenerating || session == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("جاري توليد رابط الويب المحلي...", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            UrlDisplayAndButtons(
                                displayUrl = session.shareUrl,
                                openUrl = session.localPreviewUrl,
                                copiedRecently = copiedRecently,
                                showRegenerate = true,
                                onCopy = {
                                    feedback.perform(SoundEffectType.COPY, strongHaptic = true)
                                    clipboard.setText(AnnotatedString(session.shareUrl))
                                    copiedRecently = true
                                    onFeedbackMessage("تم نسخ رابط تحميل معرب")
                                },
                                onRegenerate = {
                                    feedback.perform(SoundEffectType.HARAKA)
                                    onRegenerateLink()
                                },
                                onOpenBrowser = { url ->
                                    feedback.perform(SoundEffectType.OPEN_CARD)
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }.onFailure {
                                        onFeedbackMessage("لم يتم العثور على متصفح لفتح الرابط")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            SocialShareSection(
                shareUrl = activeShareUrl,
                goldAccent = goldAccent,
                onFeedbackMessage = onFeedbackMessage
            )

            val noteText = if (usePublicMode && hasPublicUrl) {
                "هذا الرابط العام يعمل مع أي شخص عبر الإنترنت في أي مكان. يمكنك أيضاً استخدام الزر أعلاه لإرسال ملف الـ APK مباشرة."
            } else if (!usePublicMode && session?.isLanReachable == true) {
                "يعمل الرابط المحلي عند اتصال الجهازين بنفس الواي فاي أو نقطة الاتصال (Hotspot) مع بقاء التطبيق مفتوحاً، أو أرسل ملف الـ APK مباشرة."
            } else {
                "ملاحظة: لتشغيل رابط يعمل لأي شخص عن بُعد عبر الإنترنت، انشر مجلد docs واحفظ رابط الصفحة هنا، أو أرسل ملف الـ APK مباشرة عبر واتساب."
            }
            Text(
                text = noteText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun UrlDisplayAndButtons(
    displayUrl: String,
    openUrl: String,
    copiedRecently: Boolean,
    showRegenerate: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onOpenBrowser: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = displayUrl,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .testTag("apk_share_url_text")
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onCopy,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("copy_apk_link_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (copiedRecently) "تم النسخ" else "نسخ الرابط")
        }

        if (showRegenerate) {
            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("توليد جديد")
            }
        }
    }

    OutlinedButton(
        onClick = { onOpenBrowser(openUrl) },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("open_apk_webpage_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("فتح صفحة التحميل في المتصفح (زر تحميل APK)")
    }
}
