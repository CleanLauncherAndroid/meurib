package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.share.ApkShareServer
import com.example.share.SocialTarget
import kotlinx.coroutines.launch

@Composable
fun SocialShareSection(
    shareUrl: String?,
    goldAccent: Color,
    onFeedbackMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val feedback = LocalFeedbackController.current
    val scope = rememberCoroutineScope()
    var preparingDirectApk by remember { mutableStateOf(false) }

    if (!shareUrl.isNullOrBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "مشاركة الرابط عبر التطبيقات:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SocialShareTile(
                    target = SocialTarget.WHATSAPP,
                    fallbackDrawable = R.drawable.ic_brand_whatsapp,
                    accentTint = Color(0xFF25D366),
                    onClick = {
                        feedback.perform(SoundEffectType.TAP)
                        ApkShareServer.shareLinkToTarget(
                            context = context,
                            target = SocialTarget.WHATSAPP,
                            url = shareUrl,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    }
                )
                SocialShareTile(
                    target = SocialTarget.MESSENGER,
                    fallbackDrawable = R.drawable.ic_brand_messenger,
                    accentTint = Color(0xFF0084FF),
                    onClick = {
                        feedback.perform(SoundEffectType.TAP)
                        ApkShareServer.shareLinkToTarget(
                            context = context,
                            target = SocialTarget.MESSENGER,
                            url = shareUrl,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    }
                )
                SocialShareTile(
                    target = SocialTarget.FACEBOOK,
                    fallbackDrawable = R.drawable.ic_brand_facebook,
                    accentTint = Color(0xFF1877F2),
                    onClick = {
                        feedback.perform(SoundEffectType.TAP)
                        ApkShareServer.shareLinkToTarget(
                            context = context,
                            target = SocialTarget.FACEBOOK,
                            url = shareUrl,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    }
                )
                SocialShareTile(
                    target = SocialTarget.INSTAGRAM,
                    fallbackDrawable = R.drawable.ic_brand_instagram,
                    accentTint = Color(0xFFC13584),
                    onClick = {
                        feedback.perform(SoundEffectType.TAP)
                        ApkShareServer.shareLinkToTarget(
                            context = context,
                            target = SocialTarget.INSTAGRAM,
                            url = shareUrl,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    }
                )
                SocialMoreTile(
                    onClick = {
                        feedback.perform(SoundEffectType.TAP)
                        ApkShareServer.shareLinkToTarget(
                            context = context,
                            target = SocialTarget.SYSTEM,
                            url = shareUrl,
                            onFeedbackMessage = onFeedbackMessage
                        )
                    }
                )
            }
        }
    }

    OrnamentalDivider(accentColor = goldAccent)

    Button(
        onClick = {
            if (preparingDirectApk) return@Button
            feedback.perform(SoundEffectType.SUCCESS, strongHaptic = true)
            preparingDirectApk = true
            scope.launch {
                ApkShareServer.shareApkFileDirectly(
                    context = context,
                    onError = onFeedbackMessage
                )
                preparingDirectApk = false
            }
        },
        enabled = !preparingDirectApk,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("share_apk_file_button"),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (preparingDirectApk) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("جاري تجهيز ملف APK...")
        } else {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "إرسال ملف التطبيق (APK) مباشرة",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SocialShareTile(
    target: SocialTarget,
    fallbackDrawable: Int,
    accentTint: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val installedBitmap: ImageBitmap? = remember(target) {
        target.packages.firstNotNullOfOrNull { pkg ->
            runCatching {
                context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = accentTint.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accentTint.copy(alpha = 0.30f)),
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (installedBitmap != null) {
                Image(
                    bitmap = installedBitmap,
                    contentDescription = target.labelAr,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = fallbackDrawable),
                    contentDescription = target.labelAr,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            Text(
                text = target.labelAr,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SocialMoreTile(onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.30f)),
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "المزيد",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "المزيد",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
