package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.LocalFeedbackController
import com.example.audio.SoundEffectType
import com.example.audio.SoundEffectsManager
import com.example.audio.rememberFeedbackController
import com.example.data.AnalysisRepository
import com.example.data.AppDatabase
import com.example.data.UserPreferencesRepository
import com.example.engine.FarahidiWebViewEngine
import com.example.share.ApkShareServer
import com.example.share.ApkShareSession
import com.example.ui.MueribViewModel
import com.example.ui.components.ApkShareBottomSheet
import com.example.ui.components.MueribBottomBar
import com.example.ui.components.MueribNavRail
import com.example.ui.components.MueribTab
import com.example.ui.components.WordDetailBottomSheet
import com.example.ui.screens.AnalyzerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GoldSecondaryDark
import com.example.ui.theme.MueribTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = AnalysisRepository(db.sentenceAnalysisDao())
        val preferencesRepository = UserPreferencesRepository(applicationContext)
        val farahidiEngine = FarahidiWebViewEngine(applicationContext)
        val factory = MueribViewModel.Factory(farahidiEngine, repository, preferencesRepository)

        setContent {
            val viewModel: MueribViewModel = viewModel(factory = factory)
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

            val soundManager = remember { SoundEffectsManager() }
            DisposableEffect(soundManager) {
                onDispose { soundManager.release() }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MueribTheme(
                    themeMode = userSettings.themeMode,
                    fontScale = userSettings.fontScale
                ) {
                    val feedbackController = rememberFeedbackController(
                        soundManager = soundManager,
                        soundEnabled = userSettings.soundEffectsEnabled,
                        hapticsEnabled = userSettings.hapticsEnabled
                    )
                    CompositionLocalProvider(LocalFeedbackController provides feedbackController) {
                        MueribApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MueribApp(viewModel: MueribViewModel) {
    var activeTab by rememberSaveable { mutableStateOf(MueribTab.ANALYZER) }

    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val sentenceInput by viewModel.sentenceInput.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedWord by viewModel.selectedWord.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val filteredHistory by viewModel.filteredHistory.collectAsStateWithLifecycle()
    val historySearchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val showPinnedOnly by viewModel.showPinnedOnly.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val feedback = LocalFeedbackController.current
    val appContext = LocalContext.current.applicationContext

    val apkShareServer = remember(appContext) { ApkShareServer(appContext) }
    DisposableEffect(apkShareServer) {
        onDispose { apkShareServer.stop() }
    }
    var showApkShareSheet by remember { mutableStateOf(false) }
    var apkShareSession by remember { mutableStateOf<ApkShareSession?>(null) }
    var isGeneratingShareLink by remember { mutableStateOf(false) }

    fun launchShareLinkGeneration(regenerateToken: Boolean) {
        showApkShareSheet = true
        isGeneratingShareLink = true
        scope.launch {
            runCatching { apkShareServer.startOrRefreshSession(regenerateToken) }
                .onSuccess { apkShareSession = it }
                .onFailure {
                    snackbarHostState.showSnackbar("تعذر بدء خادم المشاركة المحلي")
                }
            isGeneratingShareLink = false
        }
    }

    val totalHistoryCount = allHistory.size
    val pinnedHistoryCount = remember(allHistory) { allHistory.count { it.isPinned } }

    if (activeTab != MueribTab.ANALYZER) {
        BackHandler {
            feedback.perform(SoundEffectType.TAP)
            activeTab = MueribTab.ANALYZER
        }
    }

    val onSelectTab: (MueribTab) -> Unit = { target ->
        if (activeTab != target) {
            feedback.perform(SoundEffectType.TAP)
            activeTab = target
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        border = BorderStroke(1.dp, GoldSecondaryDark.copy(alpha = 0.55f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoldSecondaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    MueribBottomBar(
                        activeTab = activeTab,
                        totalHistoryCount = totalHistoryCount,
                        onSelectTab = onSelectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    MueribNavRail(
                        activeTab = activeTab,
                        totalHistoryCount = totalHistoryCount,
                        onSelectTab = onSelectTab
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                            (fadeIn(tween(220)) + slideInHorizontally(tween(240)) { width ->
                                (width / 12) * direction
                            }) togetherWith (fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { width ->
                                -(width / 16) * direction
                            })
                        },
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            MueribTab.ANALYZER -> {
                                AnalyzerScreen(
                                    engineStatus = engineStatus,
                                    sentenceInput = sentenceInput,
                                    uiState = uiState,
                                    onInputChange = viewModel::onSentenceInputChange,
                                    onAppendHaraka = viewModel::appendHaraka,
                                    onClearClick = viewModel::clearInput,
                                    onAnalyzeClick = { viewModel.analyzeSentence() },
                                    onPresetClick = viewModel::selectPresetAndAnalyze,
                                    onWordClick = viewModel::openWordDetails,
                                    onRetryEngine = viewModel::retryEngine,
                                    onShareAppClick = { launchShareLinkGeneration(regenerateToken = false) },
                                    formatClipboard = viewModel::formatIrabForClipboard,
                                    onCopiedMessage = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("تم نسخ الإعراب إلى الحافظة")
                                        }
                                    }
                                )
                            }
                            MueribTab.HISTORY -> {
                                HistoryScreen(
                                    historyItems = filteredHistory,
                                    totalCount = totalHistoryCount,
                                    pinnedCount = pinnedHistoryCount,
                                    searchQuery = historySearchQuery,
                                    showPinnedOnly = showPinnedOnly,
                                    onSearchChange = viewModel::onHistorySearchChange,
                                    onTogglePinnedFilter = viewModel::togglePinnedFilter,
                                    onSelectSentence = { item ->
                                        viewModel.loadFromHistory(item)
                                        activeTab = MueribTab.ANALYZER
                                    },
                                    onToggleItemPin = viewModel::toggleItemPinned,
                                    onDeleteItem = viewModel::deleteHistoryItem,
                                    onClearAll = viewModel::clearAllHistory,
                                    formatClipboard = viewModel::formatIrabForClipboard,
                                    onCopiedMessage = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("تم نسخ الإعراب إلى الحافظة")
                                        }
                                    }
                                )
                            }
                            MueribTab.SETTINGS -> {
                                SettingsScreen(
                                    settings = userSettings,
                                    engineStatus = engineStatus,
                                    onThemeModeChange = viewModel::setThemeMode,
                                    onFontScaleChange = viewModel::setFontScale,
                                    onSoundEffectsChange = viewModel::setSoundEffectsEnabled,
                                    onHapticsChange = viewModel::setHapticsEnabled,
                                    onUseLayer2Change = viewModel::setUseLayer2Context,
                                    onDetailedSignsChange = viewModel::setDetailedIrabSigns,
                                    onAutoSaveChange = viewModel::setAutoSaveHistory,
                                    onRetryEngine = viewModel::retryEngine,
                                    onClearHistory = viewModel::clearAllHistory,
                                    onNotifyMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedWord?.let { word ->
        WordDetailBottomSheet(
            word = word,
            onDismiss = {
                feedback.perform(SoundEffectType.TAP)
                viewModel.dismissWordDetails()
            }
        )
    }

    if (showApkShareSheet) {
        ApkShareBottomSheet(
            session = apkShareSession,
            isGenerating = isGeneratingShareLink,
            publicShareUrl = userSettings.publicShareUrl,
            onSavePublicShareUrl = viewModel::setPublicShareUrl,
            onRegenerateLink = { launchShareLinkGeneration(regenerateToken = true) },
            onFeedbackMessage = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            onDismiss = {
                feedback.perform(SoundEffectType.TAP)
                showApkShareSheet = false
            }
        )
    }
}
