package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.model.AffixInfo
import com.example.model.MorphReading
import com.example.model.RawSentenceAnalysis
import com.example.model.RawTokenAnalysis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

sealed interface EngineStatus {
    data object Initializing : EngineStatus
    data class Ready(val layer2Ready: Boolean) : EngineStatus
    data class Error(val message: String) : EngineStatus
}

/**
 * محرك الفراهيدي الأوفلاين داخل WebView:
 * يشغّل مكتبة farahidi (نسخة JavaScript) من مجلد assets/farahidi/ مباشرة
 * ويقرأ جداول القاموس المضغوطة محلياً عبر GZIPInputStream بدون أي طلبات شبكة أو fetch.
 */
class FarahidiWebViewEngine(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<RawSentenceAnalysis>>()

    private val _status = MutableStateFlow<EngineStatus>(EngineStatus.Initializing)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private var webView: WebView? = null
    private var layer2Loaded = false
    private var currentAssetReader: BufferedReader? = null
    private var autoRecoveredRenderer = false

    init {
        mainHandler.post { setupWebView() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        try {
            val wv = WebView(appContext).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.loadsImagesAutomatically = false
                settings.blockNetworkImage = true
                settings.blockNetworkLoads = true

                addJavascriptInterface(Bridge(), "AndroidBridge")

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage?.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                            Log.e(TAG, "WebView JS Error: ${consoleMessage.message()}")
                        }
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = true

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        val crashed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            detail?.didCrash() == true
                        } else {
                            true
                        }
                        Log.e(TAG, "WebView renderer gone (didCrash=$crashed)")
                        closeAssetReader()
                        failAllPendingRequests("توقف محرك الويب الداخلي، جاري إعادة التهيئة...")
                        if (view == webView) {
                            webView?.destroy()
                            webView = null
                        } else {
                            view?.destroy()
                        }
                        layer2Loaded = false
                        if (!autoRecoveredRenderer) {
                            autoRecoveredRenderer = true
                            _status.value = EngineStatus.Initializing
                            mainHandler.postDelayed({ setupWebView() }, 250L)
                        } else {
                            _status.value = EngineStatus.Error("تعذر تشغيل محرك التحليل بسبب محدودية الذاكرة")
                        }
                        return true
                    }
                }
            }
            wv.onResume()
            wv.resumeTimers()
            webView = wv
            wv.loadUrl("file:///android_asset/farahidi/engine.html")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebView", e)
            _status.value = EngineStatus.Error(e.message ?: "تعذر إنشاء محرك WebView المحلي")
        }
    }

    fun retryInitialization() {
        mainHandler.post {
            autoRecoveredRenderer = false
            layer2Loaded = false
            closeAssetReader()
            _status.value = EngineStatus.Initializing
            webView?.destroy()
            webView = null
            setupWebView()
        }
    }

    suspend fun analyzeSentence(
        sentence: String,
        useLayer2: Boolean = true
    ): RawSentenceAnalysis {
        val reqId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<RawSentenceAnalysis>()
        pendingRequests[reqId] = deferred

        withContext(Dispatchers.Main) {
            val wv = webView
            if (wv == null) {
                pendingRequests.remove(reqId)
                deferred.completeExceptionally(IllegalStateException("محرك الفراهيدي غير مهيأ"))
                return@withContext
            }
            val quotedSentence = JSONObject.quote(sentence)
            wv.evaluateJavascript("window.analyze_text('$reqId', $quotedSentence, $useLayer2);", null)
        }

        return try {
            withTimeout(20_000L) {
                deferred.await()
            }
        } finally {
            pendingRequests.remove(reqId)
        }
    }

    fun destroy() {
        mainHandler.post {
            closeAssetReader()
            failAllPendingRequests("تم إغلاق محرك التحليل")
            webView?.destroy()
            webView = null
        }
    }

    private fun closeAssetReader() {
        runCatching { currentAssetReader?.close() }
        currentAssetReader = null
    }

    private fun failAllPendingRequests(message: String) {
        val keys = pendingRequests.keys.toList()
        for (key in keys) {
            pendingRequests.remove(key)?.completeExceptionally(IllegalStateException(message))
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun openAssetTable(fileName: String): Boolean {
            return try {
                closeAssetReader()
                if (fileName.contains("..") || fileName.contains("/")) return false
                val charset = if (fileName.endsWith(".lm.gz") || fileName.endsWith(".lm")) {
                    Charsets.ISO_8859_1
                } else {
                    Charsets.UTF_8
                }
                val strippedName = fileName.removeSuffix(".gz")
                currentAssetReader = try {
                    appContext.assets.open("farahidi/data/$strippedName").bufferedReader(charset)
                } catch (_: Exception) {
                    val gzStream = appContext.assets.open("farahidi/data/$fileName")
                    GZIPInputStream(gzStream).bufferedReader(charset)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed opening asset $fileName", e)
                false
            }
        }

        @JavascriptInterface
        fun readAssetChunk(): String? {
            return try {
                val reader = currentAssetReader ?: return null
                val buffer = CharArray(131_072)
                var totalRead = 0
                while (totalRead < buffer.size) {
                    val n = reader.read(buffer, totalRead, buffer.size - totalRead)
                    if (n <= 0) break
                    totalRead += n
                }
                if (totalRead == 0) {
                    closeAssetReader()
                    null
                } else {
                    String(buffer, 0, totalRead)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading asset chunk", e)
                closeAssetReader()
                null
            }
        }

        @JavascriptInterface
        fun onLayer1Ready(success: Boolean, errorMessage: String) {
            if (success) {
                _status.value = EngineStatus.Ready(layer2Ready = layer2Loaded)
            } else {
                _status.value = EngineStatus.Error(
                    errorMessage.ifBlank { "تعذر تحميل بيانات القاموس الصرفي" }
                )
            }
        }

        @JavascriptInterface
        fun onLayer2Ready(success: Boolean) {
            layer2Loaded = success
            if (_status.value is EngineStatus.Ready) {
                _status.value = EngineStatus.Ready(layer2Ready = success)
            }
        }

        @JavascriptInterface
        fun onAnalysisResult(requestId: String, jsonPayload: String) {
            val deferred = pendingRequests.remove(requestId) ?: return
            try {
                deferred.complete(parsePayload(jsonPayload))
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            }
        }

        @JavascriptInterface
        fun onAnalysisError(requestId: String, errorMessage: String) {
            val deferred = pendingRequests.remove(requestId) ?: return
            deferred.completeExceptionally(IllegalStateException(errorMessage))
        }
    }

    private fun parsePayload(jsonPayload: String): RawSentenceAnalysis {
        val root = JSONObject(jsonPayload)
        val sentence = root.optString("sentence", "")
        val layer2Used = root.optBoolean("layer2Used", false)
        val tokensArr = root.optJSONArray("tokens") ?: JSONArray()

        val tokens = ArrayList<RawTokenAnalysis>(tokensArr.length())
        for (i in 0 until tokensArr.length()) {
            val obj = tokensArr.getJSONObject(i)
            val primaryObj = obj.optJSONObject("primary")
            val readingsArr = obj.optJSONArray("readings") ?: JSONArray()

            val readings = ArrayList<MorphReading>(readingsArr.length())
            for (j in 0 until readingsArr.length()) {
                readings.add(parseReading(readingsArr.getJSONObject(j)))
            }

            tokens.add(
                RawTokenAnalysis(
                    originalWord = obj.optString("originalWord"),
                    bareWord = obj.optString("bareWord"),
                    explicitHaraka = obj.optNullable("explicitHaraka"),
                    contextLemma = obj.optNullable("contextLemma"),
                    contextStem = obj.optNullable("contextStem"),
                    contextRoot = obj.optNullable("contextRoot"),
                    partOfSpeech = obj.optString("part_of_speech", "غير محدد"),
                    caseOrMood = obj.optNullable("case_or_mood"),
                    primary = primaryObj?.let { parseReading(it) },
                    readings = readings
                )
            )
        }
        return RawSentenceAnalysis(sentence = sentence, layer2Used = layer2Used, tokens = tokens)
    }

    private fun parseReading(obj: JSONObject): MorphReading {
        return MorphReading(
            diacritized = obj.optString("diacritized"),
            lemma = obj.optString("lemma"),
            stem = obj.optString("stem"),
            root = obj.optNullable("root"),
            pattern = obj.optNullable("pattern"),
            partOfSpeech = obj.optString("part_of_speech"),
            subType = obj.optNullable("sub_type"),
            tense = obj.optNullable("tense"),
            gender = obj.optNullable("gender"),
            number = obj.optNullable("number"),
            person = obj.optNullable("person"),
            pronoun = obj.optNullable("pronoun"),
            voice = obj.optNullable("voice"),
            state = obj.optNullable("state"),
            caseOrMood = obj.optNullable("case_or_mood"),
            proclitics = parseAffixes(obj.optJSONArray("proclitics")),
            enclitics = parseAffixes(obj.optJSONArray("enclitics")),
            score = obj.optDouble("score", 0.0)
        )
    }

    private fun parseAffixes(arr: JSONArray?): List<AffixInfo> {
        if (arr == null) return emptyList()
        val list = ArrayList<AffixInfo>(arr.length())
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            list.add(
                AffixInfo(
                    form = item.optString("form"),
                    description = item.optNullable("description")
                )
            )
        }
        return list
    }

    private fun JSONObject.optNullable(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "FarahidiEngine"
    }
}
