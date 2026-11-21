package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AnalysisRepository
import com.example.data.ArabicFontScale
import com.example.data.ThemeMode
import com.example.data.UserPreferencesRepository
import com.example.data.UserSettings
import com.example.engine.ArabicGrammarEngine
import com.example.engine.EngineStatus
import com.example.engine.FarahidiWebViewEngine
import com.example.model.SentenceIrabResult
import com.example.model.WordIrabResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Analyzing : AnalysisUiState
    data class Success(val result: SentenceIrabResult) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

class MueribViewModel(
    private val farahidiEngine: FarahidiWebViewEngine,
    private val repository: AnalysisRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val engineStatus: StateFlow<EngineStatus> = farahidiEngine.status

    val userSettings: StateFlow<UserSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = UserSettings()
        )

    private val _sentenceInput = MutableStateFlow("")
    val sentenceInput: StateFlow<String> = _sentenceInput.asStateFlow()

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _selectedWord = MutableStateFlow<WordIrabResult?>(null)
    val selectedWord: StateFlow<WordIrabResult?> = _selectedWord.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _showPinnedOnly = MutableStateFlow(false)
    val showPinnedOnly: StateFlow<Boolean> = _showPinnedOnly.asStateFlow()

    val allHistory: StateFlow<List<SentenceIrabResult>> = repository.allAnalyses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val filteredHistory: StateFlow<List<SentenceIrabResult>> = combine(
        allHistory,
        _historySearchQuery,
        _showPinnedOnly
    ) { items, query, pinnedOnly ->
        val trimmed = query.trim()
        items.filter { item ->
            val matchesPin = !pinnedOnly || item.isPinned
            val matchesQuery = trimmed.isEmpty() ||
                item.sentence.contains(trimmed) ||
                item.sentenceType.contains(trimmed)
            matchesPin && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    fun onSentenceInputChange(value: String) {
        _sentenceInput.value = value
    }

    fun appendHaraka(haraka: String) {
        val current = _sentenceInput.value
        if (current.isEmpty()) return
        val trimmedEnd = current.trimEnd()
        val trailingSpaces = current.substring(trimmedEnd.length)
        _sentenceInput.value = trimmedEnd + haraka + trailingSpaces
    }

    fun clearInput() {
        _sentenceInput.value = ""
        _uiState.value = AnalysisUiState.Idle
    }

    fun selectPresetAndAnalyze(preset: String) {
        _sentenceInput.value = preset
        analyzeSentence(preset)
    }

    fun analyzeSentence(explicitText: String? = null) {
        val text = (explicitText ?: _sentenceInput.value).trim()
        if (text.isBlank()) {
            _uiState.value = AnalysisUiState.Error("يرجى إدخال جملة عربية أولاً للتحليل.")
            return
        }

        if (engineStatus.value !is EngineStatus.Ready) {
            _uiState.value = AnalysisUiState.Error("جاري تهيئة قاموس الفراهيدي المحلي، يرجى المحاولة بعد ثوانٍ.")
            return
        }

        val settings = userSettings.value
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Analyzing
            try {
                val raw = farahidiEngine.analyzeSentence(
                    sentence = text,
                    useLayer2 = settings.useLayer2Context
                )
                if (raw.tokens.isEmpty()) {
                    _uiState.value = AnalysisUiState.Error("لم يتم العثور على كلمات عربية صالحة في النص المدخل.")
                    return@launch
                }
                val parsed = withContext(Dispatchers.Default) {
                    ArabicGrammarEngine.analyzeSentence(
                        raw = raw,
                        detailedSigns = settings.detailedIrabSigns
                    )
                }
                val savedId = if (settings.autoSaveHistory) {
                    withContext(Dispatchers.IO) {
                        repository.saveAnalysis(parsed)
                    }
                } else {
                    0L
                }
                _uiState.value = AnalysisUiState.Success(parsed.copy(id = savedId))
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error(
                    e.message ?: "حدث خطأ غير متوقع أثناء التحليل النحوي."
                )
            }
        }
    }

    fun retryEngine() {
        farahidiEngine.retryInitialization()
    }

    fun openWordDetails(word: WordIrabResult) {
        _selectedWord.value = word
    }

    fun dismissWordDetails() {
        _selectedWord.value = null
    }

    fun loadFromHistory(item: SentenceIrabResult) {
        _sentenceInput.value = item.sentence
        _uiState.value = AnalysisUiState.Success(item)
    }

    fun onHistorySearchChange(query: String) {
        _historySearchQuery.value = query
    }

    fun togglePinnedFilter() {
        _showPinnedOnly.value = !_showPinnedOnly.value
    }

    fun toggleItemPinned(item: SentenceIrabResult) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePinned(item.id, !item.isPinned)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
        }
    }

    // Settings actions
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setFontScale(scale: ArabicFontScale) {
        viewModelScope.launch { preferencesRepository.setFontScale(scale) }
    }

    fun setUseLayer2Context(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setUseLayer2Context(enabled) }
    }

    fun setDetailedIrabSigns(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDetailedIrabSigns(enabled) }
    }

    fun setAutoSaveHistory(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoSaveHistory(enabled) }
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundEffectsEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticsEnabled(enabled) }
    }

    fun setPublicShareUrl(url: String) {
        viewModelScope.launch { preferencesRepository.setPublicShareUrl(url) }
    }

    fun formatIrabForClipboard(result: SentenceIrabResult): String {
        val sb = StringBuilder()
        sb.appendLine("الجملة: ${result.sentence}")
        sb.appendLine("نوع التركيب: ${result.sentenceType}")
        sb.appendLine("──────────")
        for (word in result.words) {
            sb.appendLine("• ${word.word} (${word.partOfSpeech}): ${word.fullIrab}")
        }
        return sb.toString().trim()
    }

    override fun onCleared() {
        super.onCleared()
        farahidiEngine.destroy()
    }

    class Factory(
        private val engine: FarahidiWebViewEngine,
        private val repository: AnalysisRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MueribViewModel(engine, repository, preferencesRepository) as T
        }
    }
}
