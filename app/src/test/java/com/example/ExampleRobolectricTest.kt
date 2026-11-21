package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEffectType
import com.example.audio.SoundEffectsManager
import com.example.data.ArabicFontScale
import com.example.data.ThemeMode
import com.example.model.IrabCategory
import com.example.model.WordIrabResult
import com.example.ui.components.IrabTable
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MueribTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("معرب", appName)
    }

    @Test
    fun soundEffectsManager_playsAndReleasesSafely() {
        val manager = SoundEffectsManager()
        SoundEffectType.entries.forEach { effect ->
            manager.play(effect)
        }
        manager.release()
    }

    @Test
    fun mueribThemeAndIrabTable_renderWithDarkModeOverride() {
        var resolvedDark = false
        val sampleWords = listOf(
            WordIrabResult(
                word = "كَتَبَ",
                diacritizedWord = "كَتَبَ",
                partOfSpeech = "فعل",
                syntacticRole = "فعل ماضٍ",
                grammaticalState = "مبني على الفتح",
                fullIrab = "فعل ماضٍ مبني على الفتح",
                category = IrabCategory.VERB_MABNI,
                lemma = "كتب",
                root = "كتب",
                stem = "كتب",
                pattern = "فَعَلَ",
                subType = null,
                tense = "ماض",
                gender = null,
                number = "مفرد",
                proclitics = emptyList(),
                enclitics = emptyList(),
                allReadings = emptyList()
            )
        )

        composeRule.setContent {
            MueribTheme(
                themeMode = ThemeMode.DARK,
                fontScale = ArabicFontScale.LARGE
            ) {
                resolvedDark = LocalIsDarkTheme.current
                IrabTable(
                    words = sampleWords,
                    onWordClick = {}
                )
            }
        }

        assertTrue(resolvedDark)
        composeRule.onNodeWithTag("irab_table").assertIsDisplayed()
        composeRule.onNodeWithTag("irab_row_0").assertIsDisplayed()
        composeRule.onNodeWithText("فعل ماضٍ").assertIsDisplayed()
    }

    @Test
    fun apkShareServer_servesLandingPageWithDownloadButton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = com.example.share.ApkShareServer(context)
        try {
            val session = kotlinx.coroutines.runBlocking {
                server.startOrRefreshSession(regenerateToken = true)
            }
            assertTrue(session.localPreviewUrl.startsWith("http://127.0.0.1:"))
            val conn = java.net.URL(session.localPreviewUrl).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            assertEquals(200, conn.responseCode)
            val html = conn.inputStream.bufferedReader().readText()
            assertTrue(html.contains("تحميل ملف التطبيق (APK)"))
            assertTrue(html.contains("download=\"muerib.apk\""))
            conn.disconnect()
        } finally {
            server.stop()
        }
    }

    @Test
    fun apkShareBottomSheet_displaysConfiguredPublicUrl() {
        val sampleSession = com.example.share.ApkShareSession(
            shareUrl = "http://192.168.1.10:8765/share/abc123",
            localPreviewUrl = "http://127.0.0.1:8765/share/abc123",
            downloadUrl = "http://192.168.1.10:8765/download/abc123/muerib.apk",
            apkSizeMb = "14.2",
            isLanReachable = true
        )

        composeRule.setContent {
            MueribTheme(themeMode = ThemeMode.LIGHT) {
                com.example.ui.components.ApkShareBottomSheet(
                    session = sampleSession,
                    isGenerating = false,
                    publicShareUrl = "https://example.github.io/muerib/",
                    onSavePublicShareUrl = {},
                    onRegenerateLink = {},
                    onFeedbackMessage = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("apk_share_url_text").assertIsDisplayed()
        composeRule.onNodeWithText("https://example.github.io/muerib/").assertIsDisplayed()
        composeRule.onNodeWithTag("copy_apk_link_button").assertIsDisplayed()
        composeRule.onNodeWithTag("open_apk_webpage_button").assertIsDisplayed()
    }
}
